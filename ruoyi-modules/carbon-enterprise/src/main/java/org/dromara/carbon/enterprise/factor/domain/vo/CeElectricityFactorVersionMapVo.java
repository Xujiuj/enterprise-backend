package org.dromara.carbon.enterprise.factor.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactorVersionMap;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 电力因子版本映射视图对象.
 */
@Data
@AutoMapper(target = CeElectricityFactorVersionMap.class)
public class CeElectricityFactorVersionMapVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String factorVersion;

    private Integer effectiveYear;

    private Integer sortOrder;

    private String status;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
