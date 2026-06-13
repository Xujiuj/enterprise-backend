package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local emission factor confirmation.
 */
@Data
@TableName("ce_factor_confirmation")
public class CeFactorConfirmation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String factorCode;

    private String factorName;

    private String factorVersionCode;

    private String factorUnit;

    private BigDecimal factorValue;

    private String confirmationStatus;

    private String confirmedBy;

    private Date confirmedTime;

    private String licenseId;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
