package org.dromara.carbon.enterprise.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.license.CeLicenseGateBlockedResponse;
import org.dromara.carbon.enterprise.domain.license.CeLicenseGateResult;
import org.dromara.carbon.enterprise.service.CeLicenseInstallIdProvider;
import org.dromara.carbon.enterprise.service.ICeLicenseGateService;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Enforces the enterprise license gate for protected enterprise endpoints.
 */
@RequiredArgsConstructor
@Component
public class CeLicenseGateInterceptor implements HandlerInterceptor {

    private static final Map<String, String> FEATURE_BY_PATH_PREFIX = Map.of(
        "/enterprise/factor-sync/", "factor-sync",
        "/enterprise/report-template-sync/", "report-template-download",
        "/enterprise/report-template-file/", "report-template-download",
        "/enterprise/data-validation/", "report-gate"
    );

    private final ICeLicenseGateService licenseGateService;
    private final CeLicenseInstallIdProvider installIdProvider;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requiredFeatureCode = requiredFeatureCode(request);
        CeLicenseGateResult result = requiredFeatureCode == null
            ? licenseGateService.evaluateCurrent(installIdProvider.getExpectedInstallId(), new Date())
            : licenseGateService.evaluateCurrent(installIdProvider.getExpectedInstallId(), new Date(), requiredFeatureCode);
        if ("ALLOW".equals(result.getDecision())) {
            return true;
        }

        R<CeLicenseGateBlockedResponse> payload = new R<>();
        payload.setCode(HttpStatus.FORBIDDEN);
        payload.setMsg("enterprise license gate denied access");
        payload.setData(CeLicenseGateBlockedResponse.from(result));
        response.setStatus(HttpStatus.FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        writeJson(response, payload);
        return false;
    }

    private String requiredFeatureCode(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        for (Map.Entry<String, String> entry : FEATURE_BY_PATH_PREFIX.entrySet()) {
            if (requestPath.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, R<CeLicenseGateBlockedResponse> payload) {
        try {
            response.getWriter().write(objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize enterprise license gate response", e);
        }
    }
}
