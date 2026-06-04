package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;

/**
 * Enterprise-side license import response.
 */
@Data
public class CeLicenseImportResponse {

    private final boolean valid;

    private final String status;

    private final String message;

    private final CeLicenseImportStateResponse licenseState;

    public static CeLicenseImportResponse from(CeLicenseImportResult result) {
        String status = result.getStatus();
        return new CeLicenseImportResponse(result.isValid(), status, messageForStatus(status),
            CeLicenseImportStateResponse.from(result.getLicenseState()));
    }

    public static CeLicenseImportResponse publicKeyUnavailable() {
        return new CeLicenseImportResponse(false, "PUBLIC_KEY_UNAVAILABLE", messageForStatus("PUBLIC_KEY_UNAVAILABLE"),
            null);
    }

    private static String messageForStatus(String status) {
        return switch (status) {
            case "VALID" -> "license is valid";
            case "MALFORMED_LICENSE" -> "license content is malformed";
            case "UNSUPPORTED_SCHEMA" -> "license schema is unsupported";
            case "UNSUPPORTED_ALGORITHM" -> "license algorithm is unsupported";
            case "SIGNATURE_INVALID" -> "license signature is invalid";
            case "KEY_ID_MISMATCH" -> "license key id is inconsistent";
            case "NOT_YET_VALID" -> "license is not valid yet";
            case "EXPIRED" -> "license has expired";
            case "INSTALL_ID_MISMATCH" -> "license installId does not match local installId";
            case "CLOCK_ROLLBACK" -> "system time is earlier than the last observed license verification time";
            case "PUBLIC_KEY_UNAVAILABLE" -> "enterprise license public key is unavailable";
            default -> "license import failed";
        };
    }
}
