package org.dromara.carbon.enterprise.client;

import org.dromara.carbon.enterprise.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionListResponse;
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

import java.util.Optional;

/**
 * HTTP client for vendor open dimension API.
 */
@Service
public class HttpCeVendorDimensionOpenClient implements CeVendorDimensionOpenClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorDimensionListResponse listDimensions(
        String licenseId,
        String installId,
        CeDimensionRecordBo query,
        Integer pageNum,
        Integer pageSize
    ) {
        if (StringUtils.isBlank(vendorOpenBaseUrl)) {
            throw new ServiceException("厂商开放接口地址未配置：请设置 carbon.enterprise.vendor-open-base-url");
        }
        if (query == null || StringUtils.isBlank(query.getDimensionCode())) {
            throw new ServiceException("vendor dimension code cannot be blank");
        }
        String url = UriComponentsBuilder.fromUriString(vendorOpenBaseUrl.trim())
            .path("/open/dimensions")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .queryParam("dimensionCode", query.getDimensionCode())
            .queryParamIfPresent("recordCode", optional(query.getRecordCode()))
            .queryParamIfPresent("recordName", optional(query.getRecordName()))
            .queryParamIfPresent("parentCode", optional(query.getParentCode()))
            .queryParamIfPresent("pageNum", Optional.ofNullable(pageNum))
            .queryParamIfPresent("pageSize", Optional.ofNullable(pageSize))
            .toUriString();
        ResponseEntity<R<CeVendorDimensionListResponse>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {
            }
        );
        R<CeVendorDimensionListResponse> body = response.getBody();
        if (body == null || R.isError(body) || body.getData() == null) {
            throw new ServiceException(body == null ? "vendor dimension list failed" : body.getMsg());
        }
        return body.getData();
    }

    private Optional<String> optional(String value) {
        return StringUtils.isBlank(value) ? Optional.empty() : Optional.of(value.trim());
    }
}
