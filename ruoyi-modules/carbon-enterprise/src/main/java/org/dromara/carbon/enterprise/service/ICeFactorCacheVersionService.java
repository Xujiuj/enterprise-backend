package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeFactorCacheVersionBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheVersionVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local factor cache version service.
 */
public interface ICeFactorCacheVersionService {

    TableDataInfo<CeFactorCacheVersionVo> queryPageList(CeFactorCacheVersionBo bo, PageQuery pageQuery);

    List<CeFactorCacheVersionVo> queryList(CeFactorCacheVersionBo bo);

    CeFactorCacheVersionVo queryById(Long id);

    Boolean insertByBo(CeFactorCacheVersionBo bo);

    Boolean updateByBo(CeFactorCacheVersionBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
