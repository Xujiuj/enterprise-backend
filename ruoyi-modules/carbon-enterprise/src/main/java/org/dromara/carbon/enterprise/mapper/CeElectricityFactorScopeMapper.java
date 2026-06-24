package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeElectricityFactorScope;
import org.dromara.carbon.enterprise.domain.vo.CeElectricityFactorScopeVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 电力因子范围 Mapper接口.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeElectricityFactorScopeMapper extends BaseMapperPlus<CeElectricityFactorScope, CeElectricityFactorScopeVo> {
}
