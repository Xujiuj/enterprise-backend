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

    private Integer rowNo;

    @NotBlank(message = "factoryCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factoryCode;

    private String factoryName;

    private Integer activityYear;

    private Integer activityMonth;

    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    private String electricityType;

    private String electricityTypeDesc;

    @NotNull(message = "quantityKwh cannot be null", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal quantityKwh;

    private String certificateCode;

    private String issuingOrg;

    private Date purchaseDate;

    private Date expiryDate;

    private String powerGridRegion;

    private String offsetPowerSource;

    private String dataSource;

    private String sourceRemark;

    private String emissionSourceName;

    private String factorKey;

    private String proofStatus;

    private String remark;
}
