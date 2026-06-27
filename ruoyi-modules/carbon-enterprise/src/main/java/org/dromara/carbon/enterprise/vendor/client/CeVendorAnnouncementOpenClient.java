package org.dromara.carbon.enterprise.vendor.client;

import org.dromara.carbon.enterprise.vendor.domain.CeVendorAnnouncementListResponse;

/**
 * Client for vendor open announcement API.
 */
public interface CeVendorAnnouncementOpenClient {

    /**
     * List vendor announcements for the current enterprise license.
     *
     * @param licenseId license id
     * @param installId install id
     * @param limit max announcement count
     * @return vendor announcement list
     */
    CeVendorAnnouncementListResponse listAnnouncements(String licenseId, String installId, Integer limit);
}
