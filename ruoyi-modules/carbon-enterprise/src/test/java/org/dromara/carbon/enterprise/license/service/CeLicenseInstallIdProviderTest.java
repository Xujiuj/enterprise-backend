package org.dromara.carbon.enterprise.license.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class CeLicenseInstallIdProviderTest {

    @Test
    void buildsStableInstallIdFromMachineFingerprint() throws Exception {
        CeLicenseInstallIdProvider provider = new CeLicenseInstallIdProvider(
            () -> List.of("mac=001122334455", "host=machine-a", "mac=001122334455"));

        String expectedDigest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest("host=machine-a|mac=001122334455".getBytes(StandardCharsets.UTF_8)))
            .substring(0, 24)
            .toUpperCase(Locale.ROOT);

        assertEquals("INSTALL-AUTO-" + expectedDigest, provider.getExpectedInstallId());
        assertEquals("INSTALL-AUTO-" + expectedDigest, provider.getExpectedInstallId());
    }
}
