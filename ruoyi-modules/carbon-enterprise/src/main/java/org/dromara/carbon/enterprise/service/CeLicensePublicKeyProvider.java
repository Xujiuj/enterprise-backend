package org.dromara.carbon.enterprise.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Provides the enterprise-controlled license verification public key.
 */
@RequiredArgsConstructor
@Component
public class CeLicensePublicKeyProvider {

    private static final String CONFIG_KEY = "carbon.license.public-key-pem";

    private final Environment environment;

    public String getPublicKeyPem() {
        String publicKeyPem = environment.getProperty(CONFIG_KEY);
        if (StringUtils.isBlank(publicKeyPem)) {
            return null;
        }
        return publicKeyPem;
    }
}
