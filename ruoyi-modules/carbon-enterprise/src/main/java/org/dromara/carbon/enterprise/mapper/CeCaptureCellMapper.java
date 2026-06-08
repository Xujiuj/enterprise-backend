package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureCellVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local data capture cell mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeCaptureCellMapper extends BaseMapperPlus<CeCaptureCell, CeCaptureCellVo> {
}
