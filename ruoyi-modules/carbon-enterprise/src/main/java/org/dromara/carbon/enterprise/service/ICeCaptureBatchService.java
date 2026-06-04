package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeCaptureBatchBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureBatchVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * Enterprise local data capture batch service.
 */
public interface ICeCaptureBatchService {

    /**
     * Page capture batches by query object.
     *
     * @param batch query object
     * @param pageQuery page query
     * @return matching capture batches
     */
    TableDataInfo<CeCaptureBatchVo> selectPageBatchList(CeCaptureBatchBo batch, PageQuery pageQuery);

    /**
     * Get one capture batch by id.
     *
     * @param id batch id
     * @return batch or null
     */
    CeCaptureBatchVo selectBatchById(Long id);
}
