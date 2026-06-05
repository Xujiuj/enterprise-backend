package org.dromara.carbon.enterprise.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Provides the enterprise-local install id used by protected license gate checks.
 */
@RequiredArgsConstructor
@Component
public class CeLicenseInstallIdProvider {

    static final String CONFIG_KEY = "carbon.license.install-id";

    private final Environment environment;

    public String getExpectedInstallId() {
        String installId = environment.getProperty(CONFIG_KEY);
        if (StringUtils.isBlank(installId)) {
            return null;
        }
        return installId;
    }
}
