package org.dromara.carbon.enterprise.vendor.client;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Shared HTTP behavior for enterprise calls to vendor-managed open APIs.
 */
@Component
public class CeVendorOpenApiHttpSupport {

    public static final String VENDOR_BASE_URL_PROPERTY = "carbon.enterprise.vendor-open-base-url";
    private static final int CONNECT_TIMEOUT_MILLIS = (int) Duration.ofSeconds(5).toMillis();
    private static final int READ_TIMEOUT_MILLIS = (int) Duration.ofSeconds(30).toMillis();

    private final RestTemplate restTemplate;

    public CeVendorOpenApiHttpSupport() {
        this(createRestTemplate());
    }

    CeVendorOpenApiHttpSupport(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UriComponentsBuilder baseBuilder(String vendorOpenBaseUrl) {
        if (StringUtils.isBlank(vendorOpenBaseUrl)) {
            throw new ServiceException("厂商开放接口地址未配置：请设置 " + VENDOR_BASE_URL_PROPERTY);
        }
        return UriComponentsBuilder.fromUriString(vendorOpenBaseUrl.trim());
    }

    public <T> T exchangeForData(
        Supplier<String> urlSupplier,
        HttpMethod method,
        HttpEntity<?> entity,
        ParameterizedTypeReference<R<T>> responseType,
        String operationName
    ) {
        R<T> body = exchangeForBody(urlSupplier, method, entity, responseType, operationName);
        if (body == null) {
            throw new ServiceException(operationName + "失败：厂商未返回数据");
        }
        if (R.isError(body)) {
            throw new ServiceException(stableMessage(body.getMsg(), operationName + "失败"));
        }
        if (body.getData() == null) {
            throw new ServiceException(operationName + "失败");
        }
        return body.getData();
    }

    public <T> R<T> exchangeForBody(
        Supplier<String> urlSupplier,
        HttpMethod method,
        HttpEntity<?> entity,
        ParameterizedTypeReference<R<T>> responseType,
        String operationName
    ) {
        try {
            ResponseEntity<R<T>> response = restTemplate.exchange(urlSupplier.get(), method, entity, responseType);
            return response.getBody();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(operationName + "失败：" + stableMessage(e.getMessage(), "厂商接口调用异常"));
        }
    }

    public <T> ResponseEntity<T> exchange(
        Supplier<String> urlSupplier,
        HttpMethod method,
        HttpEntity<?> entity,
        Class<T> responseType,
        String operationName
    ) {
        try {
            return restTemplate.exchange(urlSupplier.get(), method, entity, responseType);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(operationName + "失败：" + stableMessage(e.getMessage(), "厂商接口调用异常"));
        }
    }

    RestTemplate restTemplate() {
        return restTemplate;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(READ_TIMEOUT_MILLIS);
        return new RestTemplate(factory);
    }

    private String stableMessage(String message, String fallback) {
        if (StringUtils.isBlank(message)) {
            return fallback;
        }
        if (message.startsWith("Unsupported vendor dimension code")) {
            return "厂商不支持该维度编码";
        }
        return switch (message) {
            case "report template file does not exist" -> "厂商报表模板文件不存在";
            case "license entitlement does not exist" -> "厂商授权不存在";
            case "license entitlement is revoked" -> "厂商授权已撤销";
            case "license entitlement is not currently valid" -> "厂商授权当前不在有效期内";
            case "dimensionCode cannot be blank" -> "维度编码不能为空";
            default -> message;
        };
    }
}
