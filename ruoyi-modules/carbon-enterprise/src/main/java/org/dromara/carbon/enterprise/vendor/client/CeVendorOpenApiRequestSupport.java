package org.dromara.carbon.enterprise.vendor.client;

import org.dromara.common.core.utils.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

/**
 * Shared request construction for vendor open APIs.
 */
public final class CeVendorOpenApiRequestSupport {

    private CeVendorOpenApiRequestSupport() {
    }

    public static HttpEntity<Void> bearerEntity(String licenseId) {
        return new HttpEntity<>(bearerHeaders(licenseId));
    }

    public static <T> HttpEntity<T> bearerEntity(T body, String licenseId) {
        return new HttpEntity<>(body, bearerHeaders(licenseId));
    }

    public static HttpHeaders bearerHeaders(String licenseId) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotBlank(licenseId)) {
            headers.setBearerAuth(licenseId.trim());
        }
        return headers;
    }
}
