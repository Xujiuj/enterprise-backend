package org.dromara.carbon.enterprise.license.domain;

import lombok.Data;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;

import java.util.Date;

/**
 * License state fields exposed by the enterprise license import API.
 */
@Data
public class CeLicenseImportStateResponse {

    private final String licenseId;

    private final String customerId;

    private final Long packageId;

    private final String packageName;

    private final String installId;

    private final String keyId;

    private final String algorithm;

    private final String schemaVersion;

    private final Date validFrom;

    private final Date validTo;

    private final Date lastVerifiedTime;

    private final Date maxObservedTime;

    private final String featureCodes;

    private final String currentSummary;

    private final String licenseStatus;

    public static CeLicenseImportStateResponse from(CeLicenseState state) {
        if (state == null) {
            return null;
        }
        return new CeLicenseImportStateResponse(
            state.getLicenseId(),
            state.getCustomerId(),
            state.getPackageId(),
            state.getPackageName(),
            state.getInstallId(),
            state.getKeyId(),
            state.getAlgorithm(),
            state.getSchemaVersion(),
            state.getValidFrom(),
            state.getValidTo(),
            state.getLastVerifiedTime(),
            state.getMaxObservedTime(),
            state.getFeatureCodes(),
            state.getCurrentSummary(),
            state.getLicenseStatus()
        );
    }
}
