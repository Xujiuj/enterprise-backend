package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheRecordVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local factor cache record mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeFactorCacheRecordMapper extends BaseMapperPlus<CeFactorCacheRecord, CeFactorCacheRecordVo> {
}
