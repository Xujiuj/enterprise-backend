package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeExtensionFieldValue;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldValueVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise extension field value mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeExtensionFieldValueMapper extends BaseMapperPlus<CeExtensionFieldValue, CeExtensionFieldValueVo> {
}
