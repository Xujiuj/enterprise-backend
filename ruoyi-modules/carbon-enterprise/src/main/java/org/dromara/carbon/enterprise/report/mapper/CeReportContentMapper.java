package org.dromara.carbon.enterprise.report.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.report.domain.CeReportContent;
import org.dromara.carbon.enterprise.report.domain.vo.CeReportContentVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local report content catalog mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeReportContentMapper extends BaseMapperPlus<CeReportContent, CeReportContentVo> {
}
