package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeFactorCacheVersion;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local factor cache version business object.
 */
@Data
@AutoMapper(target = CeFactorCacheVersion.class, reverseConvertGenerate = false)
public class CeFactorCacheVersionBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "vendorVersionId cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String vendorVersionId;

    @NotBlank(message = "licenseId cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String licenseId;

    @NotBlank(message = "versionCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String versionCode;

    private Boolean frozenFlag;

    private Date syncedTime;
}
