package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;

/**
 * Read-only response for enterprise license gate checks.
 */
@Data
public class CeLicenseGateResponse {

    private final String decision;

    private final String reason;

    private final String message;

    private final CeLicenseStateVo licenseState;

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
            case "VALID" -> "license is valid";
            case "EXPIRED" -> "license has expired";
            case "CLOCK_ROLLBACK" -> "system time is earlier than the last observed license verification time";
            case "INSTALL_ID_MISMATCH" -> "license installId does not match local installId";
            case "NO_VALID_LICENSE" -> "no valid enterprise license is currently available";
            default -> "enterprise license gate denied access";
        };
    }
}
