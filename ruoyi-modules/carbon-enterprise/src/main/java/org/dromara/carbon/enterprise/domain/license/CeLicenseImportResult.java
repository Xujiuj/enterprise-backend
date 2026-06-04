package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeLicenseState;

/**
 * Result of enterprise license import or verification.
 */
@Data
public class CeLicenseImportResult {

    private final boolean valid;

    private final String status;

    private final String message;

    private final CeLicenseState licenseState;

    public static CeLicenseImportResult valid(CeLicenseState licenseState) {
        return new CeLicenseImportResult(true, "VALID", "license is valid", licenseState);
    }

    public static CeLicenseImportResult invalid(String status, String message) {
        return new CeLicenseImportResult(false, status, message, null);
    }
}
