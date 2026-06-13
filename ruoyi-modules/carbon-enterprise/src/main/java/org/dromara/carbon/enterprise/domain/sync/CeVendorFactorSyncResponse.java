package org.dromara.carbon.enterprise.domain.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Vendor open factor sync response consumed by enterprise backend.
 */
@Data
public class CeVendorFactorSyncResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private String vendorVersionId;

    private String versionCode;

    private String versionName;

    private String publishStatus;

    private Boolean frozenFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishedTime;

    private boolean changed;

    private List<CeVendorFactorRecord> records = new ArrayList<>();
}
