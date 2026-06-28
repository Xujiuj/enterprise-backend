package org.dromara.carbon.enterprise.license.service;

import org.dromara.common.core.service.ConfigService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
public class CeLicensePublicKeyProviderTest {

    private static final String CONFIG_KEY = "carbon.license.public-key-pem";

    @Test
    public void returnsSysConfigPublicKey() {
        ConfigService configService = mock(ConfigService.class);
        when(configService.getConfigValue(CONFIG_KEY)).thenReturn("sys-config-public-key");

        CeLicensePublicKeyProvider provider = new CeLicensePublicKeyProvider(configService);

        assertEquals("sys-config-public-key", provider.getPublicKeyPem());
    }

    @Test
    public void returnsNullWhenNoPublicKeyIsConfigured() {
        ConfigService configService = mock(ConfigService.class);
        when(configService.getConfigValue(CONFIG_KEY)).thenReturn(" ");

        CeLicensePublicKeyProvider provider = new CeLicensePublicKeyProvider(configService);

        assertNull(provider.getPublicKeyPem());
    }
}
