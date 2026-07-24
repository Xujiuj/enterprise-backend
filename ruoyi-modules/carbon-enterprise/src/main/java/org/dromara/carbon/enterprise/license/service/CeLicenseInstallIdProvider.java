package org.dromara.carbon.enterprise.license.service;

import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Provides the enterprise-local install id used by protected license gate checks.
 */
@Component
public class CeLicenseInstallIdProvider {

    private static final String AUTO_PREFIX = "INSTALL-AUTO-";
    private static final int DIGEST_LENGTH = 24;

    private final Supplier<List<String>> machineFingerprintPartsSupplier;

    public CeLicenseInstallIdProvider() {
        this(CeLicenseInstallIdProvider::collectMachineFingerprintParts);
    }

    CeLicenseInstallIdProvider(Supplier<List<String>> machineFingerprintPartsSupplier) {
        this.machineFingerprintPartsSupplier = machineFingerprintPartsSupplier;
    }

    public String getExpectedInstallId() {
        return buildAutoInstallId();
    }

    private String buildAutoInstallId() {
        List<String> parts = machineFingerprintPartsSupplier.get().stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .sorted()
            .toList();
        if (parts.isEmpty()) {
            return null;
        }
        String joined = String.join("|", parts);
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(joined.getBytes(StandardCharsets.UTF_8)));
            return AUTO_PREFIX + digest.substring(0, DIGEST_LENGTH).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private static List<String> collectMachineFingerprintParts() {
        List<String> parts = new ArrayList<>();
        addSystemProperty(parts, "os.name");
        addSystemProperty(parts, "os.arch");
        addSystemProperty(parts, "user.country");
        addEnvironment(parts, "COMPUTERNAME");
        addEnvironment(parts, "HOSTNAME");
        addEnvironment(parts, "PROCESSOR_IDENTIFIER");
        addFileContent(parts, Path.of("/etc/machine-id"));
        addFileContent(parts, Path.of("/var/lib/dbus/machine-id"));
        addHostName(parts);
        addNetworkInterfaces(parts);
        return parts;
    }

    private static void addSystemProperty(List<String> parts, String key) {
        addPart(parts, key + "=" + System.getProperty(key));
    }

    private static void addEnvironment(List<String> parts, String key) {
        addPart(parts, key + "=" + System.getenv(key));
    }

    private static void addFileContent(List<String> parts, Path path) {
        try {
            if (Files.isRegularFile(path)) {
                addPart(parts, path + "=" + Files.readString(path, StandardCharsets.UTF_8).trim());
            }
        } catch (Exception ignored) {
            // Some deployments restrict host identity files; other fingerprint parts are still usable.
        }
    }

    private static void addHostName(List<String> parts) {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            addPart(parts, "hostName=" + localHost.getHostName());
            addPart(parts, "canonicalHostName=" + localHost.getCanonicalHostName());
        } catch (Exception ignored) {
            // Hostname is best-effort only.
        }
    }

    private static void addNetworkInterfaces(List<String> parts) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress == null || hardwareAddress.length == 0) {
                    continue;
                }
                addPart(parts, "mac=" + HexFormat.of().formatHex(hardwareAddress));
            }
        } catch (Exception ignored) {
            // MAC addresses are best-effort only.
        }
    }

    private static void addPart(List<String> parts, String value) {
        if (StringUtils.isNotBlank(value) && !value.endsWith("=null")) {
            parts.add(value);
        }
    }
}
