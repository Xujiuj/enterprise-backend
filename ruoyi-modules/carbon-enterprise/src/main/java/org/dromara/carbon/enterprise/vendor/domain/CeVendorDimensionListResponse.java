package org.dromara.carbon.enterprise.vendor.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Vendor open dimension list response consumed by enterprise backend.
 */
@Data
public class CeVendorDimensionListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private String dimensionCode;

    private long total;

    /** Vendor-controlled synchronization boundary for 103 versions. */
    private String publicationId;

    private String publishMode;

    private List<String> publishedVersions = new ArrayList<>();

    private List<CeVendorDimensionRecord> records = new ArrayList<>();
}
