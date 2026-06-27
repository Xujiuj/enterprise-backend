package org.dromara.enterprise.config;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * CLI helper for enterprises without managed secrets.
 */
public final class EnterpriseSecureConfigSetup {

    private static final String DB_URL = "spring.datasource.dynamic.datasource.master.url";
    private static final String DB_USERNAME = "spring.datasource.dynamic.datasource.master.username";
    private static final String DB_PASSWORD = "spring.datasource.dynamic.datasource.master.password";
    private static final String REDIS_HOST = "spring.data.redis.host";
    private static final String REDIS_PORT = "spring.data.redis.port";
    private static final String REDIS_PASSWORD = "spring.data.redis.password";

    private EnterpriseSecureConfigSetup() {
    }

    public static void run(String[] args) throws Exception {
        SetupOptions options = SetupOptions.from(args);
        Console console = System.console();
        Scanner scanner = console == null ? new Scanner(System.in, StandardCharsets.UTF_8) : null;

        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(DB_URL, prompt(console, scanner, "Enterprise DB JDBC URL", null, false));
        properties.put(DB_USERNAME, prompt(console, scanner, "Enterprise DB username", null, false));
        properties.put(DB_PASSWORD, prompt(console, scanner, "Enterprise DB password", null, true));
        requireText(properties.get(DB_URL), "Enterprise DB JDBC URL");
        requireText(properties.get(DB_USERNAME), "Enterprise DB username");
        requireText(properties.get(DB_PASSWORD), "Enterprise DB password");
        properties.put(REDIS_HOST, prompt(console, scanner, "Redis host", "127.0.0.1", false));
        properties.put(REDIS_PORT, prompt(console, scanner, "Redis port", "6379", false));
        String redisPassword = prompt(console, scanner, "Redis password (blank if none)", "", true);
        if (!redisPassword.isBlank()) {
            properties.put(REDIS_PASSWORD, redisPassword);
        }

        if (options.testConnection()) {
            testConnection(properties.get(DB_URL), properties.get(DB_USERNAME), properties.get(DB_PASSWORD));
        }

        String masterKey = EnterpriseSecureConfig.generateMasterKey();
        String encrypted = EnterpriseSecureConfig.encrypt(properties, masterKey);
        writeSecret(options.configPath(), encrypted);
        writeSecret(options.masterKeyPath(), masterKey + System.lineSeparator());

        System.out.println("Encrypted enterprise config written to: " + options.configPath().toAbsolutePath());
        System.out.println("Master key written to: " + options.masterKeyPath().toAbsolutePath());
        System.out.println("Keep both files on the enterprise machine only. Do not send them to the vendor.");
    }

    private static String prompt(Console console, Scanner scanner, String label, String defaultValue, boolean secret) {
        String suffix = defaultValue == null ? ": " : " [" + defaultValue + "]: ";
        String value;
        if (console != null) {
            if (secret) {
                char[] chars = console.readPassword(label + suffix);
                value = chars == null ? "" : new String(chars);
            } else {
                value = console.readLine(label + suffix);
            }
        } else {
            System.out.print(label + suffix);
            value = scanner.nextLine();
        }
        if (value == null || value.isBlank()) {
            return defaultValue == null ? "" : defaultValue;
        }
        return value.trim();
    }

    private static void testConnection(String url, String username, String password) throws Exception {
        try (var ignored = DriverManager.getConnection(url, username, password)) {
            System.out.println("Database connection test succeeded.");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    private static void writeSecret(Path path, String content) throws Exception {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        path.toFile().setReadable(false, false);
        path.toFile().setReadable(true, true);
        path.toFile().setWritable(false, false);
        path.toFile().setWritable(true, true);
    }

    private record SetupOptions(Path configPath, Path masterKeyPath, boolean testConnection) {
        static SetupOptions from(String[] args) {
            Path configPath = Path.of(EnterpriseSecureConfig.DEFAULT_CONFIG_PATH);
            Path masterKeyPath = Path.of(EnterpriseSecureConfig.DEFAULT_MASTER_KEY_PATH);
            boolean testConnection = true;
            for (String arg : args) {
                if (arg.startsWith("--enterprise-secure-config=")) {
                    configPath = Path.of(arg.substring("--enterprise-secure-config=".length()));
                } else if (arg.startsWith("--enterprise-master-key=")) {
                    masterKeyPath = Path.of(arg.substring("--enterprise-master-key=".length()));
                } else if ("--skip-db-test".equals(arg)) {
                    testConnection = false;
                }
            }
            return new SetupOptions(configPath, masterKeyPath, testConnection);
        }
    }
}
