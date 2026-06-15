package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local license runtime state business object.
 */
@Data
@AutoMapper(target = CeLicenseState.class, reverseConvertGenerate = false)
public class CeLicenseStateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "licenseId cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String licenseId;

    @NotBlank(message = "customerId cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String customerId;

    private Long packageId;

    private String packageName;

    @NotBlank(message = "installId cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String installId;

    @NotBlank(message = "keyId cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String keyId;

    @NotBlank(message = "algorithm cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String algorithm;

    @NotBlank(message = "schemaVersion cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String schemaVersion;

    @NotNull(message = "validFrom cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Date validFrom;

    @NotNull(message = "validTo cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Date validTo;

    private Date lastVerifiedTime;

    private Date maxObservedTime;

    private String featureCodes;

    private String payloadDigest;

    private String currentSummary;

    private String licenseStatus;
}
