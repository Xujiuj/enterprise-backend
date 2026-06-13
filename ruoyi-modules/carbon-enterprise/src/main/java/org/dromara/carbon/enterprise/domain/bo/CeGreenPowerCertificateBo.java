package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local green electricity and certificate proof business object.
 */
@Data
@AutoMapper(target = CeGreenPowerCertificate.class, reverseConvertGenerate = false)
public class CeGreenPowerCertificateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "certificateCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String certificateCode;

    @NotBlank(message = "certificateType cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String certificateType;

    @NotBlank(message = "energyPeriod cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String energyPeriod;

    @NotNull(message = "energyAmount cannot be null", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal energyAmount;

    private String energyUnit;

    private String issuingOrg;

    private Date purchaseDate;

    private Date expiryDate;

    private String offsetSourceCode;

    private String proofStatus;

    private String remark;
}
