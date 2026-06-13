package org.dromara.carbon.enterprise.client;

import org.dromara.carbon.enterprise.domain.sync.CeVendorAnnouncementListResponse;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP client for vendor open announcement API.
 */
@Service
public class HttpCeVendorAnnouncementOpenClient implements CeVendorAnnouncementOpenClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorAnnouncementListResponse listAnnouncements(String licenseId, String installId, Integer limit) {
        if (StringUtils.isBlank(vendorOpenBaseUrl)) {
            throw new ServiceException("厂商开放接口地址未配置：请设置 carbon.enterprise.vendor-open-base-url");
        }
        String url = UriComponentsBuilder.fromUriString(vendorOpenBaseUrl.trim())
            .path("/open/announcements")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .queryParamIfPresent("limit", java.util.Optional.ofNullable(limit))
            .toUriString();
        ResponseEntity<R<CeVendorAnnouncementListResponse>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {
            }
        );
        R<CeVendorAnnouncementListResponse> body = response.getBody();
        if (body == null || R.isError(body) || body.getData() == null) {
            throw new ServiceException(body == null ? "vendor announcement list failed" : body.getMsg());
        }
        return body.getData();
    }
}
