package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local green electricity and certificate proof view object.
 */
@Data
@AutoMapper(target = CeGreenPowerCertificate.class)
public class CeGreenPowerCertificateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Integer rowNo;

    private String factoryCode;

    private String factoryName;

    private Integer activityYear;

    private Integer activityMonth;

    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    private String electricityType;

    private String electricityTypeDesc;

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

    private Date createTime;

    private Date updateTime;

    private String remark;
}
