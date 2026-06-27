package org.dromara.carbon.enterprise.vendor.client;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorAnnouncementListResponse;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * HTTP client for vendor open announcement API.
 */
@RequiredArgsConstructor
@Service
public class HttpCeVendorAnnouncementOpenClient implements CeVendorAnnouncementOpenClient {

    private final CeVendorOpenApiHttpSupport httpSupport;

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorAnnouncementListResponse listAnnouncements(String licenseId, String installId, Integer limit) {
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/announcements")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .queryParamIfPresent("limit", Optional.ofNullable(limit))
            .toUriString();
        return httpSupport.exchangeForData(
            () -> url,
            HttpMethod.GET,
            CeVendorOpenApiRequestSupport.bearerEntity(licenseId),
            new ParameterizedTypeReference<R<CeVendorAnnouncementListResponse>>() {
            },
            "厂商公告查询"
        );
    }
}
