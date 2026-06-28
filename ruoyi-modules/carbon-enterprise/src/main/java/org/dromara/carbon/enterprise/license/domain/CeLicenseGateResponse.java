package org.dromara.carbon.enterprise.license.domain;

import lombok.Data;
import org.dromara.carbon.enterprise.license.domain.vo.CeLicenseStateVo;

/**
 * Read-only response for enterprise license gate checks.
 */
@Data
public class CeLicenseGateResponse {

    private final String decision;

    private final String reason;

    private final String message;

    private final CeLicenseStateVo licenseState;

    /**
     * Compatibility alias for clients that consume a boolean gate decision.
     */
    public boolean isAllowed() {
        return "ALLOW".equals(decision);
    }

    /**
     * Compatibility alias for clients that consume the effective gate status.
     */
    public String getStatus() {
        return reason;
    }

    public static CeLicenseGateResponse from(CeLicenseGateResult result) {
        return new CeLicenseGateResponse(
            result.getDecision(),
            result.getReason(),
            messageForReason(result.getReason()),
            result.getLicenseState()
        );
    }

    private static String messageForReason(String reason) {
        return switch (reason) {
            case "VALID" -> "当前授权有效";
            case "EXPIRED" -> "授权已过期";
            case "CLOCK_ROLLBACK" -> "系统时间早于最近授权校验时间";
            case "INSTALL_ID_MISMATCH" -> "授权文件的部署指纹与本机不匹配";
            case "FEATURE_NOT_ENABLED" -> "当前授权未包含所需功能";
            case "NO_VALID_LICENSE" -> "当前没有可用的企业授权";
            default -> "企业授权网关拒绝访问";
        };
    }
}
