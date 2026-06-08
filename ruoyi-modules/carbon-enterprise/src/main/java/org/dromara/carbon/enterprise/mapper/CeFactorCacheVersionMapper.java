package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.domain.CeFactorCacheVersion;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheVersionVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local factor cache version mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeFactorCacheVersionMapper extends BaseMapperPlus<CeFactorCacheVersion, CeFactorCacheVersionVo> {
}
