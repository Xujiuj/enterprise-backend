package org.dromara.carbon.enterprise.vendor.client;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorFactorSyncResponse;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * HTTP client for vendor open factor API.
 */
@RequiredArgsConstructor
@Service
public class HttpCeVendorFactorOpenClient implements CeVendorFactorOpenClient {

    private final CeVendorOpenApiHttpSupport httpSupport;

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorFactorSyncResponse syncFactors(String licenseId, String installId, String currentVersionCode) {
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/factors")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .queryParamIfPresent("currentVersionCode", Optional.ofNullable(currentVersionCode))
            .toUriString();
        return httpSupport.exchangeForData(
            () -> url,
            HttpMethod.GET,
            CeVendorOpenApiRequestSupport.bearerEntity(licenseId),
            new ParameterizedTypeReference<R<CeVendorFactorSyncResponse>>() {
            },
            "厂商因子同步"
        );
    }
}
