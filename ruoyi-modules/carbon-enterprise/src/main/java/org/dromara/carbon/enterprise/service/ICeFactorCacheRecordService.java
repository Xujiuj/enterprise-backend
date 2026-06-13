package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeFactorCacheRecordBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheRecordVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local factor cache record service.
 */
public interface ICeFactorCacheRecordService {

    TableDataInfo<CeFactorCacheRecordVo> queryPageList(CeFactorCacheRecordBo bo, PageQuery pageQuery);

    List<CeFactorCacheRecordVo> queryList(CeFactorCacheRecordBo bo);

    CeFactorCacheRecordVo queryById(Long id);

    Boolean insertByBo(CeFactorCacheRecordBo bo);

    Boolean updateByBo(CeFactorCacheRecordBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
