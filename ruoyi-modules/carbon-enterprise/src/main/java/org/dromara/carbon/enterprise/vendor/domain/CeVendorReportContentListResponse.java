package org.dromara.carbon.enterprise.vendor.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Vendor report content catalog list response.
 */
@Data
public class CeVendorReportContentListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private List<CeVendorReportContentRecord> contents = new ArrayList<>();
}
