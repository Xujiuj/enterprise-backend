package org.dromara.carbon.enterprise.client;

import org.dromara.carbon.enterprise.domain.sync.CeVendorFactorSyncResponse;
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
 * HTTP client for vendor open factor API.
 */
@Service
public class HttpCeVendorFactorOpenClient implements CeVendorFactorOpenClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorFactorSyncResponse syncFactors(String licenseId, String installId, String currentVersionCode) {
        if (StringUtils.isBlank(vendorOpenBaseUrl)) {
            throw new ServiceException("厂商开放接口地址未配置：请设置 carbon.enterprise.vendor-open-base-url");
        }
        String url = UriComponentsBuilder.fromUriString(vendorOpenBaseUrl.trim())
            .path("/open/factors")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .queryParamIfPresent("currentVersionCode", java.util.Optional.ofNullable(currentVersionCode))
            .toUriString();
        try {
            ResponseEntity<R<CeVendorFactorSyncResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                CeVendorOpenApiRequestSupport.bearerEntity(licenseId),
                new ParameterizedTypeReference<>() {
                }
            );
            R<CeVendorFactorSyncResponse> body = response.getBody();
            if (body == null || R.isError(body) || body.getData() == null) {
                throw new ServiceException(body == null ? "厂商因子同步失败：厂商未返回数据" : body.getMsg());
            }
            return body.getData();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("厂商因子同步失败：" + e.getMessage());
        }
    }
}
