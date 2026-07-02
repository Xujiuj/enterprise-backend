package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.report.domain.CeReportContentSyncResponse;
import org.dromara.carbon.enterprise.report.domain.bo.CeReportContentBo;
import org.dromara.carbon.enterprise.report.domain.vo.CeReportContentVo;

import java.util.List;

/**
 * Enterprise local report content catalog service.
 */
public interface ICeReportContentService {

    /**
     * List report content rows ordered by customer workbook order.
     *
     * @return report content rows
     */
    List<CeReportContentVo> listContent();

    CeReportContentVo getContent(Long id);

    Boolean insertContent(CeReportContentBo bo);

    Boolean updateContent(CeReportContentBo bo);

    Boolean deleteContent(Long[] ids);

    /**
     * Sync report content rows from vendor configuration to local enterprise table.
     *
     * @return sync result
     */
    CeReportContentSyncResponse syncContent();
}
