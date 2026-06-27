package org.dromara.carbon.enterprise.report.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.report.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.report.domain.vo.CeReportTemplateFileVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local report template download catalog mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeReportTemplateFileMapper extends BaseMapperPlus<CeReportTemplateFile, CeReportTemplateFileVo> {
}
