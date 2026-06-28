package org.dromara.carbon.enterprise.license.domain;

import lombok.Data;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;

/**
 * Result of enterprise license import or verification.
 */
@Data
public class CeLicenseImportResult {

    private final boolean valid;

    private final String status;

    private final String message;

    private final CeLicenseState licenseState;

    private final String syncMessage;

    public static CeLicenseImportResult valid(CeLicenseState licenseState) {
        return new CeLicenseImportResult(true, "VALID", "授权文件校验通过", licenseState, null);
    }

    public static CeLicenseImportResult invalid(String status, String message) {
        return new CeLicenseImportResult(false, status, message, null, null);
    }

    public CeLicenseImportResult withSyncMessage(String syncMessage) {
        return new CeLicenseImportResult(valid, status, message, licenseState, syncMessage);
    }
}
