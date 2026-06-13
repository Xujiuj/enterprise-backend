package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local activity data service.
 */
public interface ICeActivityDataService {

    TableDataInfo<CeActivityDataVo> queryPageList(CeActivityDataBo bo, PageQuery pageQuery);

    List<CeActivityDataVo> queryList(CeActivityDataBo bo);

    CeActivityDataValidationDashboardVo queryValidationDashboard(CeActivityDataBo bo);

    CeActivityDataVo queryById(Long id);

    Boolean insertByBo(CeActivityDataBo bo);

    Boolean updateByBo(CeActivityDataBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
