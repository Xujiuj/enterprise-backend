package org.dromara.carbon.enterprise.activity.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local activity data mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeActivityDataMapper extends BaseMapperPlus<CeActivityData, CeActivityDataVo> {
}
