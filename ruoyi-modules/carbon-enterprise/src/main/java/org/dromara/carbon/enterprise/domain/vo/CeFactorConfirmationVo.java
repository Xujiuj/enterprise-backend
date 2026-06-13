package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeFactorConfirmation;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local emission factor confirmation view object.
 */
@Data
@AutoMapper(target = CeFactorConfirmation.class)
public class CeFactorConfirmationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
