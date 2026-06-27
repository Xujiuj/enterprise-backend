package org.dromara.carbon.enterprise.license.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.license.domain.vo.CeLicenseStateVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local license runtime state mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeLicenseStateMapper extends BaseMapperPlus<CeLicenseState, CeLicenseStateVo> {
}
