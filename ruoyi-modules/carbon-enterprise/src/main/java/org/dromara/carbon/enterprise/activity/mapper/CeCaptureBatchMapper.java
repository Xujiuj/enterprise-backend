package org.dromara.carbon.enterprise.activity.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.activity.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.activity.domain.vo.CeCaptureBatchVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local data capture batch mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeCaptureBatchMapper extends BaseMapperPlus<CeCaptureBatch, CeCaptureBatchVo> {
}
