package org.dromara.carbon.enterprise.vendor.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validTo;

    private String licensePayload;

    private String signatureText;
}
