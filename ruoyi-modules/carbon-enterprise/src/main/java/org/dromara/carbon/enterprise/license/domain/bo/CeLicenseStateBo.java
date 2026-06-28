package org.dromara.carbon.enterprise.license.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
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

    @NotNull(message = "授权状态ID不能为空", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "授权编号不能为空", groups = { AddGroup.class, EditGroup.class })
    private String licenseId;

    @NotBlank(message = "客户标识不能为空", groups = { AddGroup.class, EditGroup.class })
    private String customerId;

    private Long packageId;

    private String packageName;

    @NotBlank(message = "部署指纹不能为空", groups = { AddGroup.class, EditGroup.class })
    private String installId;

    @NotBlank(message = "签名密钥不能为空", groups = { AddGroup.class, EditGroup.class })
    private String keyId;

    @NotBlank(message = "签名算法不能为空", groups = { AddGroup.class, EditGroup.class })
    private String algorithm;

    @NotBlank(message = "授权文件版本不能为空", groups = { AddGroup.class, EditGroup.class })
    private String schemaVersion;

    @NotNull(message = "有效期开始时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private Date validFrom;

    @NotNull(message = "有效期结束时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private Date validTo;

    private Date lastVerifiedTime;

    private Date maxObservedTime;

    private String featureCodes;

    private String payloadDigest;

    private String currentSummary;

    private String licenseStatus;
}
