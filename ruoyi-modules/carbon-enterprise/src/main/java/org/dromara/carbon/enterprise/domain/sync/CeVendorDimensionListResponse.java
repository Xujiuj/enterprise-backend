package org.dromara.carbon.enterprise.domain.sync;

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

    private List<CeVendorDimensionRecord> records = new ArrayList<>();
}
