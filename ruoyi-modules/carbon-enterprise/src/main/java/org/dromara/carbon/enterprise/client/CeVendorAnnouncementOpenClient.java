package org.dromara.carbon.enterprise.client;

import org.dromara.carbon.enterprise.domain.sync.CeVendorAnnouncementListResponse;

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
