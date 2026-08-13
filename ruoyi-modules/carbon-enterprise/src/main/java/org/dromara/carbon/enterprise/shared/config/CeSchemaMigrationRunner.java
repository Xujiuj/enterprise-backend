package org.dromara.carbon.enterprise.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityValidationServiceImpl;
import org.dromara.carbon.enterprise.shared.config.CeGbIndustryClassification.IndustryRecord;
import org.dromara.carbon.enterprise.shared.service.ICeCompanyFactoryDeptSyncService;
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
    private final ICeCompanyFactoryDeptSyncService companyFactoryDeptSyncService;

    public CeSchemaMigrationRunner(JdbcTemplate jdbcTemplate,
                                   ICeCompanyFactoryDeptSyncService companyFactoryDeptSyncService) {
        this.jdbcTemplate = jdbcTemplate;
        this.companyFactoryDeptSyncService = companyFactoryDeptSyncService;
    }

    @Override
    public void run(String... args) {
        createIndustryClassificationTableIfMissing();
        seedIndustryClassificationRecords();
        addColumnIfMissing("ce_emission_source", "source_unit", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_emission_source", "factory_code", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_emission_source", "data_frequency", "NVARCHAR(16) NULL");
        addColumnIfMissing("ce_emission_source", "responsible_user_id", "BIGINT NULL");
        addColumnIfMissing("ce_emission_source", "responsible_user_name", "NVARCHAR(100) NULL");
        addColumnIfMissing("ce_activity_data", "emission_source_id", "BIGINT NULL");
        addColumnIfMissing("ce_activity_data", "activity_period", "NVARCHAR(32) NULL");
        addColumnIfMissing("ce_activity_data", "factory_code", "NVARCHAR(64) NULL");
        addColumnIfMissing("ce_factor_cache_record", "custom_fields", "NVARCHAR(MAX) NULL");
        addColumnIfMissing("ce_factor_cache_record", "remark", "NVARCHAR(500) NULL");
        addColumnIfMissing("ce_template_field", "business_field_code", "NVARCHAR(64) NULL");
        alterColumnIfExists("ce_base_year", "factory_code", "NVARCHAR(64) NULL");
        alterColumnIfExists("ce_base_year", "factory_name", "NVARCHAR(255) NULL");
        clearEnterpriseReportContentCharts();
        backfillTemplateFieldBusinessCode();
        seedEmissionActivityTemplateIfMissing();
        addUniqueConstraintIfMissing("ce_template_field", "uk_ce_template_field_business_code", "sheet_id, business_field_code");
        backfillSourceARelationshipColumns();
        backfillEfFactorRecordCodes();
        removeIndustryMenuIfPresent();
        seedEnterpriseDeptMenuIfMissing();
        addColumnIfMissing("sys_dept", "factory_code", "NVARCHAR(64) NULL");
        backfillFactoryCodesFromCompanyProjection();
        syncSysDeptToCompanyFactories();
        updateEnterpriseMenuIcons();
    }

    private void syncSysDeptToCompanyFactories() {
        try {
            companyFactoryDeptSyncService.syncSysDeptToCompanyFactories();
        } catch (Exception e) {
            log.warn("[SchemaMigration] department company projection sync skipped: {}", e.getMessage());
        }
    }

    private void backfillFactoryCodesFromCompanyProjection() {
        try {
            jdbcTemplate.update("""
                IF OBJECT_ID(N'dbo.sys_dept', N'U') IS NOT NULL
                   AND OBJECT_ID(N'dbo.ce_company_factory', N'U') IS NOT NULL
                BEGIN
                    UPDATE factory
                       SET factory.factory_code = source.factory_code,
                           factory.update_time = SYSDATETIME()
                      FROM dbo.sys_dept factory
                      JOIN dbo.sys_dept company ON company.dept_id = factory.parent_id
                      JOIN dbo.ce_company_factory source
                        ON source.company_code = company.dept_category
                       AND source.factory_name = factory.dept_name
                     WHERE factory.del_flag = N'0'
                       AND company.del_flag = N'0'
                       AND factory.dept_category = company.dept_category
                       AND NULLIF(LTRIM(RTRIM(factory.factory_code)), N'') IS NULL
                       AND NULLIF(LTRIM(RTRIM(source.factory_code)), N'') IS NOT NULL;
                END
                """);
        } catch (Exception e) {
            log.warn("[SchemaMigration] factory code backfill skipped: {}", e.getMessage());
        }
    }

    private void syncEmissionSourceDepartmentsToSysDept() {
        try {
            jdbcTemplate.update("""
                IF OBJECT_ID(N'dbo.sys_dept', N'U') IS NOT NULL
                   AND OBJECT_ID(N'dbo.ce_emission_source', N'U') IS NOT NULL
                BEGIN
                    ;WITH parent_dept AS (
                        SELECT
                            CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id,
                            CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN N'0,100' ELSE N'0' END AS ancestors
                    ),
                    resolved_source AS (
                        SELECT DISTINCT
                            LTRIM(RTRIM(es.responsible_dept)) AS dept_name,
                            NULLIF(LTRIM(RTRIM(cf.company_code)), N'') AS company_code,
                            COALESCE(
                                NULLIF(LTRIM(RTRIM(cf.factory_name)), N''),
                                NULLIF(LTRIM(RTRIM(cf.factory_code)), N'')
                            ) AS factory_dept_name
                        FROM dbo.ce_emission_source es
                        JOIN dbo.ce_company_factory cf
                          ON (
                              NULLIF(LTRIM(RTRIM(es.factory_code)), N'') IS NOT NULL
                              AND cf.factory_code = NULLIF(LTRIM(RTRIM(es.factory_code)), N'')
                          )
                          OR (
                              NULLIF(LTRIM(RTRIM(es.factory_code)), N'') IS NULL
                              AND NULLIF(LTRIM(RTRIM(es.factory_name)), N'') IS NOT NULL
                              AND cf.factory_name = NULLIF(LTRIM(RTRIM(es.factory_name)), N'')
                          )
                        WHERE es.responsible_dept IS NOT NULL
                          AND LTRIM(RTRIM(es.responsible_dept)) <> N''
                    ),
                    source_depts AS (
                        SELECT DISTINCT
                            resolved_source.dept_name,
                            resolved_source.company_code AS dept_category,
                            factory_dept.dept_id AS parent_id,
                            CONCAT(factory_dept.ancestors, N',', factory_dept.dept_id) AS ancestors
                        FROM resolved_source
                        CROSS JOIN parent_dept
                        JOIN dbo.sys_dept company_dept
                            ON company_dept.del_flag = N'0'
                           AND company_dept.parent_id = parent_dept.parent_id
                           AND ISNULL(company_dept.dept_category, N'') = resolved_source.company_code
                        JOIN dbo.sys_dept factory_dept
                            ON factory_dept.del_flag = N'0'
                           AND factory_dept.dept_name = resolved_source.factory_dept_name
                           AND ISNULL(factory_dept.dept_category, N'') = resolved_source.company_code
                           AND factory_dept.parent_id = company_dept.dept_id
                        WHERE resolved_source.dept_name <> resolved_source.factory_dept_name
                    ),
                    candidates AS (
                        SELECT source_depts.dept_name, source_depts.dept_category, source_depts.parent_id, source_depts.ancestors
                        FROM source_depts
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM dbo.sys_dept d
                            WHERE d.del_flag = N'0'
                              AND d.dept_name = source_depts.dept_name
                              AND ISNULL(d.dept_category, N'') = source_depts.dept_category
                              AND d.parent_id = source_depts.parent_id
                        )
                    ),
                    numbered AS (
                        SELECT
                            candidates.dept_name,
                            candidates.dept_category,
                            candidates.parent_id,
                            candidates.ancestors,
                            ROW_NUMBER() OVER (ORDER BY candidates.dept_category, candidates.parent_id, candidates.dept_name) AS rn
                        FROM candidates
                    ),
                    id_base AS (
                        SELECT CASE WHEN ISNULL(MAX(dept_id), 0) < 100000 THEN 100000 ELSE MAX(dept_id) END AS max_dept_id
                        FROM dbo.sys_dept
                    ),
                    tenant_value AS (
                        SELECT COALESCE((SELECT TOP 1 tenant_id FROM dbo.sys_dept WHERE tenant_id IS NOT NULL ORDER BY dept_id), N'000000') AS tenant_id
                    )
                    INSERT INTO dbo.sys_dept (
                        dept_id, tenant_id, parent_id, ancestors, dept_name, dept_category,
                        order_num, status, del_flag, create_dept, create_by, create_time
                    )
                    SELECT
                        id_base.max_dept_id + numbered.rn,
                        tenant_value.tenant_id,
                        numbered.parent_id,
                        numbered.ancestors,
                        numbered.dept_name,
                        numbered.dept_category,
                        100 + numbered.rn,
                        N'0',
                        N'0',
                        numbered.parent_id,
                        1,
                        SYSDATETIME()
                    FROM numbered
                    CROSS JOIN id_base
                    CROSS JOIN tenant_value;
                END
                """);
        } catch (Exception e) {
            log.warn("[SchemaMigration] emission source department sync skipped: {}", e.getMessage());
        }
    }

    private void seedEnterpriseDeptMenuIfMissing() {
        try {
            seedMenu(103L, "部门管理", 1L, 4, "dept", "system/dept/index", "C", "system:dept:list", "form", "部门管理");
            seedMenu(1051L, "部门查询", 103L, 1, "#", "", "F", "system:dept:query", "#", "部门查询权限");
            seedMenu(1052L, "部门新增", 103L, 2, "#", "", "F", "system:dept:add", "#", "部门新增权限");
            seedMenu(1053L, "部门修改", 103L, 3, "#", "", "F", "system:dept:edit", "#", "部门修改权限");
            seedMenu(1054L, "部门删除", 103L, 4, "#", "", "F", "system:dept:remove", "#", "部门删除权限");
            updateMenuIcon("dept", "form");
            grantMenuToRole(900001L, 103L, 1051L, 1052L, 1053L, 1054L);
            grantMenuToRole(900002L, 103L, 1051L, 1052L, 1053L, 1054L);
        } catch (Exception e) {
            log.warn("[SchemaMigration] enterprise dept menu seed skipped: {}", e.getMessage());
        }
    }

    private void seedMenu(Long menuId, String menuName, Long parentId, int orderNum, String path, String component,
                          String menuType, String perms, String icon, String remark) {
        jdbcTemplate.update("""
            IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = ?)
            INSERT INTO sys_menu (
                menu_id, menu_name, parent_id, order_num, path, component, query_param,
                is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark
            )
            VALUES (?, ?, ?, ?, ?, ?, N'', 1, 0, ?, N'0', N'0', ?, ?, 103, 1, SYSDATETIME(), ?)
            """,
            menuId, menuId, menuName, parentId, orderNum, path, component, menuType, perms, icon, remark);
    }

    private void grantMenuToRole(Long roleId, Long... menuIds) {
        for (Long menuId : menuIds) {
            jdbcTemplate.update("""
                IF EXISTS (SELECT 1 FROM sys_role WHERE role_id = ?)
                   AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = ?)
                   AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = ? AND menu_id = ?)
                INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)
                """, roleId, menuId, roleId, menuId, roleId, menuId);
        }
    }

    private void updateEnterpriseMenuIcons() {
        updateMenuIcon("system-auth", "link");
        updateMenuIcon("license-import", "link");
        updateMenuIcon("emission-source-config", "list");
        updateMenuIcon("factor-confirm", "list");
        updateMenuIcon("activity-data", "list");
        updateMenuIcon("green-electricity", "list");
        updateMenuIcon("intensity", "list");
        updateMenuIcon("admin-division", "link");
        updateMenuIcon("company", "form");
        updateMenuIcon("emission-source-category", "link");
        updateMenuIcon("base-year", "link");
        updateMenuIcon("ef-factor", "form");
        updateMenuIcon("ef-electricity-factor", "link");
        updateMenuIcon("ef-electricity-version", "link");
        updateMenuIcon("ef-electricity-scope", "link");
        updateMenuIcon("greenhouse-gas", "link");
        updateMenuIcon("intensity-target", "form");
        updateMenuIcon("intensity-tolerance", "form");
        updateMenuIcon("content", "link");
        updateMenuIcon("report-template-download", "link");
    }

    private void updateMenuIcon(String path, String icon) {
        try {
            jdbcTemplate.update("UPDATE sys_menu SET icon = ? WHERE path = ?", icon, path);
        } catch (Exception e) {
            log.warn("[SchemaMigration] menu icon update skipped for path {}: {}", path, e.getMessage());
        }
    }

    private void clearEnterpriseReportContentCharts() {
        try {
            jdbcTemplate.update("""
                IF OBJECT_ID(N'dbo.ce_report_content', N'U') IS NOT NULL
                   AND COL_LENGTH(N'dbo.ce_report_content', N'chart_names') IS NOT NULL
                UPDATE dbo.ce_report_content
                   SET chart_names = NULL,
                       update_time = SYSDATETIME()
                 WHERE chart_names IS NOT NULL
                """);
        } catch (Exception e) {
            log.warn("[SchemaMigration] enterprise report content chart cleanup skipped: {}", e.getMessage());
        }
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

    private void removeIndustryMenuIfPresent() {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM sys_menu WHERE menu_id = ? OR path = ? OR menu_name = N'107 行业代码表'",
                900116L, "industry"
            );
            if (deleted > 0) {
                log.info("[SchemaMigration] removed industry menu, deleted={}", deleted);
            }
        } catch (Exception e) {
            log.warn("[SchemaMigration] industry menu removal skipped: {}", e.getMessage());
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

    private void seedIndustryClassificationRecords() {
        try {
            migrateLegacyIndustryClassificationRecord(
                "S", "", "", "",
                CeGbIndustryClassification.HEADQUARTERS
            );
            migrateLegacyIndustryClassificationRecord(
                "C", "26", "261", "2614",
                CeGbIndustryClassification.POLYSILICON
            );
            for (IndustryRecord record : CeGbIndustryClassification.records()) {
                seedIndustryClassificationRecord(record);
            }
            markSeededIndustryClassificationRecord("D", "44", "441", "4411");
            markSeededIndustryClassificationRecord("C", "30", "301", "3011");
            migrateCompanyFactoryIndustryCodes();
            seedCompanyFactoryReferenceRecords();
            log.info("[SchemaMigration] processed ce_industry_classification GB/T 4754-2017 reference records");
        } catch (Exception e) {
            log.warn("[SchemaMigration] industry classification seed skipped: {}", e.getMessage());
        }
    }

    private void migrateCompanyFactoryIndustryCodes() {
        jdbcTemplate.update("""
            UPDATE ce_company_factory
               SET industry_section_code = ?,
                   industry_section_name = ?,
                   industry_division_code = ?,
                   industry_division_name = ?,
                   industry_group_code = ?,
                   industry_group_name = ?,
                   industry_class_code = ?,
                   industry_class_name = ?,
                   update_time = SYSDATETIME()
             WHERE industry_section_code = ?
               AND ISNULL(industry_division_code, '') = ?
               AND ISNULL(industry_group_code, '') = ?
               AND ISNULL(industry_class_code, '') = ?
            """,
            CeGbIndustryClassification.HEADQUARTERS.sectionCode(), CeGbIndustryClassification.HEADQUARTERS.sectionName(),
            CeGbIndustryClassification.HEADQUARTERS.divisionCode(), CeGbIndustryClassification.HEADQUARTERS.divisionName(),
            CeGbIndustryClassification.HEADQUARTERS.groupCode(), CeGbIndustryClassification.HEADQUARTERS.groupName(),
            CeGbIndustryClassification.HEADQUARTERS.classCode(), CeGbIndustryClassification.HEADQUARTERS.className(),
            "S", "", "", ""
        );
        jdbcTemplate.update("""
            UPDATE ce_company_factory
               SET industry_section_code = ?,
                   industry_section_name = ?,
                   industry_division_code = ?,
                   industry_division_name = ?,
                   industry_group_code = ?,
                   industry_group_name = ?,
                   industry_class_code = ?,
                   industry_class_name = ?,
                   update_time = SYSDATETIME()
             WHERE industry_section_code = ?
               AND ISNULL(industry_division_code, '') = ?
               AND ISNULL(industry_group_code, '') = ?
               AND ISNULL(industry_class_code, '') = ?
            """,
            CeGbIndustryClassification.POLYSILICON.sectionCode(), CeGbIndustryClassification.POLYSILICON.sectionName(),
            CeGbIndustryClassification.POLYSILICON.divisionCode(), CeGbIndustryClassification.POLYSILICON.divisionName(),
            CeGbIndustryClassification.POLYSILICON.groupCode(), CeGbIndustryClassification.POLYSILICON.groupName(),
            CeGbIndustryClassification.POLYSILICON.classCode(), CeGbIndustryClassification.POLYSILICON.className(),
            "C", "26", "261", "2614"
        );
    }

    private void seedCompanyFactoryReferenceRecords() {
        seedCompanyFactoryReferenceRecord(
            "1", "101", "10101", "峰行智成集团", "集团总部", "650000", "新疆维吾尔自治区", "集团总部",
            CeGbIndustryClassification.HEADQUARTERS
        );
        seedCompanyFactoryReferenceRecord(
            "2", "101", "10102", "峰行智成集团", "峰行智成（新疆）硅基材料有限公司", "650000", "新疆维吾尔自治区", "多晶硅生产",
            CeGbIndustryClassification.POLYSILICON
        );
        seedCompanyFactoryReferenceRecord(
            "3", "101", "10103", "峰行智成集团", "峰行智成（山东）热电联产有限公司", "370000", "山东省", "电力生产",
            CeGbIndustryClassification.THERMAL_POWER
        );
        seedCompanyFactoryReferenceRecord(
            "4", "101", "10104", "峰行智成集团", "峰行智成（安徽）建材有限公司", "340000", "安徽省", "水泥生产",
            CeGbIndustryClassification.CEMENT
        );
    }

    private void seedCompanyFactoryReferenceRecord(String companySk,
                                                   String companyCode,
                                                   String factoryCode,
                                                   String companyName,
                                                   String factoryName,
                                                   String provinceCode,
                                                   String provinceName,
                                                   String factoryType,
                                                   IndustryRecord industry) {
        jdbcTemplate.update("""
            IF NOT EXISTS (
                SELECT 1 FROM ce_company_factory WHERE factory_code = ?
            )
            INSERT INTO ce_company_factory (
                company_sk, company_code, factory_code, company_name, factory_name,
                province_code, province_name, factory_type,
                industry_section_code, industry_section_name,
                industry_division_code, industry_division_name,
                industry_group_code, industry_group_name,
                industry_class_code, industry_class_name,
                effective_date, expiry_date, is_active, create_time, remark
            )
            VALUES (
                ?, ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?,
                ?, ?,
                ?, ?,
                ?, ?,
                '2024-01-01', '9999-12-31', N'Y', SYSDATETIME(), ?
            )
            """,
            factoryCode,
            companySk, companyCode, factoryCode, companyName, factoryName,
            provinceCode, provinceName, factoryType,
            industry.sectionCode(), industry.sectionName(),
            industry.divisionCode(), industry.divisionName(),
            industry.groupCode(), industry.groupName(),
            industry.classCode(), industry.className(),
            "source(A)"
        );
    }

    private void migrateLegacyIndustryClassificationRecord(String oldSectionCode,
                                                           String oldDivisionCode,
                                                           String oldGroupCode,
                                                           String oldClassCode,
                                                           IndustryRecord record) {
        jdbcTemplate.update("""
            IF EXISTS (
                SELECT 1
                  FROM ce_industry_classification
                 WHERE industry_section_code = ?
                   AND ISNULL(industry_division_code, '') = ISNULL(?, '')
                   AND ISNULL(industry_group_code, '') = ISNULL(?, '')
                   AND ISNULL(industry_class_code, '') = ISNULL(?, '')
                   AND remark = N'%s'
            )
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                      FROM ce_industry_classification
                     WHERE industry_section_code = ?
                       AND ISNULL(industry_division_code, '') = ISNULL(?, '')
                       AND ISNULL(industry_group_code, '') = ISNULL(?, '')
                       AND ISNULL(industry_class_code, '') = ISNULL(?, '')
                )
                    UPDATE ce_industry_classification
                       SET industry_section_code = ?,
                           industry_section_name = ?,
                           industry_division_code = NULLIF(?, ''),
                           industry_division_name = NULLIF(?, ''),
                           industry_group_code = NULLIF(?, ''),
                           industry_group_name = NULLIF(?, ''),
                           industry_class_code = NULLIF(?, ''),
                           industry_class_name = NULLIF(?, ''),
                           sort_order = ?,
                           status = N'active',
                           update_time = SYSDATETIME(),
                           remark = N'%s'
                     WHERE industry_section_code = ?
                       AND ISNULL(industry_division_code, '') = ISNULL(?, '')
                       AND ISNULL(industry_group_code, '') = ISNULL(?, '')
                       AND ISNULL(industry_class_code, '') = ISNULL(?, '')
                       AND remark = N'%s'
                ELSE
                    DELETE FROM ce_industry_classification
                     WHERE industry_section_code = ?
                       AND ISNULL(industry_division_code, '') = ISNULL(?, '')
                       AND ISNULL(industry_group_code, '') = ISNULL(?, '')
                       AND ISNULL(industry_class_code, '') = ISNULL(?, '')
                       AND remark = N'%s'
            END
            """.formatted(CeGbIndustryClassification.LEGACY_REMARK, CeGbIndustryClassification.REMARK,
                CeGbIndustryClassification.LEGACY_REMARK, CeGbIndustryClassification.LEGACY_REMARK),
            oldSectionCode, oldDivisionCode, oldGroupCode, oldClassCode,
            record.sectionCode(), record.divisionCode(), record.groupCode(), record.classCode(),
            record.sectionCode(), record.sectionName(), record.divisionCode(), record.divisionName(),
            record.groupCode(), record.groupName(), record.classCode(), record.className(), record.sortOrder(),
            oldSectionCode, oldDivisionCode, oldGroupCode, oldClassCode,
            oldSectionCode, oldDivisionCode, oldGroupCode, oldClassCode
        );
    }

    private void seedIndustryClassificationRecord(IndustryRecord record) {
        jdbcTemplate.update("""
            IF NOT EXISTS (
                SELECT 1
                  FROM ce_industry_classification
                 WHERE industry_section_code = ?
                   AND ISNULL(industry_division_code, '') = ISNULL(?, '')
                   AND ISNULL(industry_group_code, '') = ISNULL(?, '')
                   AND ISNULL(industry_class_code, '') = ISNULL(?, '')
            )
            INSERT INTO ce_industry_classification (
                industry_section_code, industry_section_name,
                industry_division_code, industry_division_name,
                industry_group_code, industry_group_name,
                industry_class_code, industry_class_name,
                sort_order, status, create_time, remark
            )
            VALUES (
                ?, ?,
                NULLIF(?, ''), NULLIF(?, ''),
                NULLIF(?, ''), NULLIF(?, ''),
                NULLIF(?, ''), NULLIF(?, ''),
                ?, N'active', SYSDATETIME(), N'%s'
            )
            """.formatted(CeGbIndustryClassification.REMARK),
            record.sectionCode(), record.divisionCode(), record.groupCode(), record.classCode(),
            record.sectionCode(), record.sectionName(), record.divisionCode(), record.divisionName(),
            record.groupCode(), record.groupName(), record.classCode(), record.className(), record.sortOrder()
        );
    }

    private void markSeededIndustryClassificationRecord(String sectionCode, String divisionCode,
                                                        String groupCode, String classCode) {
        jdbcTemplate.update("""
            UPDATE ce_industry_classification
               SET remark = N'%s',
                   update_time = SYSDATETIME()
             WHERE industry_section_code = ?
               AND ISNULL(industry_division_code, '') = ISNULL(?, '')
               AND ISNULL(industry_group_code, '') = ISNULL(?, '')
               AND ISNULL(industry_class_code, '') = ISNULL(?, '')
               AND remark = N'%s'
            """.formatted(CeGbIndustryClassification.REMARK, CeGbIndustryClassification.LEGACY_REMARK),
            sectionCode, divisionCode, groupCode, classCode
        );
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

    private void alterColumnIfExists(String tableName, String columnName, String columnDef) {
        try {
            if (!columnExists(tableName, columnName)) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + columnDef);
            log.info("[SchemaMigration] altered {}.{} ({})", tableName, columnName, columnDef);
        } catch (Exception e) {
            log.warn("[SchemaMigration] failed to alter {}.{}: {}", tableName, columnName, e.getMessage());
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
                UPDATE es
                   SET es.factory_code = cf.factory_code,
                       es.company_code = cf.company_code,
                       es.company_name = COALESCE(NULLIF(es.company_name, ''), cf.company_name),
                       es.factory_name = COALESCE(NULLIF(es.factory_name, ''), cf.factory_name)
                  FROM ce_emission_source es
                  JOIN ce_company_factory cf
                    ON cf.factory_code = es.company_code
                 WHERE (es.factory_code IS NULL OR es.factory_code = '')
                   AND es.company_code IS NOT NULL
                   AND es.company_code <> ''
                """);
            jdbcTemplate.update("""
                UPDATE es
                   SET es.company_code = cf.company_code,
                       es.company_name = COALESCE(NULLIF(es.company_name, ''), cf.company_name),
                       es.factory_name = COALESCE(NULLIF(es.factory_name, ''), cf.factory_name)
                  FROM ce_emission_source es
                  JOIN ce_company_factory cf
                    ON cf.factory_code = es.factory_code
                 WHERE es.factory_code IS NOT NULL
                   AND es.factory_code <> ''
                   AND (es.company_code IS NULL OR es.company_code = '' OR es.company_code <> cf.company_code)
                """);
            jdbcTemplate.update("""
                UPDATE ad
                   SET ad.factory_code = cf.factory_code,
                       ad.company_code = cf.company_code,
                       ad.company_name = COALESCE(NULLIF(ad.company_name, ''), cf.company_name),
                       ad.factory_name = COALESCE(NULLIF(ad.factory_name, ''), cf.factory_name)
                  FROM ce_activity_data ad
                  JOIN ce_company_factory cf
                    ON cf.factory_code = ad.company_code
                 WHERE (ad.factory_code IS NULL OR ad.factory_code = '')
                   AND ad.company_code IS NOT NULL
                   AND ad.company_code <> ''
                """);
            jdbcTemplate.update("""
                UPDATE ad
                   SET ad.company_code = cf.company_code,
                       ad.company_name = COALESCE(NULLIF(ad.company_name, ''), cf.company_name),
                       ad.factory_name = COALESCE(NULLIF(ad.factory_name, ''), cf.factory_name)
                  FROM ce_activity_data ad
                  JOIN ce_company_factory cf
                    ON cf.factory_code = ad.factory_code
                 WHERE ad.factory_code IS NOT NULL
                   AND ad.factory_code <> ''
                   AND (ad.company_code IS NULL OR ad.company_code = '' OR ad.company_code <> cf.company_code)
                """);
            jdbcTemplate.update("""
                UPDATE ad
                   SET ad.emission_source_id = es.id,
                       ad.activity_period = COALESCE(NULLIF(ad.activity_period, ''), CONCAT(ad.activity_year, '-', RIGHT('0' + CAST(ad.activity_month AS VARCHAR(2)), 2))),
                       ad.factory_code = COALESCE(NULLIF(ad.factory_code, ''), es.factory_code),
                       ad.company_code = COALESCE(NULLIF(ad.company_code, ''), es.company_code)
                  FROM ce_activity_data ad
                  JOIN ce_emission_source es
                    ON es.source_identification_code = ad.source_identification_code
                   AND (
                        es.factory_code = ad.factory_code
                        OR (ad.factory_code IS NULL OR ad.factory_code = '')
                   )
                 WHERE (ad.emission_source_id IS NULL OR ad.emission_source_id = 0 OR ad.activity_period IS NULL OR ad.activity_period = '' OR ad.factory_code IS NULL OR ad.factory_code = '')
             """);
        } catch (Exception e) {
            log.warn("[SchemaMigration] Source(A) relationship column backfill skipped: {}", e.getMessage());
        }
    }

    private void backfillEfFactorRecordCodes() {
        try {
            jdbcTemplate.update("""
                IF OBJECT_ID(N'dbo.ce_ef_factor', N'U') IS NOT NULL
                BEGIN
                    ;WITH existing AS (
                        SELECT ISNULL(MAX(TRY_CONVERT(INT, factor_sk)), 0) AS max_no
                          FROM dbo.ce_ef_factor
                         WHERE TRY_CONVERT(INT, factor_sk) IS NOT NULL
                    ),
                    blank_rows AS (
                        SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
                          FROM dbo.ce_ef_factor
                         WHERE NULLIF(LTRIM(RTRIM(factor_sk)), N'') IS NULL
                    )
                    UPDATE f
                       SET factor_sk = CONVERT(NVARCHAR(64), blank_rows.rn + existing.max_no)
                      FROM dbo.ce_ef_factor f
                      JOIN blank_rows ON blank_rows.id = f.id
                     CROSS JOIN existing;

                    IF OBJECT_ID(N'dbo.ce_emission_source', N'U') IS NOT NULL
                    BEGIN
                        ;WITH mapped AS (
                            SELECT
                                factor_sk AS old_key,
                                CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(factor_sk, 4))) AS new_key
                              FROM dbo.ce_ef_factor f
                             WHERE factor_sk LIKE N'EF201-[0-9][0-9][0-9][0-9]'
                               AND TRY_CONVERT(INT, RIGHT(factor_sk, 4)) IS NOT NULL
                               AND NOT EXISTS (
                                   SELECT 1
                                     FROM dbo.ce_ef_factor existing
                                    WHERE existing.factor_sk = CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(f.factor_sk, 4)))
                               )
                        )
                        UPDATE es
                           SET factor_key = mapped.new_key
                          FROM dbo.ce_emission_source es
                          JOIN mapped ON es.factor_key = mapped.old_key;
                    END

                    IF OBJECT_ID(N'dbo.ce_activity_data', N'U') IS NOT NULL
                    BEGIN
                        ;WITH mapped AS (
                            SELECT
                                factor_sk AS old_key,
                                CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(factor_sk, 4))) AS new_key
                              FROM dbo.ce_ef_factor f
                             WHERE factor_sk LIKE N'EF201-[0-9][0-9][0-9][0-9]'
                               AND TRY_CONVERT(INT, RIGHT(factor_sk, 4)) IS NOT NULL
                               AND NOT EXISTS (
                                   SELECT 1
                                     FROM dbo.ce_ef_factor existing
                                    WHERE existing.factor_sk = CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(f.factor_sk, 4)))
                               )
                        )
                        UPDATE ad
                           SET factor_key = mapped.new_key
                          FROM dbo.ce_activity_data ad
                          JOIN mapped ON ad.factor_key = mapped.old_key;
                    END

                    IF OBJECT_ID(N'dbo.ce_green_power_certificate', N'U') IS NOT NULL
                    BEGIN
                        ;WITH mapped AS (
                            SELECT
                                factor_sk AS old_key,
                                CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(factor_sk, 4))) AS new_key
                              FROM dbo.ce_ef_factor f
                             WHERE factor_sk LIKE N'EF201-[0-9][0-9][0-9][0-9]'
                               AND TRY_CONVERT(INT, RIGHT(factor_sk, 4)) IS NOT NULL
                               AND NOT EXISTS (
                                   SELECT 1
                                     FROM dbo.ce_ef_factor existing
                                    WHERE existing.factor_sk = CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(f.factor_sk, 4)))
                               )
                        )
                        UPDATE gp
                           SET factor_key = mapped.new_key
                          FROM dbo.ce_green_power_certificate gp
                          JOIN mapped ON gp.factor_key = mapped.old_key;
                    END

                    ;WITH mapped AS (
                        SELECT
                            id,
                            CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(factor_sk, 4))) AS new_key
                          FROM dbo.ce_ef_factor f
                         WHERE factor_sk LIKE N'EF201-[0-9][0-9][0-9][0-9]'
                           AND TRY_CONVERT(INT, RIGHT(factor_sk, 4)) IS NOT NULL
                           AND NOT EXISTS (
                               SELECT 1
                                 FROM dbo.ce_ef_factor existing
                                WHERE existing.factor_sk = CONVERT(NVARCHAR(64), TRY_CONVERT(INT, RIGHT(f.factor_sk, 4)))
                           )
                    )
                    UPDATE f
                       SET factor_sk = mapped.new_key
                      FROM dbo.ce_ef_factor f
                      JOIN mapped ON mapped.id = f.id;
                END
                """);
        } catch (Exception e) {
            log.warn("[SchemaMigration] EF factor code backfill skipped: {}", e.getMessage());
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
