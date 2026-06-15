package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeLicenseState;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local license runtime state view object.
 */
@Data
@AutoMapper(target = CeLicenseState.class)
public class CeLicenseStateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String licenseId;

    private String customerId;

    private Long packageId;

    private String packageName;

    private String installId;

    private String keyId;

    private String algorithm;

    private String schemaVersion;

    private Date validFrom;

    private Date validTo;

    private Date lastVerifiedTime;

    private Date maxObservedTime;

    private String featureCodes;

    private String payloadDigest;

    private String currentSummary;

    private String licenseStatus;
}
