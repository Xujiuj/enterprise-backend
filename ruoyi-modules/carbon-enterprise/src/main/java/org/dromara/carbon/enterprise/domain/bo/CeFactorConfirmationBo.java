package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeFactorConfirmation;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local emission factor confirmation business object.
 */
@Data
@AutoMapper(target = CeFactorConfirmation.class, reverseConvertGenerate = false)
public class CeFactorConfirmationBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "factorCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorCode;

    @NotBlank(message = "factorName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorName;

    @NotBlank(message = "factorVersionCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorVersionCode;

    @NotBlank(message = "factorUnit cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorUnit;

    @NotNull(message = "factorValue cannot be null", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal factorValue;

    private String confirmationStatus;

    private String confirmedBy;

    private Date confirmedTime;

    private String licenseId;

    private String remark;
}
