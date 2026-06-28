package org.dromara.carbon.enterprise.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Enterprise SQL Server schema migration runner.
 */
@Slf4j
@Component
public class CeSchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public CeSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("ce_emission_source", "source_unit", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_emission_source", "factory_code", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_activity_data", "emission_source_id", "BIGINT NULL");
        addColumnIfMissing("ce_activity_data", "activity_period", "NVARCHAR(32) NULL");
        addColumnIfMissing("ce_activity_data", "factory_code", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_template_field", "business_field_code", "NVARCHAR(64) NULL");
        backfillTemplateFieldBusinessCode();
        addUniqueConstraintIfMissing("ce_template_field", "uk_ce_template_field_business_code", "sheet_id, business_field_code");
        backfillSourceARelationshipColumns();
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDef) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, tableName, columnName
            );
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

    private void addUniqueConstraintIfMissing(String tableName, String constraintName, String columns) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys.key_constraints WHERE parent_object_id = OBJECT_ID(?) AND name = ? AND type = 'UQ'",
                Integer.class, tableName, constraintName
            );
            if (count != null && count > 0) {
                log.debug("[SchemaMigration] unique constraint {} exists, skipped", constraintName);
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName + " UNIQUE (" + columns + ")");
            log.info("[SchemaMigration] added unique constraint {} on {} ({})", constraintName, tableName, columns);
        } catch (Exception e) {
            log.warn("[SchemaMigration] failed to add unique constraint {} on {}: {}", constraintName, tableName, e.getMessage());
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
            jdbcTemplate.update("""
                UPDATE ad
                   SET ad.emission_source_id = es.id,
                       ad.activity_period = COALESCE(NULLIF(ad.activity_period, ''), CONCAT(ad.activity_year, '-', RIGHT('0' + CAST(ad.activity_month AS VARCHAR(2)), 2))),
                       ad.factory_code = COALESCE(NULLIF(ad.factory_code, ''), NULLIF(ad.company_code, ''), es.factory_code)
                  FROM ce_activity_data ad
                  JOIN ce_emission_source es
                    ON es.source_identification_code = ad.source_identification_code
                   AND (es.company_code = ad.company_code OR ad.company_code IS NULL OR ad.company_code = '')
                 WHERE (ad.emission_source_id IS NULL OR ad.emission_source_id = 0 OR ad.activity_period IS NULL OR ad.activity_period = '' OR ad.factory_code IS NULL OR ad.factory_code = '')
             """);
        } catch (Exception e) {
            log.warn("[SchemaMigration] Source(A) relationship column backfill skipped: {}", e.getMessage());
        }
    }

    private void backfillTemplateFieldBusinessCode() {
        try {
            Integer legacyColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, "ce_template_field", "target_column_code"
            );
            if (legacyColumnCount != null && legacyColumnCount > 0) {
                jdbcTemplate.update("""
                    UPDATE ce_template_field
                       SET business_field_code = COALESCE(NULLIF(target_column_code, ''), NULLIF(original_field_name, ''))
                     WHERE (business_field_code IS NULL OR business_field_code = '')
                    """);
            } else {
                jdbcTemplate.update("""
                    UPDATE ce_template_field
                       SET business_field_code = original_field_name
                     WHERE (business_field_code IS NULL OR business_field_code = '')
                       AND original_field_name IS NOT NULL
                       AND original_field_name <> ''
                    """);
            }
            log.info("[SchemaMigration] backfilled ce_template_field.business_field_code");
        } catch (Exception e) {
            log.warn("[SchemaMigration] ce_template_field business code backfill skipped: {}", e.getMessage());
        }
    }
}
