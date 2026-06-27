package org.dromara.carbon.enterprise.template.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.dromara.carbon.enterprise.template.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.template.domain.vo.CeTemplateSheetVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise source workbook sheet inventory mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeTemplateSheetMapper extends BaseMapperPlus<CeTemplateSheet, CeTemplateSheetVo> {
}
