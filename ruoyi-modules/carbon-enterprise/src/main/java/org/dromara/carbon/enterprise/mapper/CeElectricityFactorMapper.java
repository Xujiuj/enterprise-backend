package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeElectricityFactor;
import org.dromara.carbon.enterprise.domain.vo.CeElectricityFactorVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 电力排放因子 Mapper接口.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeElectricityFactorMapper extends BaseMapperPlus<CeElectricityFactor, CeElectricityFactorVo> {
}
