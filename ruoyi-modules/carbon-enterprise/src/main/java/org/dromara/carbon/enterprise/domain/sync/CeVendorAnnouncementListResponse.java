package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Vendor open announcement list response.
 */
@Data
public class CeVendorAnnouncementListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private List<CeVendorAnnouncementRecord> announcements = new ArrayList<>();
}
