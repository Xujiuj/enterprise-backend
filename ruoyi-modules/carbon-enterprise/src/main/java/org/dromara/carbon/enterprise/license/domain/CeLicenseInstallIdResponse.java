package org.dromara.carbon.enterprise.license.domain;

/**
 * Enterprise-side install id response.
 */
public class CeLicenseInstallIdResponse {

    private final String expectedInstallId;

    public CeLicenseInstallIdResponse(String expectedInstallId) {
        this.expectedInstallId = expectedInstallId;
    }

    public String getExpectedInstallId() {
        return expectedInstallId;
    }
}
