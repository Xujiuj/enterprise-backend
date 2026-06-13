package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.domain.vo.CeReportTemplateFileVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local report template download catalog mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeReportTemplateFileMapper extends BaseMapperPlus<CeReportTemplateFile, CeReportTemplateFileVo> {
}
