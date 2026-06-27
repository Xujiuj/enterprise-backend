package org.dromara.enterprise.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fails fast when production deployment secrets are absent.
 */
public class EnterpriseDeploymentConfigValidator implements EnvironmentPostProcessor, Ordered {

    private static final String VALIDATION_ENABLED = "enterprise.deployment.validation.enabled";

    private static final String[] REQUIRED_PROPERTIES = {
        "spring.datasource.dynamic.datasource.master.url",
        "spring.datasource.dynamic.datasource.master.username",
        "spring.datasource.dynamic.datasource.master.password"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean prod = List.of(environment.getActiveProfiles()).contains("prod");
        boolean enabled = environment.getProperty(VALIDATION_ENABLED, Boolean.class, prod);
        if (!enabled) {
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String property : REQUIRED_PROPERTIES) {
            String value = environment.getProperty(property);
            if (value == null || value.trim().isEmpty()) {
                missing.add(property);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("""
                Enterprise deployment configuration is incomplete.
                Missing required properties: %s
                Configure enterprise-managed secrets through ENTERPRISE_DB_URL, ENTERPRISE_DB_USERNAME, ENTERPRISE_DB_PASSWORD,
                or generate local encrypted config with: java -jar ruoyi-admin.jar --enterprise-setup
                """.formatted(String.join(", ", missing)));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }
}
