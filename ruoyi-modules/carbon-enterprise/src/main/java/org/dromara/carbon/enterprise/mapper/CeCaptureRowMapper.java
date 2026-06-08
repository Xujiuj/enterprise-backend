package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureRowVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local data capture row mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeCaptureRowMapper extends BaseMapperPlus<CeCaptureRow, CeCaptureRowVo> {
}
