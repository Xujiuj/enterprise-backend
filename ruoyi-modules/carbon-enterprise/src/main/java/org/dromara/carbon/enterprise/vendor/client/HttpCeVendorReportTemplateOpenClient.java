package org.dromara.carbon.enterprise.vendor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.client.CeVendorReportTemplateOpenClient;
import org.dromara.carbon.enterprise.report.domain.CeVendorReportTemplateDownloadResponse;
import org.dromara.carbon.enterprise.report.domain.CeVendorReportTemplateListResponse;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP client for vendor open report template API.
 */
@RequiredArgsConstructor
@Service
public class HttpCeVendorReportTemplateOpenClient implements CeVendorReportTemplateOpenClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CeVendorOpenApiHttpSupport httpSupport;

    @Value("${carbon.enterprise.vendor-open-base-url:}")
    private String vendorOpenBaseUrl;

    @Override
    public CeVendorReportTemplateListResponse listTemplates(String licenseId, String installId) {
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/report-templates")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .toUriString();
        return httpSupport.exchangeForData(
            () -> url,
            HttpMethod.GET,
            CeVendorOpenApiRequestSupport.bearerEntity(licenseId),
            new ParameterizedTypeReference<R<CeVendorReportTemplateListResponse>>() {
            },
            "厂商模板列表查询"
        );
    }

    @Override
    public CeVendorReportTemplateDownloadResponse downloadTemplate(Long templateId, String licenseId, String installId) {
        if (templateId == null) {
            throw new ServiceException("厂商报表模板ID不能为空");
        }
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/report-templates/{id}/download")
            .queryParam("licenseId", licenseId)
            .queryParam("installId", installId)
            .buildAndExpand(templateId)
            .toUriString();
        return httpSupport.exchangeForData(
            () -> url,
            HttpMethod.GET,
            CeVendorOpenApiRequestSupport.bearerEntity(licenseId),
            new ParameterizedTypeReference<R<CeVendorReportTemplateDownloadResponse>>() {
            },
            "厂商模板下载授权"
        );
    }

    @Override
    public byte[] downloadTemplateFile(String downloadToken) {
        if (StringUtils.isBlank(downloadToken)) {
            throw new ServiceException("厂商报表模板下载凭证不能为空");
        }
        String url = httpSupport.baseBuilder(vendorOpenBaseUrl)
            .path("/open/report-templates/download-tokens/{token}")
            .buildAndExpand(downloadToken.trim())
            .toUriString();
        ResponseEntity<byte[]> response = httpSupport.exchange(
            () -> url,
            HttpMethod.GET,
            null,
            byte[].class,
            "厂商模板文件下载"
        );
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new ServiceException("厂商报表模板文件下载失败");
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ServiceException("厂商报表模板文件下载失败");
        }
        if (isJsonResponse(response.getHeaders().getContentType()) || looksLikeJson(body)) {
            throw new ServiceException(vendorDownloadErrorMessage(body));
        }
        return body;
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
                return translateVendorMessage(message.toString());
            }
        } catch (IOException | RuntimeException ignored) {
            // Fall through to a stable generic error for non-R JSON bodies.
        }
        return "厂商报表模板文件下载失败";
    }

    private String translateVendorMessage(String message) {
        return switch (message) {
            case "report template file does not exist" -> "厂商报表模板文件不存在";
            case "vendor report template file download failed" -> "厂商报表模板文件下载失败";
            case "vendor report template download token cannot be blank" -> "厂商报表模板下载凭证不能为空";
            default -> message;
        };
    }
}
