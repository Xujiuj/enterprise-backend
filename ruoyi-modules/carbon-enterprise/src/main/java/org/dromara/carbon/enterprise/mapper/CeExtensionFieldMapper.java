package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.domain.CeExtensionField;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise allowed extension fields mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeExtensionFieldMapper extends BaseMapperPlus<CeExtensionField, CeExtensionFieldVo> {
}
