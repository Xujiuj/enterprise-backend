package org.dromara.carbon.enterprise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

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

        assertThat(sql).contains("delete from sys_menu where menu_id between 900100 and 900108");
        assertThat(extractMenuIds(sql)).containsExactlyInAnyOrder(
            900100, 900101, 900102, 900103, 900104, 900105, 900106, 900107, 900108
        );
        assertThat(sql).contains(
            "企业本地业务",
            "License 授权",
            "01 配置排放源",
            "02 确认排放因子",
            "03 活动数据",
            "04 绿电绿证",
            "05 强度管理",
            "因子查询",
            "报表模板下载"
        );
        assertThat(sql).contains(
            "enterprise/licenseImport/index",
            "system/emissionSource/index",
            "system/factorConfirm/index",
            "system/activityData/index",
            "system/greenElectricity/index",
            "system/intensity/index",
            "system/factorLibrary/index",
            "system/reportTemplate/index",
            "enterprise:license:import",
            "enterprise:emissionSource:list",
            "enterprise:factorConfirm:list",
            "enterprise:activityData:list",
            "enterprise:greenElectricity:list",
            "enterprise:intensity:list",
            "enterprise:factor:query",
            "enterprise:reportTemplate:download"
        );
        assertThat(sql).doesNotContain(
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
            "ce_emission_source",
            "ce_factor_confirmation",
            "ce_activity_data",
            "ce_green_power_certificate",
            "ce_intensity_metric",
            "ce_report_template_file"
        );
        assertThat(mysql).contains("INSERT INTO ce_report_template_file");

        assertThat(mysql).doesNotContain("CREATE TABLE IF NOT EXISTS cv_");
        assertThat(sqlserver).doesNotContain("CREATE TABLE cv_");
        assertThat(mysql).doesNotContain("vendor:");
        assertThat(sqlserver).doesNotContain("vendor:");
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
        return Pattern.compile("\\((90010\\d),")
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
