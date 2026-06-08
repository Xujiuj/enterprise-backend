package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.domain.CeTemplateField;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateFieldVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise original field preservation inventory mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeTemplateFieldMapper extends BaseMapperPlus<CeTemplateField, CeTemplateFieldVo> {
}
