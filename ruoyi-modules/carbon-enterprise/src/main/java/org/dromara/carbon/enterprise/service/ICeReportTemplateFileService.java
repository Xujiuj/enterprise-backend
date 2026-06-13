package org.dromara.carbon.enterprise.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.carbon.enterprise.domain.bo.CeReportTemplateFileBo;
import org.dromara.carbon.enterprise.domain.vo.CeReportTemplateFileVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Enterprise local report template download catalog service.
 */
public interface ICeReportTemplateFileService {

    TableDataInfo<CeReportTemplateFileVo> queryPageList(CeReportTemplateFileBo bo, PageQuery pageQuery);

    List<CeReportTemplateFileVo> queryList(CeReportTemplateFileBo bo);

    CeReportTemplateFileVo queryById(Long id);

    void download(Long id, HttpServletResponse response) throws IOException;

    Boolean insertByBo(CeReportTemplateFileBo bo);

    Boolean updateByBo(CeReportTemplateFileBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
