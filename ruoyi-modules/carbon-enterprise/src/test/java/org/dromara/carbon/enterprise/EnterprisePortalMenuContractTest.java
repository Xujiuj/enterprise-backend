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

    private static final String INIT_SQL = "script/sql/enterprise_init.sql";
    private static final String TEST_DATA_SQL = "script/sql/enterprise_test_data.sql";

    @Test
    void enterpriseSqlDirectoryContainsOnlyInitAndTestData() throws Exception {
        Path sqlDirectory = resolveFromWorkspace("script/sql");

        Set<String> sqlFiles;
        try (var stream = Files.walk(sqlDirectory)) {
            sqlFiles = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                .map(path -> sqlDirectory.relativize(path).toString().replace('\\', '/'))
                .collect(Collectors.toSet());
        }

        assertThat(sqlFiles).containsExactlyInAnyOrder("enterprise_init.sql", "enterprise_test_data.sql");
    }

    @Test
    void enterpriseInitSqlCreatesCurrentSystemAndBusinessTables() throws Exception {
        String sql = Files.readString(resolveFromWorkspace(INIT_SQL));

        assertThat(sql).contains(
            "CREATE DATABASE IF NOT EXISTS enterprise",
            "USE enterprise"
        );
        assertThat(createTableNames(sql)).contains(
            "sys_menu",
            "sys_logininfor",
            "sys_dict_type",
            "sys_dict_data",
            "sys_dept",
            "sys_config",
            "sys_client",
            "sys_tenant_package",
            "sys_tenant",
            "sys_role_menu",
            "sys_role_dept",
            "sys_user_role",
            "sys_user_post",
            "sys_role",
            "sys_post",
            "sys_user",
            "sys_oss_config",
            "sys_oss",
            "sys_oper_log",
            "ce_admin_division",
            "ce_capture_batch",
            "ce_activity_data",
            "ce_capture_row",
            "ce_company_factory",
            "ce_base_year",
            "ce_capture_cell",
            "ce_extension_field",
            "ce_extension_field_value",
            "ce_intensity_metric",
            "ce_green_power_certificate",
            "ce_intensity_denominator_fact",
            "ce_emission_source_category",
            "ce_emission_source",
            "ce_electricity_factor_version_map",
            "ce_electricity_factor_scope",
            "ce_factor_cache_version",
            "ce_factor_cache_record",
            "ce_factor_confirmation",
            "ce_greenhouse_gas",
            "ce_electricity_factor",
            "ce_template_sheet",
            "ce_template_version",
            "ce_template_field",
            "ce_report_template_file",
            "ce_report_content",
            "ce_license_state",
            "ce_ef_factor",
            "ce_fuel_factor_calc",
            "ce_intensity_denominator_rule",
            "ce_intensity_target",
            "ce_intensity_tolerance"
        );
        assertThat(sql).doesNotContain(
            "CREATE DATABASE vendor",
            "USE vendor",
            "CREATE TABLE IF NOT EXISTS cv_",
            "CREATE TABLE cv_",
            "vendor:"
        );
    }

    @Test
    void enterpriseInitSqlSeedsMenusRolesAndLeastPrivilege() throws Exception {
        String sql = Files.readString(resolveFromWorkspace(INIT_SQL));

        assertThat(sql).contains(
            "enterprise_admin",
            "enterprise_data_entry",
            "enterprise_auditor",
            "enterprise_report_viewer",
            "enterprise/licenseImport/index",
            "enterprise/dimension/index",
            "enterprise/emissionSource/index",
            "enterprise/activityData/index",
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
            "enterprise:reportTemplateFile:download",
            "enterprise:reportTemplateSync:run",
            "enterprise:reports:view",
            "enterprise:dataValidation:view",
            "enterprise:sourceA:import",
            "enterprise:sourceA:validate"
        );

        Set<Integer> adminMenus = extractPresetRoleMenuIds(sql, 900001);
        Set<Integer> dataEntryMenus = extractPresetRoleMenuIds(sql, 900002);
        Set<Integer> auditorMenus = extractPresetRoleMenuIds(sql, 900003);
        Set<Integer> reportViewerMenus = extractPresetRoleMenuIds(sql, 900004);

        assertThat(adminMenus).contains(
            1, 100, 101, 102,
            900103, 900172, 900173, 900174,
            900182, 900183, 900184, 900189,
            900211, 900212, 900222
        );

        assertThat(dataEntryMenus).contains(
            900105, 900130, 900131, 900140, 900141, 900150, 900152,
            900185, 900186, 900187, 900188, 900189,
            900197, 900198, 900202, 900203,
            900217, 900218, 900219, 900220, 900221
        );
        assertThat(dataEntryMenus).doesNotContain(
            1, 100, 101, 102,
            900103, 900172, 900173, 900174,
            900184, 900194, 900199, 900204,
            900211, 900212, 900222
        );

        assertThat(auditorMenus).contains(
            900105, 900131, 900141, 900161, 900162, 900163,
            900170, 900171, 900185, 900186, 900187,
            900190, 900191, 900195, 900196,
            900200, 900201, 900205, 900206, 900210, 900220
        );
        assertThat(auditorMenus).doesNotContain(
            900103, 900172, 900173, 900174,
            900182, 900183, 900184, 900188, 900189,
            900192, 900193, 900194, 900197, 900198, 900199,
            900202, 900203, 900204, 900207, 900208, 900209,
            900211, 900212, 900217, 900218, 900219, 900221, 900222
        );

        assertThat(reportViewerMenus).containsExactlyInAnyOrder(
            900105, 900160, 900161, 900162, 900163, 900205, 900206, 900210
        );
    }

    @Test
    void enterpriseTestDataSqlOnlyWritesEnterpriseTablesAndTestUsers() throws Exception {
        String sql = Files.readString(resolveFromWorkspace(TEST_DATA_SQL));

        assertThat(sql).contains(
            "USE enterprise",
            "entry01",
            "audit01",
            "report01",
            "LIC-TEST-2026",
            "INSERT INTO sys_user",
            "INSERT INTO sys_user_role",
            "INSERT INTO ce_admin_division",
            "INSERT INTO ce_company_factory",
            "INSERT INTO ce_base_year",
            "INSERT INTO ce_emission_source_category",
            "INSERT INTO ce_ef_factor",
            "INSERT INTO ce_electricity_factor",
            "INSERT INTO ce_electricity_factor_version_map",
            "INSERT INTO ce_electricity_factor_scope",
            "INSERT INTO ce_greenhouse_gas",
            "INSERT INTO ce_fuel_factor_calc",
            "INSERT INTO ce_emission_source",
            "INSERT INTO ce_activity_data",
            "INSERT INTO ce_green_power_certificate",
            "INSERT INTO ce_intensity_denominator_rule",
            "INSERT INTO ce_intensity_target",
            "INSERT INTO ce_intensity_tolerance",
            "INSERT INTO ce_intensity_denominator_fact",
            "INSERT INTO ce_intensity_metric",
            "INSERT INTO ce_license_state",
            "INSERT INTO ce_factor_confirmation",
            "INSERT INTO ce_factor_cache_version",
            "INSERT INTO ce_factor_cache_record",
            "INSERT INTO ce_report_template_file"
        );
        assertThat(sql).doesNotContain(
            "CREATE DATABASE vendor",
            "USE vendor",
            "CREATE TABLE IF NOT EXISTS cv_",
            "CREATE TABLE cv_",
            "vendor:"
        );
    }

    @Test
    void enterpriseWorkbenchLinksUseCurrentPortalRoutes() throws Exception {
        String source = Files.readString(resolveFromWorkspace(
            "ruoyi-modules/carbon-enterprise/src/main/java/org/dromara/carbon/enterprise/workbench/service/impl/CeWorkbenchServiceImpl.java"
        ));

        assertThat(source).contains(
            "/system-auth/license-import",
            "/report-management/report-template-download",
            "/activity-data/emission-activity-data",
            "/factor-confirm/ef-factor"
        );
        assertThat(source).doesNotContain(
            "/license-manage/license-import",
            "/data-management/report-template-download",
            "/data-management/factor-cache-record",
            "/activity-data/emission-activity-entry",
            "/factor-confirm/factor-cache-record"
        );
    }

    private static Set<Integer> extractPresetRoleMenuIds(String sql, int roleId) {
        Pattern blockPattern = Pattern.compile(
            "(?is)SELECT\\s+" + roleId + ",\\s+menu_id\\s+FROM\\s+sys_menu\\s+WHERE\\s+menu_id\\s+IN\\s*\\((.*?)\\);"
        );
        var matcher = blockPattern.matcher(sql);
        assertThat(matcher.find()).as("role " + roleId + " menu matrix exists").isTrue();
        return Pattern.compile("\\b\\d+\\b")
            .matcher(matcher.group(1))
            .results()
            .map(MatchResult::group)
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
