package org.dromara.carbon.enterprise.vendor.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Vendor report content catalog record.
 */
@Data
public class CeVendorReportContentRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long contentId;

    private Integer directoryNo;

    private String directoryName;

    private Integer subdirectoryNo;

    private String subdirectoryName;

    private String chartNames;

    private Integer displayOrder;

    private String remark;
}
