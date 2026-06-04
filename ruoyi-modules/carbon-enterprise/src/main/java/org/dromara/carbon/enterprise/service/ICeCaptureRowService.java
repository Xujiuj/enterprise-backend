package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeCaptureRowBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureRowVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * Enterprise local data capture row service.
 */
public interface ICeCaptureRowService {

    /**
     * Page capture rows by query object.
     *
     * @param row query object
     * @param pageQuery page query
     * @return matching capture rows
     */
    TableDataInfo<CeCaptureRowVo> selectPageRowList(CeCaptureRowBo row, PageQuery pageQuery);

    /**
     * Get one capture row by id.
     *
     * @param id row id
     * @return row or null
     */
    CeCaptureRowVo selectRowById(Long id);
}
