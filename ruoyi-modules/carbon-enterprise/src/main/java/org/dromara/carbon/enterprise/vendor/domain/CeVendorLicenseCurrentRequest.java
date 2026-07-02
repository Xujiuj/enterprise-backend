package org.dromara.carbon.enterprise.vendor.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request used to confirm a license install binding with the vendor backend.
 */
@Data
public class CeVendorLicenseCurrentRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private String installId;

    private String keyId;

    private String currentSummary;
}
