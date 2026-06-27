package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.activity.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local activity data service.
 *
 * <p>Note: {@code insertByBo} is used by the emission_activity capture pipeline.
 * The controller still blocks direct raw creation, while update/delete/status
 * operations are exposed for maintaining already persisted activity data.</p>
 */
public interface ICeActivityDataService {

    TableDataInfo<CeActivityDataVo> queryPageList(CeActivityDataBo bo, PageQuery pageQuery);

    List<CeActivityDataVo> queryList(CeActivityDataBo bo);

    CeActivityDataValidationDashboardVo queryValidationDashboard(CeActivityDataBo bo);

    CeActivityDataVo queryById(Long id);

    Boolean insertByBo(CeActivityDataBo bo);

    Boolean updateByBo(CeActivityDataBo bo);

    Boolean deleteByIds(Collection<Long> ids);

    Boolean updateStatusByIds(Collection<Long> ids, String dataStatus);
}
