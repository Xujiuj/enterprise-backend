package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeLicenseState;

import java.util.Date;

/**
 * License state fields exposed by the enterprise license import API.
 */
@Data
public class CeLicenseImportStateResponse {

    private final String licenseId;

    private final String customerId;

    private final String installId;

    private final String keyId;

    private final String algorithm;

    private final String schemaVersion;

    private final Date validFrom;

    private final Date validTo;

    private final String licenseStatus;

    public static CeLicenseImportStateResponse from(CeLicenseState state) {
        if (state == null) {
            return null;
        }
        return new CeLicenseImportStateResponse(
            state.getLicenseId(),
            state.getCustomerId(),
            state.getInstallId(),
            state.getKeyId(),
            state.getAlgorithm(),
            state.getSchemaVersion(),
            state.getValidFrom(),
            state.getValidTo(),
            state.getLicenseStatus()
        );
    }
}
