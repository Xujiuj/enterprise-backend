package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;

/**
 * Stable error payload returned when a protected enterprise operation is blocked by the license gate.
 */
@Data
public class CeLicenseGateBlockedResponse {

    private final String errorCode;

    private final CeLicenseGateBlockedDetails gate;

    public static CeLicenseGateBlockedResponse from(CeLicenseGateResult result) {
        return new CeLicenseGateBlockedResponse(
            "ENTERPRISE_LICENSE_GATE_DENIED",
            new CeLicenseGateBlockedDetails(
                result.getDecision(),
                result.getReason(),
                CeLicenseGateResponse.from(result).getMessage()
            )
        );
    }

    @Data
    public static class CeLicenseGateBlockedDetails {

        private final String decision;

        private final String reason;

        private final String message;
    }
}
