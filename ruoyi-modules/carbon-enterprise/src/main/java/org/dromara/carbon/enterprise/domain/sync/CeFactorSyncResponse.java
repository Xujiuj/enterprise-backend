package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise factor sync operation result.
 */
@Data
public class CeFactorSyncResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private String vendorVersionId;

    private String versionCode;

    private Boolean frozenFlag;

    private boolean changed;

    private int recordCount;

    private Date syncedTime;
}
