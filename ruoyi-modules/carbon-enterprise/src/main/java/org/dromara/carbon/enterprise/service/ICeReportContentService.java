package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.vo.CeReportContentVo;

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
