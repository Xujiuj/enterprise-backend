package org.dromara.carbon.enterprise;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class EnterprisePortalMenuContractTest {

    @Test
    void enterpriseSqlDirectoryKeepsOnlySqlServerDeliveryTarget() throws Exception {
        Path sqlDirectory = resolveFromWorkspace("script/sql");

        Set<String> sqlFiles;
        try (var stream = Files.walk(sqlDirectory)) {
            sqlFiles = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                .map(path -> sqlDirectory.relativize(path).toString().replace('\\', '/'))
                .collect(Collectors.toSet());
        }

        assertThat(sqlFiles)
            .doesNotContain("enterprise_init.sql", "enterprise_test_data.sql")
            .allSatisfy(path -> assertThat(path).startsWith("sqlserver/"));
    }

    @Test
    void enterpriseRuntimeConfigurationUsesSqlServerOnly() throws Exception {
        String secretTemplate = Files.readString(resolveFromWorkspace("deploy/enterprise-secrets.example.env"));
        String sourceAImportService = Files.readString(resolveFromWorkspace(
            "ruoyi-modules/carbon-enterprise/src/main/java/org/dromara/carbon/enterprise/sourcea/service/impl/CeSourceAImportServiceImpl.java"
        ));

        assertThat(secretTemplate).contains("jdbc:sqlserver://127.0.0.1:1433;DatabaseName=enterprise");
        assertThat(secretTemplate).doesNotContain("jdbc:" + "mysql");
        assertThat(sourceAImportService).doesNotContain("SET FOREIGN_" + "KEY_CHECKS");
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
