package org.dromara.enterprise.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads local encrypted deployment configuration before datasource auto configuration.
 */
public class EnterpriseSecureConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE_NAME = "enterpriseSecureConfig";
    private static final String CONFIG_PATH_PROPERTY = "enterprise.secure-config.path";
    private static final String MASTER_KEY_PATH_PROPERTY = "enterprise.secure-config.master-key-path";
    private static final String MASTER_KEY_ENV = "ENTERPRISE_SECURE_CONFIG_KEY";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path configPath = Path.of(environment.getProperty(CONFIG_PATH_PROPERTY, EnterpriseSecureConfig.DEFAULT_CONFIG_PATH));
        if (!Files.isRegularFile(configPath)) {
            return;
        }
        String masterKey = resolveMasterKey(environment);
        if (!hasText(masterKey)) {
            throw new IllegalStateException("Found " + configPath + " but no master key. Set " + MASTER_KEY_ENV
                + " or provide " + environment.getProperty(MASTER_KEY_PATH_PROPERTY, EnterpriseSecureConfig.DEFAULT_MASTER_KEY_PATH));
        }
        try {
            String payload = Files.readString(configPath, StandardCharsets.UTF_8);
            Map<String, Object> decrypted = EnterpriseSecureConfig.decrypt(payload, masterKey.trim());
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : decrypted.entrySet()) {
                if (entry.getValue() != null) {
                    properties.put(entry.getKey(), entry.getValue().toString());
                }
            }
            MapPropertySource propertySource = new MapPropertySource(SOURCE_NAME, properties);
            if (environment.getPropertySources().contains("systemEnvironment")) {
                environment.getPropertySources().addAfter("systemEnvironment", propertySource);
            } else {
                environment.getPropertySources().addLast(propertySource);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load encrypted enterprise config from " + configPath, e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private static String resolveMasterKey(ConfigurableEnvironment environment) {
        String envKey = environment.getProperty(MASTER_KEY_ENV);
        if (hasText(envKey)) {
            return envKey;
        }
        Path masterKeyPath = Path.of(environment.getProperty(MASTER_KEY_PATH_PROPERTY, EnterpriseSecureConfig.DEFAULT_MASTER_KEY_PATH));
        if (!Files.isRegularFile(masterKeyPath)) {
            return null;
        }
        try {
            return Files.readString(masterKeyPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read enterprise secure config master key from " + masterKeyPath, e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
