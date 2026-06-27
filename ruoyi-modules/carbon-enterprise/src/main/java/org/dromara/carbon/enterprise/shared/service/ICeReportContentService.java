package org.dromara.carbon.enterprise.shared.service;

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
}
