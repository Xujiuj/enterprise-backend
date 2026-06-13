package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local report template sync result.
 */
@Data
public class CeReportTemplateSyncResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private int templateCount;

    private Date syncedTime;
}
