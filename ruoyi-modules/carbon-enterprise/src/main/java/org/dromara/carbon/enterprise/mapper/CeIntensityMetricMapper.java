package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeIntensityMetric;
import org.dromara.carbon.enterprise.domain.vo.CeIntensityMetricVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local carbon intensity metric mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeIntensityMetricMapper extends BaseMapperPlus<CeIntensityMetric, CeIntensityMetricVo> {
}
