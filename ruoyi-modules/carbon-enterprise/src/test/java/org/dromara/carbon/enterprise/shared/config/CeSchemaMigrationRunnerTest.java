package org.dromara.carbon.enterprise.shared.config;

import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityValidationServiceImpl;
import org.dromara.carbon.enterprise.shared.service.ICeCompanyFactoryDeptSyncService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeSchemaMigrationRunnerTest {

    @Test
    void seedsEmissionActivityTemplateWhenSheetIsMissing() {
        JdbcTemplate jdbcTemplate = baseJdbcTemplate();
        when(jdbcTemplate.queryForObject(
            eq("""
                SELECT TOP 1 id
                  FROM ce_template_version
                 WHERE version_code = ?
                 ORDER BY id DESC
                """),
            eq(Long.class),
            eq("enterprise-local-emission-activity")
        )).thenReturn(10L);
        when(jdbcTemplate.queryForObject(
            eq("""
                INSERT INTO ce_template_sheet (
                    template_version_id, source_file, source_group, sheet_name, sheet_type,
                    header_row, field_count, module_code, target_table_code, allow_extension, create_time
                )
                OUTPUT INSERTED.id
                VALUES (?, N'enterprise-local', N'activity', N'emission_activity', N'business',
                    ?, ?, ?, ?, ?, SYSDATETIME())
                """),
            eq(Long.class),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(20L);

        new CeSchemaMigrationRunner(jdbcTemplate, mock(ICeCompanyFactoryDeptSyncService.class)).run();

        verify(jdbcTemplate).queryForObject(
            eq("""
                SELECT TOP 1 id
                  FROM ce_template_sheet
                 WHERE target_table_code = ?
                 ORDER BY id DESC
                """),
            eq(Long.class),
            eq("emission_activity")
        );
        verify(jdbcTemplate).queryForObject(
            eq("""
                INSERT INTO ce_template_sheet (
                    template_version_id, source_file, source_group, sheet_name, sheet_type,
                    header_row, field_count, module_code, target_table_code, allow_extension, create_time
                )
                OUTPUT INSERTED.id
                VALUES (?, N'enterprise-local', N'activity', N'emission_activity', N'business',
                    ?, ?, ?, ?, ?, SYSDATETIME())
                """),
            eq(Long.class),
            eq(10L), eq(1), eq(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().size()),
            eq("activity-data"), eq("emission_activity"), eq(true)
        );
        verifyEmissionActivityFieldRepair(jdbcTemplate, 20L);
        verifyIndustryClassificationSeed(jdbcTemplate);
        verifyCompanyFactoryUniquenessConstraintCleanup(jdbcTemplate);
    }

    @Test
    void repairsEmissionActivityTemplateWhenSheetExistsWithoutFields() {
        JdbcTemplate jdbcTemplate = baseJdbcTemplate();
        when(jdbcTemplate.queryForObject(
            eq("""
                SELECT TOP 1 id
                  FROM ce_template_sheet
                 WHERE target_table_code = ?
                 ORDER BY id DESC
                """),
            eq(Long.class),
            eq("emission_activity")
        )).thenReturn(20L);

        new CeSchemaMigrationRunner(jdbcTemplate, mock(ICeCompanyFactoryDeptSyncService.class)).run();

        verify(jdbcTemplate).update(
            eq("UPDATE ce_template_sheet SET field_count = ? WHERE id = ?"),
            eq(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().size()), eq(20L)
        );
        verifyEmissionActivityFieldRepair(jdbcTemplate, 20L);
        verifyIndustryClassificationSeed(jdbcTemplate);
        verifyCompanyFactoryUniquenessConstraintCleanup(jdbcTemplate);
    }

    private JdbcTemplate baseJdbcTemplate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ?"),
            eq(Integer.class),
            any()
        )).thenReturn(1);
        when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ? AND COLUMN_NAME = ?"),
            eq(Integer.class),
            any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM sys.key_constraints WHERE parent_object_id = OBJECT_ID(?) AND name = ? AND type = 'UQ'"),
            eq(Integer.class),
            any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.queryForObject(
            eq("""
                SELECT TOP 1 id
                  FROM ce_template_sheet
                 WHERE target_table_code = ?
                 ORDER BY id DESC
                """),
            eq(Long.class),
            eq("emission_activity")
        )).thenThrow(new EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForObject(
            eq("""
                SELECT COUNT(*)
                  FROM ce_template_field
                 WHERE sheet_id = ?
                   AND (business_field_code = ? OR target_column_code = ?)
                """),
            eq(Integer.class),
            any(), any(), any()
        )).thenReturn(0);
        when(jdbcTemplate.update(
            contains("IF EXISTS"),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.update(
            contains("INSERT INTO ce_industry_classification"),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.update(
            eq("DELETE FROM sys_menu WHERE menu_id = ? OR path = ? OR menu_name = N'107 行业代码表'"),
            any(), any()
        )).thenReturn(1);
        return jdbcTemplate;
    }

    private void verifyEmissionActivityFieldRepair(JdbcTemplate jdbcTemplate, Long sheetId) {
        verify(jdbcTemplate, times(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().size()))
            .update(
                eq("""
                    INSERT INTO ce_template_field (
                        sheet_id, field_order, original_field_name, target_column_code, business_field_code,
                        value_type, required_flag, original_field_flag, extensible_flag, create_time
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())
                    """),
                eq(sheetId), any(), any(), any(), any(), any(), any(), any(), any()
            );
        verify(jdbcTemplate, times(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().size()))
            .queryForObject(
                eq("""
                    SELECT COUNT(*)
                      FROM ce_template_field
                     WHERE sheet_id = ?
                       AND (business_field_code = ? OR target_column_code = ?)
                    """),
                eq(Integer.class),
                eq(sheetId), any(), any()
            );
        verify(jdbcTemplate, atLeastOnce()).queryForObject(
            eq("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ? AND COLUMN_NAME = ?"),
            eq(Integer.class),
            eq("ce_template_field"), eq("target_column_code")
        );
    }

    private void verifyIndustryClassificationSeed(JdbcTemplate jdbcTemplate) {
        verify(jdbcTemplate, times(2)).update(
            contains("IF EXISTS"),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any()
        );
        verify(jdbcTemplate, times(CeGbIndustryClassification.records().size())).update(
            contains("INSERT INTO ce_industry_classification"),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
        verify(jdbcTemplate).update(
            contains("INSERT INTO ce_industry_classification"),
            eq("L"), eq("72"), eq("721"), eq("7211"),
            eq("L"), any(), eq("72"), any(), eq("721"), any(), eq("7211"), any(), any()
        );
        verify(jdbcTemplate).update(
            contains("INSERT INTO ce_industry_classification"),
            eq("C"), eq("39"), eq("398"), eq("3985"),
            eq("C"), any(), eq("39"), any(), eq("398"), any(), eq("3985"), any(), any()
        );
        verify(jdbcTemplate).update(
            contains("INSERT INTO ce_industry_classification"),
            eq("D"), eq("44"), eq("441"), eq("4411"),
            eq("D"), any(), eq("44"), any(), eq("441"), any(), eq("4411"), any(), any()
        );
        verify(jdbcTemplate).update(
            contains("INSERT INTO ce_industry_classification"),
            eq("C"), eq("30"), eq("301"), eq("3011"),
            eq("C"), any(), eq("30"), any(), eq("301"), any(), eq("3011"), any(), any()
        );
        verify(jdbcTemplate, times(2)).update(
            contains("SET remark = N'GB/T 4754-2017 行业分类'"),
            any(), any(), any(), any()
        );
        verify(jdbcTemplate).update(
            contains("UPDATE ce_company_factory"),
            eq("L"), any(), eq("72"), any(), eq("721"), any(), eq("7211"), any(),
            eq("S"), eq(""), eq(""), eq("")
        );
        verify(jdbcTemplate).update(
            contains("UPDATE ce_company_factory"),
            eq("C"), any(), eq("39"), any(), eq("398"), any(), eq("3985"), any(),
            eq("C"), eq("26"), eq("261"), eq("2614")
        );
        verify(jdbcTemplate, times(4)).update(
            contains("INSERT INTO ce_company_factory"),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any()
        );
        verify(jdbcTemplate).update(
            contains("INSERT INTO ce_company_factory"),
            eq("10102"), eq("2"), eq("101"), eq("10102"), any(), any(),
            eq("650000"), any(), eq("多晶硅生产"),
            eq("C"), any(), eq("39"), any(), eq("398"), any(), eq("3985"), any(), eq("source(A)")
        );
        verify(jdbcTemplate).update(
            eq("DELETE FROM sys_menu WHERE menu_id = ? OR path = ? OR menu_name = N'107 行业代码表'"),
            eq(900116L), eq("industry")
        );
    }

    private void verifyCompanyFactoryUniquenessConstraintCleanup(JdbcTemplate jdbcTemplate) {
        verify(jdbcTemplate).execute(eq("""
            IF OBJECT_ID(N'dbo.ce_company_factory', N'U') IS NOT NULL
               AND EXISTS (
                   SELECT 1
                     FROM sys.key_constraints
                    WHERE parent_object_id = OBJECT_ID(N'dbo.ce_company_factory')
                      AND name = N'uk_ce_company_factory'
                      AND type = 'UQ'
               )
            BEGIN
                ALTER TABLE dbo.ce_company_factory
                DROP CONSTRAINT [uk_ce_company_factory];
            END
            """));
    }
}
