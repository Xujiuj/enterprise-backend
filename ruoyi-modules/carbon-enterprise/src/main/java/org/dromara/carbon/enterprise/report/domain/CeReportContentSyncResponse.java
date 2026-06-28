package org.dromara.carbon.enterprise.report.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise report content catalog sync result.
 */
@Data
public class CeReportContentSyncResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private int contentCount;

    private Date syncedTime;
}
