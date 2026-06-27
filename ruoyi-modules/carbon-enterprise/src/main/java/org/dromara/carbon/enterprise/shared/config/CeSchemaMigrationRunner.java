package org.dromara.carbon.enterprise.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 企业端 Schema 自动迁移。
 *
 * <p>应用启动时检测并补齐缺失的数据库列，避免因 Schema 不同步导致运行时 SQL 错误。
 * 每个迁移步骤使用 INFORMATION_SCHEMA 检查，幂等可重复执行。</p>
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
        backfillSourceARelationshipColumns();
    }

    /**
     * 检查指定表是否包含目标列，缺失时执行 ALTER TABLE ADD COLUMN。
     */
    private void addColumnIfMissing(String tableName, String columnName, String columnDef) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, tableName, columnName
            );
            if (count != null && count > 0) {
                log.debug("[Schema迁移] {}.{} 已存在，跳过", tableName, columnName);
                return;
            }
            String sql = "ALTER TABLE " + tableName + " ADD " + columnName + " " + columnDef;
            jdbcTemplate.execute(sql);
            log.info("[Schema迁移] 已添加 {}.{} ({})", tableName, columnName, columnDef);
        } catch (Exception e) {
            log.warn("[Schema迁移] 处理 {}.{} 时出错（可能列已存在）: {}", tableName, columnName, e.getMessage());
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
                UPDATE ce_activity_data ad
                LEFT JOIN ce_emission_source es
                       ON es.source_identification_code = ad.source_identification_code
                      AND (es.company_code = ad.company_code OR ad.company_code IS NULL OR ad.company_code = '')
                   SET ad.emission_source_id = es.id,
                       ad.activity_period = COALESCE(NULLIF(ad.activity_period, ''), CONCAT(ad.activity_year, '-', LPAD(ad.activity_month, 2, '0'))),
                       ad.factory_code = COALESCE(NULLIF(ad.factory_code, ''), NULLIF(ad.company_code, ''), es.factory_code)
              WHERE (ad.emission_source_id IS NULL OR ad.emission_source_id = 0 OR ad.activity_period IS NULL OR ad.activity_period = '' OR ad.factory_code IS NULL OR ad.factory_code = '')
                AND es.id IS NOT NULL
             """);
        } catch (Exception e) {
            log.warn("[Schema杩佺Щ] Source(A) relationship column backfill skipped: {}", e.getMessage());
        }
    }
}
