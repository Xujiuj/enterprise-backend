package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeCaptureCellBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureCellVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * Enterprise local data capture cell service.
 */
public interface ICeCaptureCellService {

    /**
     * Page capture cells by query object.
     *
     * @param cell query object
     * @param pageQuery page query
     * @return matching capture cells
     */
    TableDataInfo<CeCaptureCellVo> selectPageCellList(CeCaptureCellBo cell, PageQuery pageQuery);

    /**
     * Get one capture cell by id.
     *
     * @param id cell id
     * @return cell or null
     */
    CeCaptureCellVo selectCellById(Long id);
}
