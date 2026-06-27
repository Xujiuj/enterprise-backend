package org.dromara.carbon.enterprise.intensity.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.intensity.domain.vo.CeIntensityDenominatorFactVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local intensity denominator fact mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeIntensityDenominatorFactMapper extends BaseMapperPlus<CeIntensityDenominatorFact, CeIntensityDenominatorFactVo> {
}
