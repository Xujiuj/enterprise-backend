package org.dromara.carbon.enterprise.vendor.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Vendor response for the current license binding check.
 */
@Data
public class CeVendorLicenseCurrentResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private Long customerId;

    private String status;

    private Long packageId;

    private String packageName;

    private String edition;

    private String featureCodes;

    private String keyId;

    private String algorithm;

    private String schemaVersion;

    private Date validFrom;

    private Date validTo;

    private String licensePayload;

    private String signatureText;
}
