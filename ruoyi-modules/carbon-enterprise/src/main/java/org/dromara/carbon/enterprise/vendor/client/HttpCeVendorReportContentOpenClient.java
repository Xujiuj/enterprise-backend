package org.dromara.carbon.enterprise.vendor.client;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorReportContentListResponse;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

/**
 * HTTP client for vendor open report content catalog API.
 */
@RequiredArgsConstructor
@Service
public class HttpCeVendorReportContentOpenClient implements CeVendorReportContentOpenClient {

    private final CeVendorOpenApiHttpSupport httpSupport;

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorReportContentListResponse listContents(String licenseId, String installId) {
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/report-contents")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .toUriString();
        return httpSupport.exchangeForData(
            () -> url,
            HttpMethod.GET,
            CeVendorOpenApiRequestSupport.bearerEntity(licenseId),
            new ParameterizedTypeReference<R<CeVendorReportContentListResponse>>() {
            },
            "厂商报表内容目录查询"
        );
    }
}
