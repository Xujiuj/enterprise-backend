package org.dromara.carbon.enterprise.license.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.vendor.client.CeVendorOpenApiHttpSupport;
import org.dromara.carbon.enterprise.vendor.client.CeVendorOpenApiRequestSupport;
import org.dromara.carbon.enterprise.license.domain.bo.CeOnlinePurchaseCreateBo;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise-side proxy for vendor online purchase open API.
 */
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/enterprise/online-purchase")
public class CeOnlinePurchaseController extends BaseController {

    private final CeVendorOpenApiHttpSupport httpSupport;

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @PostMapping
    public R<Object> create(@Valid @RequestBody CeOnlinePurchaseCreateBo bo) {
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/purchases")
            .toUriString();
        return httpSupport.exchangeForBody(
            () -> url,
            HttpMethod.POST,
            CeVendorOpenApiRequestSupport.bearerEntity(bo, bo.getLicenseId()),
            new ParameterizedTypeReference<R<Object>>() {
            },
            "厂商在线购买"
        );
    }
}
