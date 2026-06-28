package org.dromara.carbon.enterprise.vendor.client;

import org.dromara.carbon.enterprise.vendor.domain.CeVendorReportContentListResponse;

/**
 * Client for vendor open report content catalog API.
 */
public interface CeVendorReportContentOpenClient {

    /**
     * List vendor configured report content rows for the current enterprise license.
     *
     * @param licenseId license id
     * @param installId install id
     * @return vendor report content catalog
     */
    CeVendorReportContentListResponse listContents(String licenseId, String installId);
}
