package org.dromara.carbon.enterprise.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.annotation.SaIgnore;
import org.dromara.carbon.enterprise.client.CeVendorOpenApiRequestSupport;
import org.dromara.carbon.enterprise.domain.bo.CeOnlinePurchaseCreateBo;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.core.BaseController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Enterprise-side proxy for vendor online purchase open API.
 */
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/enterprise/online-purchase")
public class CeOnlinePurchaseController extends BaseController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @PostMapping
    public R<Object> create(@Valid @RequestBody CeOnlinePurchaseCreateBo bo) {
        if (StringUtils.isBlank(vendorOpenBaseUrl)) {
            throw new ServiceException("厂商开放接口地址未配置：请设置 carbon.enterprise.vendor-open-base-url");
        }
        String url = UriComponentsBuilder.fromUriString(vendorOpenBaseUrl.trim())
            .path("/open/purchases")
            .toUriString();
        HttpEntity<CeOnlinePurchaseCreateBo> entity = CeVendorOpenApiRequestSupport.bearerEntity(bo, bo.getLicenseId());
        ResponseEntity<R<Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new org.springframework.core.ParameterizedTypeReference<>() {
        });
        R<Object> body = response.getBody();
        if (body == null) {
            return R.fail("vendor online purchase failed");
        }
        return body;
    }
}
