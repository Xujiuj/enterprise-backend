package org.dromara.carbon.enterprise.domain;

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

    private String certificateCode;

    private String certificateType;

    private String energyPeriod;

    private BigDecimal energyAmount;

    private String energyUnit;

    private String issuingOrg;

    private Date purchaseDate;

    private Date expiryDate;

    private String offsetSourceCode;

    private String proofStatus;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
