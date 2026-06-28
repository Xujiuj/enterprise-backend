package org.dromara.carbon.enterprise.license.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.service.ConfigService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Provides the enterprise-controlled license verification public key.
 */
@RequiredArgsConstructor
@Component
public class CeLicensePublicKeyProvider {

    private static final String CONFIG_KEY = "carbon.license.public-key-pem";

    private final ConfigService configService;

    public String getPublicKeyPem() {
        String publicKeyPem = configService.getConfigValue(CONFIG_KEY);
        if (StringUtils.isBlank(publicKeyPem)) {
            return null;
        }
        return publicKeyPem;
    }
}
