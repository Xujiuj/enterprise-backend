package org.dromara.carbon.enterprise.vendor.client;

import org.dromara.carbon.enterprise.vendor.domain.CeVendorLicenseCurrentResponse;

/**
 * Client for vendor-managed license open APIs.
 */
public interface CeVendorLicenseOpenClient {

    CeVendorLicenseCurrentResponse currentLicense(String licenseId, String installId, String keyId, String currentSummary);
}
