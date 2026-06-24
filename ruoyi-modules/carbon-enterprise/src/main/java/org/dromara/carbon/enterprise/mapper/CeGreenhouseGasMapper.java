package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeGreenhouseGas;
import org.dromara.carbon.enterprise.domain.vo.CeGreenhouseGasVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 温室气体 Mapper接口.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeGreenhouseGasMapper extends BaseMapperPlus<CeGreenhouseGas, CeGreenhouseGasVo> {
}
