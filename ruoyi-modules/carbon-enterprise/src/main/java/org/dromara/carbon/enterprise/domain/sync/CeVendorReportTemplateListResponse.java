package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Vendor report template list response.
 */
@Data
public class CeVendorReportTemplateListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private List<CeVendorReportTemplateRecord> templates = new ArrayList<>();
}
