package org.dromara.carbon.enterprise.vendor.client;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorLicenseCurrentRequest;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorLicenseCurrentResponse;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

/**
 * HTTP client for vendor license binding APIs.
 */
@RequiredArgsConstructor
@Service
public class HttpCeVendorLicenseOpenClient implements CeVendorLicenseOpenClient {

    private final CeVendorOpenApiHttpSupport httpSupport;

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorLicenseCurrentResponse currentLicense(String licenseId, String installId, String keyId,
                                                         String currentSummary) {
        CeVendorLicenseCurrentRequest request = new CeVendorLicenseCurrentRequest();
        request.setLicenseId(licenseId);
        request.setInstallId(installId);
        request.setKeyId(keyId);
        request.setCurrentSummary(currentSummary);

        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/licenses/current")
            .toUriString();
        return httpSupport.exchangeForData(
            () -> url,
            HttpMethod.POST,
            CeVendorOpenApiRequestSupport.bearerEntity(request, licenseId),
            new ParameterizedTypeReference<R<CeVendorLicenseCurrentResponse>>() {
            },
            "厂商授权绑定确认"
        );
    }
}
