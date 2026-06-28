package org.dromara.carbon.enterprise.factor.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 电力排放因子视图对象.
 */
@Data
@AutoMapper(target = CeElectricityFactor.class)
public class CeElectricityFactorVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String versionProvinceCode;

    private String factorVersion;

    private String divisionCode;

    private String divisionName;

    private String regionName;

    private BigDecimal provinceFactor;

    private BigDecimal regionFactor;

    private BigDecimal nationalFactor;

    private BigDecimal nonFossilExcludedFactor;

    private BigDecimal nationalFossilPowerFactor;

    private Integer sortOrder;

    private String status;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
