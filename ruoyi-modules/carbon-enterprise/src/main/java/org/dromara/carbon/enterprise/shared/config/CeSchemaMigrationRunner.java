package org.dromara.carbon.enterprise.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityValidationServiceImpl;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enterprise SQL Server schema migration runner.
 */
@Slf4j
@Component
public class CeSchemaMigrationRunner implements CommandLineRunner {

    private static final String EMISSION_ACTIVITY_TEMPLATE_VERSION_CODE = "enterprise-local-emission-activity";
    private static final String EMISSION_ACTIVITY_TARGET_TABLE_CODE = "emission_activity";
    private static final String EMISSION_ACTIVITY_MODULE_CODE = "activity-data";

    private final JdbcTemplate jdbcTemplate;

    public CeSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createIndustryClassificationTableIfMissing();
        addColumnIfMissing("ce_emission_source", "source_unit", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_emission_source", "factory_code", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_activity_data", "emission_source_id", "BIGINT NULL");
        addColumnIfMissing("ce_activity_data", "activity_period", "NVARCHAR(32) NULL");
        addColumnIfMissing("ce_activity_data", "factory_code", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_template_field", "business_field_code", "NVARCHAR(64) NULL");
        backfillTemplateFieldBusinessCode();
        seedEmissionActivityTemplateIfMissing();
        addUniqueConstraintIfMissing("ce_template_field", "uk_ce_template_field_business_code", "sheet_id, business_field_code");
        backfillSourceARelationshipColumns();
        insertIndustryMenuIfMissing();
    }

    private void seedEmissionActivityTemplateIfMissing() {
        try {
            List<CeEmissionActivityFieldDescriptor> fields = CeEmissionActivityValidationServiceImpl.allFieldDescriptors();
            Long sheetId = resolveOrInsertEmissionActivitySheet(fields.size());
            boolean hasTargetColumnCode = columnExists("ce_template_field", "target_column_code");
            int inserted = 0;
            for (CeEmissionActivityFieldDescriptor field : fields) {
                if (emissionActivityTemplateFieldExists(sheetId, field, hasTargetColumnCode)) {
                    continue;
                }
                insertEmissionActivityTemplateField(sheetId, field, hasTargetColumnCode);
                inserted++;
            }
            log.info("[SchemaMigration] seeded emission_activity template, sheetId={}, fields={}, inserted={}",
                sheetId, fields.size(), inserted);
        } catch (Exception e) {
            log.warn("[SchemaMigration] emission_activity template seed skipped: {}", e.getMessage());
        }
    }

    private Long resolveOrInsertEmissionActivitySheet(int fieldCount) {
        try {
            Long existingId = jdbcTemplate.queryForObject("""
                SELECT TOP 1 id
                  FROM ce_template_sheet
                 WHERE target_table_code = ?
                 ORDER BY id DESC
                """, Long.class, EMISSION_ACTIVITY_TARGET_TABLE_CODE);
            if (existingId != null) {
                jdbcTemplate.update("UPDATE ce_template_sheet SET field_count = ? WHERE id = ?", fieldCount, existingId);
                return existingId;
            }
        } catch (EmptyResultDataAccessException ignored) {
            // Insert below.
        }

        Long templateVersionId = resolveOrInsertEmissionActivityTemplateVersion(fieldCount);
        return jdbcTemplate.queryForObject("""
                INSERT INTO ce_template_sheet (
                    template_version_id, source_file, source_group, sheet_name, sheet_type,
                    header_row, field_count, module_code, target_table_code, allow_extension, create_time
                )
                OUTPUT INSERTED.id
                VALUES (?, N'enterprise-local', N'activity', N'emission_activity', N'business',
                    ?, ?, ?, ?, ?, SYSDATETIME())
                """,
            Long.class,
            templateVersionId, 1, fieldCount, EMISSION_ACTIVITY_MODULE_CODE,
            EMISSION_ACTIVITY_TARGET_TABLE_CODE, true
        );
    }

    private Long resolveOrInsertEmissionActivityTemplateVersion(int fieldCount) {
        try {
            Long existingId = jdbcTemplate.queryForObject("""
                SELECT TOP 1 id
                  FROM ce_template_version
                 WHERE version_code = ?
                 ORDER BY id DESC
                """, Long.class, EMISSION_ACTIVITY_TEMPLATE_VERSION_CODE);
            if (existingId != null) {
                return existingId;
            }
        } catch (EmptyResultDataAccessException ignored) {
            // Insert below.
        }
        return jdbcTemplate.queryForObject("""
            INSERT INTO ce_template_version (
                version_code, version_name, source_dir, workbook_count, sheet_count,
                field_count, status, imported_by, imported_time, remark
            )
            OUTPUT INSERTED.id
            VALUES (?, N'企业端排放活动数据模板', N'enterprise-local', ?, ?, ?, N'active',
                N'system', SYSDATETIME(), N'Enterprise local emission_activity capture template')
            """,
            Long.class,
            EMISSION_ACTIVITY_TEMPLATE_VERSION_CODE, 1, 1, fieldCount
        );
    }

    private void insertEmissionActivityTemplateField(Long sheetId, CeEmissionActivityFieldDescriptor field,
                                                     boolean hasTargetColumnCode) {
        if (hasTargetColumnCode) {
            jdbcTemplate.update("""
                INSERT INTO ce_template_field (
                    sheet_id, field_order, original_field_name, target_column_code, business_field_code,
                    value_type, required_flag, original_field_flag, extensible_flag, create_time
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())
                """,
                sheetId, field.getFieldOrder(), field.getFieldName(), field.getFieldCode(), field.getFieldCode(),
                valueTypeOf(field.getFieldCode()), field.isRowValueRequired(), true, false
            );
            return;
        }
        jdbcTemplate.update("""
            INSERT INTO ce_template_field (
                sheet_id, field_order, original_field_name, business_field_code, value_type,
                required_flag, original_field_flag, extensible_flag, create_time
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())
            """,
            sheetId, field.getFieldOrder(), field.getFieldName(), field.getFieldCode(),
            valueTypeOf(field.getFieldCode()), field.isRowValueRequired(), true, false
        );
    }

    private boolean emissionActivityTemplateFieldExists(Long sheetId, CeEmissionActivityFieldDescriptor field,
                                                        boolean hasTargetColumnCode) {
        Integer count;
        if (hasTargetColumnCode) {
            count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM ce_template_field
                 WHERE sheet_id = ?
                   AND (business_field_code = ? OR target_column_code = ?)
                """,
                Integer.class, sheetId, field.getFieldCode(), field.getFieldCode()
            );
        } else {
            count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM ce_template_field
                 WHERE sheet_id = ?
                   AND business_field_code = ?
                """,
                Integer.class, sheetId, field.getFieldCode()
            );
        }
        return count != null && count > 0;
    }

    private String valueTypeOf(String fieldCode) {
        return switch (fieldCode) {
            case "activityValue" -> "decimal";
            case "activityYear", "activityMonth" -> "integer";
            case "activityDate" -> "date";
            default -> "text";
        };
    }

    private void insertIndustryMenuIfMissing() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE menu_id = ? OR path = ?",
                Integer.class, 900116L, "industry"
            );
            if (count != null && count > 0) {
                log.debug("[SchemaMigration] industry menu exists, skipped");
                return;
            }
            jdbcTemplate.update("""
                INSERT INTO sys_menu (
                    menu_id, menu_name, parent_id, order_num, path, component, query_param,
                    is_frame, is_cache, menu_type, visible, status, perms, icon, remark
                ) VALUES (
                    ?, N'107 行业代码表', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, N'行业代码表'
                )
                """,
                900116L, 900110L, 6, "industry", "enterprise/dimension/index", "{\"code\":\"industry\"}",
                1, 0, "C", "0", "0", "enterprise:dimension:list", "dict"
            );
            log.info("[SchemaMigration] inserted industry menu");
        } catch (Exception e) {
            log.warn("[SchemaMigration] industry menu insert skipped: {}", e.getMessage());
        }
    }

    private void createIndustryClassificationTableIfMissing() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ?",
                Integer.class, "ce_industry_classification"
            );
            if (count != null && count > 0) {
                log.debug("[SchemaMigration] ce_industry_classification exists, skipped create");
            } else {
                jdbcTemplate.execute("""
                    CREATE TABLE ce_industry_classification (
                        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                        industry_section_code NVARCHAR(32) NOT NULL,
                        industry_section_name NVARCHAR(128) NOT NULL,
                        industry_division_code NVARCHAR(32) NULL,
                        industry_division_name NVARCHAR(128) NULL,
                        industry_group_code NVARCHAR(32) NULL,
                        industry_group_name NVARCHAR(128) NULL,
                        industry_class_code NVARCHAR(32) NULL,
                        industry_class_name NVARCHAR(128) NULL,
                        sort_order INT NULL,
                        status NVARCHAR(16) NULL,
                        create_time DATETIME2 NULL DEFAULT SYSDATETIME(),
                        update_time DATETIME2 NULL,
                        remark NVARCHAR(500) NULL
                    )
                    """);
                log.info("[SchemaMigration] created ce_industry_classification");
            }
        } catch (Exception e) {
            log.warn("[SchemaMigration] failed to create ce_industry_classification: {}", e.getMessage());
        }
        addColumnIfMissing("ce_industry_classification", "industry_path_key",
            "AS (concat(industry_section_code,'|',isnull(industry_division_code,''),'|',isnull(industry_group_code,''),'|',isnull(industry_class_code,''))) PERSISTED");
        addUniqueConstraintIfMissing("ce_industry_classification", "uk_ce_industry_classification_path", "industry_path_key");
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDef) {
        try {
            if (columnExists(tableName, columnName)) {
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

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class, tableName, columnName
        );
        return count != null && count > 0;
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
