package org.dromara.carbon.enterprise;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class EnterprisePortalMenuContractTest {

    @Test
    void enterpriseMenuSqlKeepsEnterpriseMenusAndBlocksVendorMenus() throws Exception {
        String sql = Files.readString(resolveFromWorkspace("script/sql/portal/enterprise_portal_menu.sql"));

        assertThat(sql).contains("delete from sys_menu where menu_id between 900100 and 900230");
        assertThat(sql).contains("delete from sys_role_menu where menu_id between 900100 and 900230");
        assertThat(extractMenuIds(sql)).containsExactlyInAnyOrder(
            900100, 900102, 900103, 900104, 900105, 900106,
            900110, 900111, 900112, 900113, 900114, 900115,
            900120, 900121, 900122, 900123, 900124, 900125, 900126, 900127,
            900130, 900131, 900132,
            900140, 900141,
            900150, 900151, 900152, 900153, 900154,
            900160, 900161, 900162, 900163,
            900170, 900171, 900172, 900173, 900174,
            900180, 900181, 900182, 900183, 900184,
            900185, 900186, 900187, 900188, 900189,
            900190, 900191, 900192, 900193, 900194,
            900195, 900196, 900197, 900198, 900199,
            900200, 900201, 900202, 900203, 900204,
            900205, 900206, 900207, 900208, 900209, 900210,
            900211, 900212, 900213, 900214,
            900215, 900216, 900217, 900218
        );
        assertThat(sql).contains(
            "系统授权",
            "授权管理",
            "数据管理",
            "配置排放源",
            "行政区划",
            "公司表",
            "排放源分类",
            "排放源识别",
            "基准年维度表",
            "确认排放因子",
            "EF排放因子维度表",
            "EF电力因子维度表",
            "EF电力因子版本对应",
            "EF电力因子口径维度",
            "温室气体维度",
            "活动数据",
            "排放活动数据",
            "绿电绿证",
            "绿电绿证数据",
            "强度管理",
            "碳排放强度分母维度表",
            "强度目标表",
            "分母事实表",
            "碳排放强度容忍率参数表",
            "报表管理",
            "Content",
            "数据验证",
            "报表模板下载"
        );
        assertThat(sql).contains(
            "enterprise/licenseImport/index",
            "enterprise/dimension/index",
            "enterprise/emissionSource/index",
            "enterprise/factorConfirmation/index",
            "enterprise/factorCacheRecord/index",
            "enterprise/activityData/index",
            "enterprise/activityEntry/index",
            "enterprise/greenPowerCertificate/index",
            "enterprise/intensityMetric/index",
            "enterprise/reportTemplateFile/index",
            "enterprise/reports/index",
            "enterprise/dataValidation/index",
            "enterprise:licenseImport:import",
            "enterprise:licenseState:query",
            "enterprise:dimension:view",
            "enterprise:dimension:list",
            "enterprise:dimension:query",
            "enterprise:dimension:add",
            "enterprise:dimension:edit",
            "enterprise:dimension:remove",
            "enterprise:emissionSource:list",
            "enterprise:emissionSource:query",
            "enterprise:emissionSource:add",
            "enterprise:emissionSource:edit",
            "enterprise:emissionSource:remove",
            "enterprise:activityData:list",
            "enterprise:activityData:query",
            "enterprise:activityImportValidation:validate",
            "enterprise:activity:save",
            "enterprise:activityImport:import",
            "enterprise:factorConfirmation:list",
            "enterprise:factorConfirmation:query",
            "enterprise:factorConfirmation:add",
            "enterprise:factorConfirmation:edit",
            "enterprise:factorConfirmation:remove",
            "enterprise:factorSync:run",
            "enterprise:factorCacheRecord:list",
            "enterprise:factorCacheRecord:query",
            "enterprise:extensionField:list",
            "enterprise:extensionFieldValue:list",
            "enterprise:extensionFieldValue:add",
            "enterprise:extensionFieldValue:edit",
            "enterprise:greenPowerCertificate:list",
            "enterprise:greenPowerCertificate:query",
            "enterprise:greenPowerCertificate:add",
            "enterprise:greenPowerCertificate:edit",
            "enterprise:greenPowerCertificate:remove",
            "enterprise:intensityMetric:list",
            "enterprise:intensityMetric:query",
            "enterprise:intensityMetric:add",
            "enterprise:intensityMetric:edit",
            "enterprise:intensityMetric:remove",
            "enterprise:reportTemplateFile:list",
            "enterprise:reportTemplateFile:query",
            "enterprise:reportTemplateFile:add",
            "enterprise:reportTemplateFile:edit",
            "enterprise:reportTemplateFile:remove",
            "enterprise:reportTemplateFile:download",
            "enterprise:reportTemplateSync:run",
            "enterprise:reports:view",
            "enterprise:dataValidation:view"
        );
        assertThat(sql).contains(
            "insert ignore into sys_role_menu (role_id, menu_id)",
            "r.role_key in ('test1', 'test2')",
            "m.menu_id between 900100 and 900230",
            "m.menu_id <> 900131"
        );
        assertThat(sql).contains(
            "(900106, '数据管理', 0, 2, 'data-management', 'Layout'",
            "(900111, '行政区划', 900106",
            "(900113, '排放源分类', 900106",
            "(900115, '基准年维度表', 900106",
            "(900122, 'EF电力因子维度表', 900106",
            "(900123, 'EF电力因子版本对应', 900106",
            "(900124, 'EF电力因子口径维度', 900106",
            "(900125, '温室气体维度', 900106",
            "(900127, '因子缓存记录', 900106",
            "(900163, '报表模板下载', 900106",
            "(900211, '厂商因子同步', 900106",
            "(900212, '厂商模板同步', 900106"
        );
        assertThat(sql).doesNotContain(
            "enterprise:activityDataRaw:add",
            "enterprise:activityDataRaw:edit",
            "enterprise:activityDataRaw:remove",
            "enterprise:activityData:add",
            "enterprise:activityData:edit",
            "enterprise:activityData:remove",
            "system/emissionSource/index",
            "system/factorConfirm/index",
            "system/factorLibrary/index",
            "system/activityData/index",
            "system/greenElectricity/index",
            "system/intensityDenominator/index",
            "system/intensity/index",
            "system/submissionTracking/index",
            "system/reportTemplate/index",
            "客户档案",
            "License 签发",
            "License 续签",
            "模板分发",
            "续费订单",
            "vendor:"
        );
    }

    @Test
    void enterpriseSchemaSqlOwnsBusinessTablesAndBlocksVendorTables() throws Exception {
        String mysql = Files.readString(resolveFromWorkspace("script/sql/mysql/carbon_enterprise_schema_v1.sql"));
        String sqlserver = Files.readString(resolveFromWorkspace("script/sql/sqlserver/carbon_enterprise_schema_v1.sql"));

        assertThat(createTableNames(mysql)).contains(
            "ce_template_version",
            "ce_template_sheet",
            "ce_template_field",
            "ce_capture_batch",
            "ce_capture_row",
            "ce_capture_cell",
            "ce_extension_field",
            "ce_dimension_record",
            "ce_license_state",
            "ce_factor_cache_version",
            "ce_emission_source",
            "ce_factor_confirmation",
            "ce_activity_data",
            "ce_green_power_certificate",
            "ce_intensity_metric",
            "ce_report_template_file"
        );
        assertThat(createTableNames(sqlserver)).contains(
            "ce_dimension_record",
            "ce_emission_source",
            "ce_factor_confirmation",
            "ce_activity_data",
            "ce_green_power_certificate",
            "ce_intensity_metric",
            "ce_report_template_file"
        );
        assertThat(mysql).contains("INSERT INTO ce_report_template_file");
        assertThat(mysql).contains("feature_codes", "payload_digest", "current_summary");
        assertThat(sqlserver).contains("feature_codes", "payload_digest", "current_summary");
        assertThat(sqlserver).contains(
            "rpt.v_ActivityDataFact",
            "rpt.v_GreenElectricityFact",
            "rpt.v_IntensityMetricFact"
        );

        assertThat(mysql).doesNotContain("CREATE TABLE IF NOT EXISTS cv_");
        assertThat(sqlserver).doesNotContain("CREATE TABLE cv_");
        assertThat(mysql).doesNotContain("vendor:");
        assertThat(sqlserver).doesNotContain("vendor:");
        assertThat(mysql).doesNotContain(
            "'admin-division'",
            "'emission-source-category'",
            "'base-year'",
            "'ef-electricity-factor'",
            "'ef-electricity-version'",
            "'ef-electricity-scope'",
            "'greenhouse-gas'",
            "'report-template-download'",
            "'vendor' AS source_type",
            ", 'vendor',"
        );
    }

    @Test
    void baseRuoyiMenuSqlKeepsSystemManagementLogsAndCodeGeneration() throws Exception {
        String sql = Files.readString(resolveFromWorkspace("script/sql/ry_vue_5.X.sql"));

        assertThat(sql).contains(
            "system/user/index",
            "system/role/index",
            "system/menu/index",
            "system/dept/index",
            "system/post/index",
            "system/dict/index",
            "system/config/index",
            "system/notice/index",
            "monitor/logininfor/index",
            "monitor/operlog/index",
            "tool/gen/index"
        );
    }

    private static Set<Integer> extractMenuIds(String sql) {
        return Pattern.compile("\\((900[12]\\d\\d),")
            .matcher(sql)
            .results()
            .map(MatchResult::group)
            .map(value -> value.substring(1, value.length() - 1))
            .map(Integer::valueOf)
            .collect(Collectors.toSet());
    }

    private static Set<String> createTableNames(String sql) {
        return Pattern.compile("(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-zA-Z0-9_]+)")
            .matcher(sql)
            .results()
            .map(result -> result.group(1).toLowerCase())
            .collect(Collectors.toSet());
    }

    private static Path resolveFromWorkspace(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find " + relativePath + " from current working directory");
    }
}
