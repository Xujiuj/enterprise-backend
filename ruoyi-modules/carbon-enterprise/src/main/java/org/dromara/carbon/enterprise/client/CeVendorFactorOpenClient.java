package org.dromara.carbon.enterprise.client;

import org.dromara.carbon.enterprise.domain.sync.CeVendorFactorSyncResponse;

/**
 * Client for vendor open factor API.
 */
public interface CeVendorFactorOpenClient {

    /**
     * Fetch license-scoped factor records from vendor backend.
     *
     * @param licenseId enterprise license id
     * @param installId enterprise install id
     * @param currentVersionCode current local cache version code
     * @return vendor factor sync response
     */
    CeVendorFactorSyncResponse syncFactors(String licenseId, String installId, String currentVersionCode);
}
