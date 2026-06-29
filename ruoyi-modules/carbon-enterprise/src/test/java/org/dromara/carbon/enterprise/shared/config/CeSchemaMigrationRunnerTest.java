package org.dromara.carbon.enterprise.shared.config;

import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityValidationServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
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
        when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM sys.key_constraints WHERE parent_object_id = OBJECT_ID(?) AND name = ? AND type = 'UQ'"),
            eq(Integer.class),
            any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM sys_menu WHERE menu_id = ? OR path = ?"),
            eq(Integer.class),
            any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.update(
            eq("""
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
                    ?, N'active', SYSDATETIME(), N'Source(A) 102公司表参考数据'
                )
                """),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(1);

        new CeSchemaMigrationRunner(jdbcTemplate).run();

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
        verify(jdbcTemplate, times(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().size()))
            .update(
                eq("""
                    INSERT INTO ce_template_field (
                        sheet_id, field_order, original_field_name, target_column_code, business_field_code,
                        value_type, required_flag, original_field_flag, extensible_flag, create_time
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())
                    """),
                any(), any(), any(), any(), any(), any(), any(), any(), any()
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
                any(), any(), any()
            );
        verify(jdbcTemplate, atLeastOnce()).queryForObject(
            eq("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_NAME = ? AND COLUMN_NAME = ?"),
            eq(Integer.class),
            eq("ce_template_field"), eq("target_column_code")
        );
        verifyIndustryReferenceSeeds(jdbcTemplate);
    }

    @Test
    void repairsEmissionActivityTemplateWhenSheetExistsWithoutFields() {
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
            eq("""
                SELECT TOP 1 id
                  FROM ce_template_sheet
                 WHERE target_table_code = ?
                 ORDER BY id DESC
                """),
            eq(Long.class),
            eq("emission_activity")
        )).thenReturn(20L);
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
        when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM sys.key_constraints WHERE parent_object_id = OBJECT_ID(?) AND name = ? AND type = 'UQ'"),
            eq(Integer.class),
            any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM sys_menu WHERE menu_id = ? OR path = ?"),
            eq(Integer.class),
            any(), any()
        )).thenReturn(1);
        when(jdbcTemplate.update(
            eq("""
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
                    ?, N'active', SYSDATETIME(), N'Source(A) 102公司表参考数据'
                )
                """),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(1);

        new CeSchemaMigrationRunner(jdbcTemplate).run();

        verify(jdbcTemplate).update(
            eq("UPDATE ce_template_sheet SET field_count = ? WHERE id = ?"),
            eq(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().size()), eq(20L)
        );
        verify(jdbcTemplate, times(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().size()))
            .update(
                eq("""
                    INSERT INTO ce_template_field (
                        sheet_id, field_order, original_field_name, target_column_code, business_field_code,
                        value_type, required_flag, original_field_flag, extensible_flag, create_time
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())
                    """),
                eq(20L), any(), any(), any(), any(), any(), any(), any(), any()
            );
        verifyIndustryReferenceSeeds(jdbcTemplate);
    }

    private void verifyIndustryReferenceSeeds(JdbcTemplate jdbcTemplate) {
        verify(jdbcTemplate, times(4)).update(
            eq("""
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
                    ?, N'active', SYSDATETIME(), N'Source(A) 102公司表参考数据'
                )
                """),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }
}
