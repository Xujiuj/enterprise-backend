package org.dromara.carbon.enterprise.factor.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.factor.domain.CeFactorCacheVersion;
import org.dromara.carbon.enterprise.factor.domain.vo.CeFactorCacheVersionVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local factor cache version mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeFactorCacheVersionMapper extends BaseMapperPlus<CeFactorCacheVersion, CeFactorCacheVersionVo> {
}
