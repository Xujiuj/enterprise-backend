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

/**
 * Enforces the enterprise license gate for protected enterprise endpoints.
 */
@RequiredArgsConstructor
@Component
public class CeLicenseGateInterceptor implements HandlerInterceptor {

    private final ICeLicenseGateService licenseGateService;
    private final CeLicenseInstallIdProvider installIdProvider;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CeLicenseGateResult result = licenseGateService.evaluateCurrent(
            installIdProvider.getExpectedInstallId(),
            new Date()
        );
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

    private void writeJson(HttpServletResponse response, R<CeLicenseGateBlockedResponse> payload) {
        try {
            response.getWriter().write(objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize enterprise license gate response", e);
        }
    }
}
