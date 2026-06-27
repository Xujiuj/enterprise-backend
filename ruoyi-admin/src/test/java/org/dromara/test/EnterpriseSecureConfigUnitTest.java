package org.dromara.test;

import org.dromara.enterprise.config.EnterpriseDeploymentConfigValidator;
import org.dromara.enterprise.config.EnterpriseSecureConfig;
import org.dromara.enterprise.config.EnterpriseSecureConfigEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnterpriseSecureConfigUnitTest {

    @Tag("dev")
    @Tag("prod")
    @Test
    void encryptedConfigRoundTrips() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.dynamic.datasource.master.url", "jdbc:sqlserver://127.0.0.1:1433;DatabaseName=enterprise;encrypt=false;trustServerCertificate=true;SelectMethod=cursor");
        properties.put("spring.datasource.dynamic.datasource.master.username", "enterprise_user");
        properties.put("spring.datasource.dynamic.datasource.master.password", "secret");

        String masterKey = EnterpriseSecureConfig.generateMasterKey();
        String encrypted = EnterpriseSecureConfig.encrypt(properties, masterKey);

        assertEquals(properties, EnterpriseSecureConfig.decrypt(encrypted, masterKey));
    }

    @Tag("dev")
    @Tag("prod")
    @Test
    void encryptedConfigLoadsAfterEnvironmentSecretsSoEnterpriseManagedSecretsWin() throws Exception {
        Path tempDir = Files.createTempDirectory("enterprise-secure-config-test");
        Path encryptedPath = tempDir.resolve("secure-config.enc");
        Path keyPath = tempDir.resolve(".master.key");
        String masterKey = EnterpriseSecureConfig.generateMasterKey();
        Map<String, String> properties = Map.of(
            "spring.datasource.dynamic.datasource.master.url", "jdbc:sqlserver://127.0.0.1:1433;DatabaseName=local;encrypt=false;trustServerCertificate=true;SelectMethod=cursor"
        );
        Files.writeString(encryptedPath, EnterpriseSecureConfig.encrypt(properties, masterKey), StandardCharsets.UTF_8);
        Files.writeString(keyPath, masterKey, StandardCharsets.UTF_8);

        MockEnvironment environment = new MockEnvironment()
            .withProperty("enterprise.secure-config.path", encryptedPath.toString())
            .withProperty("enterprise.secure-config.master-key-path", keyPath.toString())
            .withProperty("spring.datasource.dynamic.datasource.master.url", "jdbc:sqlserver://127.0.0.1:1433;DatabaseName=from-env;encrypt=false;trustServerCertificate=true;SelectMethod=cursor");

        new EnterpriseSecureConfigEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertEquals("jdbc:sqlserver://127.0.0.1:1433;DatabaseName=from-env;encrypt=false;trustServerCertificate=true;SelectMethod=cursor",
            environment.getProperty("spring.datasource.dynamic.datasource.master.url"));
    }

    @Tag("dev")
    @Tag("prod")
    @Test
    void prodValidationFailsWithoutDatasourceSecrets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class,
            () -> new EnterpriseDeploymentConfigValidator().postProcessEnvironment(environment, new SpringApplication()));
    }
}
