package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.domain.CeTemplateVersion;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateVersionVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise Excel template version mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeTemplateVersionMapper extends BaseMapperPlus<CeTemplateVersion, CeTemplateVersionVo> {
}
