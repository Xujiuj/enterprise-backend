package org.dromara.carbon.enterprise.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateDownloadResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateListResponse;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP client for vendor open report template API.
 */
@Service
public class HttpCeVendorReportTemplateOpenClient implements CeVendorReportTemplateOpenClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorReportTemplateListResponse listTemplates(String licenseId, String installId) {
        String url = baseBuilder()
            .path("/open/report-templates")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .toUriString();
        ResponseEntity<R<CeVendorReportTemplateListResponse>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {
            }
        );
        R<CeVendorReportTemplateListResponse> body = response.getBody();
        if (body == null || R.isError(body) || body.getData() == null) {
            throw new ServiceException(body == null ? "vendor report template list failed" : body.getMsg());
        }
        return body.getData();
    }

    @Override
    public CeVendorReportTemplateDownloadResponse downloadTemplate(Long templateId, String licenseId, String installId) {
        if (templateId == null) {
            throw new ServiceException("vendor report template id cannot be null");
        }
        String url = baseBuilder()
            .path("/open/report-templates/{id}/download")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .buildAndExpand(templateId)
            .toUriString();
        ResponseEntity<R<CeVendorReportTemplateDownloadResponse>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {
            }
        );
        R<CeVendorReportTemplateDownloadResponse> body = response.getBody();
        if (body == null || R.isError(body) || body.getData() == null) {
            throw new ServiceException(body == null ? "vendor report template download failed" : body.getMsg());
        }
        return body.getData();
    }

    @Override
    public byte[] downloadTemplateFile(String downloadToken) {
        if (StringUtils.isBlank(downloadToken)) {
            throw new ServiceException("vendor report template download token cannot be blank");
        }
        String url = baseBuilder()
            .path("/open/report-templates/download-tokens/{token}")
            .buildAndExpand(downloadToken.trim())
            .toUriString();
        ResponseEntity<byte[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            byte[].class
        );
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new ServiceException("vendor report template file download failed");
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ServiceException("vendor report template file download failed");
        }
        if (isJsonResponse(response.getHeaders().getContentType()) || looksLikeJson(body)) {
            throw new ServiceException(vendorDownloadErrorMessage(body));
        }
        return body;
    }

    private UriComponentsBuilder baseBuilder() {
        if (StringUtils.isBlank(vendorOpenBaseUrl)) {
            throw new ServiceException("厂商开放接口地址未配置：请设置 carbon.enterprise.vendor-open-base-url");
        }
        return UriComponentsBuilder.fromUriString(vendorOpenBaseUrl.trim());
    }

    private boolean isJsonResponse(MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        return MediaType.APPLICATION_JSON.includes(contentType)
            || contentType.getSubtype().toLowerCase().contains("json");
    }

    private boolean looksLikeJson(byte[] body) {
        for (byte item : body) {
            if (!Character.isWhitespace((char) item)) {
                return item == '{' || item == '[';
            }
        }
        return false;
    }

    private String vendorDownloadErrorMessage(byte[] body) {
        try {
            Map<?, ?> payload = OBJECT_MAPPER.readValue(body, Map.class);
            Object message = payload.get("msg");
            if (message != null && StringUtils.isNotBlank(message.toString())) {
                return message.toString();
            }
        } catch (IOException | RuntimeException ignored) {
            // Fall through to a stable generic error for non-R JSON bodies.
        }
        return "vendor report template file download failed";
    }
}
