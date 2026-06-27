package org.dromara.carbon.enterprise.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Enterprise schema migration runner.
 */
@Slf4j
@Component
public class CeSchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final boolean sqlServer;

    public CeSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlServer = resolveDatabaseProductName().contains("sql server");
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("ce_emission_source", "source_unit", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_emission_source", "factory_code", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_activity_data", "emission_source_id", "BIGINT NULL");
        addColumnIfMissing("ce_activity_data", "activity_period", "NVARCHAR(32) NULL");
        addColumnIfMissing("ce_activity_data", "factory_code", "NVARCHAR(64) NULL");
        backfillSourceARelationshipColumns();
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDef) {
        try {
            String existsSql = sqlServer
                ? "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ? AND COLUMN_NAME = ?"
                : "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(existsSql, Integer.class, tableName, columnName);
            if (count != null && count > 0) {
                log.debug("[SchemaMigration] {}.{} exists, skipped", tableName, columnName);
                return;
            }
            String sql = "ALTER TABLE " + tableName + " ADD " + columnName + " " + columnDef;
            jdbcTemplate.execute(sql);
            log.info("[SchemaMigration] added {}.{} ({})", tableName, columnName, columnDef);
        } catch (Exception e) {
            log.warn("[SchemaMigration] failed to process {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }

    private void backfillSourceARelationshipColumns() {
        try {
            jdbcTemplate.update("""
                UPDATE ce_emission_source
                   SET factory_code = company_code
                 WHERE (factory_code IS NULL OR factory_code = '')
                   AND company_code IS NOT NULL
                   AND company_code <> ''
                """);
            jdbcTemplate.update(sqlServer ? sqlServerActivityBackfillSql() : mysqlActivityBackfillSql());
        } catch (Exception e) {
            log.warn("[SchemaMigration] Source(A) relationship column backfill skipped: {}", e.getMessage());
        }
    }

    private String mysqlActivityBackfillSql() {
        return """
                UPDATE ce_activity_data ad
                LEFT JOIN ce_emission_source es
                       ON es.source_identification_code = ad.source_identification_code
                      AND (es.company_code = ad.company_code OR ad.company_code IS NULL OR ad.company_code = '')
                   SET ad.emission_source_id = es.id,
                       ad.activity_period = COALESCE(NULLIF(ad.activity_period, ''), CONCAT(ad.activity_year, '-', LPAD(ad.activity_month, 2, '0'))),
                       ad.factory_code = COALESCE(NULLIF(ad.factory_code, ''), NULLIF(ad.company_code, ''), es.factory_code)
                 WHERE (ad.emission_source_id IS NULL OR ad.emission_source_id = 0 OR ad.activity_period IS NULL OR ad.activity_period = '' OR ad.factory_code IS NULL OR ad.factory_code = '')
                   AND es.id IS NOT NULL
             """;
    }

    private String sqlServerActivityBackfillSql() {
        return """
                UPDATE ad
                   SET ad.emission_source_id = es.id,
                       ad.activity_period = COALESCE(NULLIF(ad.activity_period, ''), CONCAT(ad.activity_year, '-', RIGHT('0' + CAST(ad.activity_month AS VARCHAR(2)), 2))),
                       ad.factory_code = COALESCE(NULLIF(ad.factory_code, ''), NULLIF(ad.company_code, ''), es.factory_code)
                  FROM ce_activity_data ad
                  JOIN ce_emission_source es
                    ON es.source_identification_code = ad.source_identification_code
                   AND (es.company_code = ad.company_code OR ad.company_code IS NULL OR ad.company_code = '')
                 WHERE (ad.emission_source_id IS NULL OR ad.emission_source_id = 0 OR ad.activity_period IS NULL OR ad.activity_period = '' OR ad.factory_code IS NULL OR ad.factory_code = '')
             """;
    }

    private String resolveDatabaseProductName() {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName().toLowerCase(Locale.ROOT);
        } catch (SQLException | NullPointerException e) {
            log.warn("[SchemaMigration] database product detection failed, fallback to MySQL syntax: {}", e.getMessage());
            return "";
        }
    }
}
