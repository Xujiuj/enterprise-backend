package org.dromara.carbon.enterprise.vendor.client;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionListResponse;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * HTTP client for vendor open dimension API.
 */
@RequiredArgsConstructor
@Service
public class HttpCeVendorDimensionOpenClient implements CeVendorDimensionOpenClient {

    private final CeVendorOpenApiHttpSupport httpSupport;

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
        if (query == null || StringUtils.isBlank(query.getDimensionCode())) {
            throw new ServiceException("vendor dimension code cannot be blank");
        }
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
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
        return httpSupport.exchangeForData(
            () -> url,
            HttpMethod.GET,
            CeVendorOpenApiRequestSupport.bearerEntity(licenseId),
            new ParameterizedTypeReference<R<CeVendorDimensionListResponse>>() {
            },
            "厂商维度查询"
        );
    }

    private Optional<String> optional(String value) {
        return StringUtils.isBlank(value) ? Optional.empty() : Optional.of(value.trim());
    }
}
