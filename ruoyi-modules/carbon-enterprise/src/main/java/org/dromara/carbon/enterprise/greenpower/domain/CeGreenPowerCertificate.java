package org.dromara.carbon.enterprise.greenpower.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local green electricity and certificate proof.
 */
@Data
@TableName("ce_green_power_certificate")
public class CeGreenPowerCertificate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

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
