package org.dromara.carbon.enterprise.factor.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactorVersionMap;
import org.dromara.carbon.enterprise.factor.domain.vo.CeElectricityFactorVersionMapVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 电力因子版本映射 Mapper接口.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeElectricityFactorVersionMapMapper extends BaseMapperPlus<CeElectricityFactorVersionMap, CeElectricityFactorVersionMapVo> {
}
