package org.dromara.carbon.enterprise.factor.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactorScope;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 电力因子范围视图对象.
 */
@Data
@AutoMapper(target = CeElectricityFactorScope.class)
public class CeElectricityFactorScopeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String scopeKey;

    private String scopeName;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
