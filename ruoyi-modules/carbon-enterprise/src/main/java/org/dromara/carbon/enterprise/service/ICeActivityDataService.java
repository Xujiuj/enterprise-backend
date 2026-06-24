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
 *
 * <p>Note: {@code insertByBo}, {@code updateByBo}, and {@code deleteByIds} are
 * intentionally exposed at the service layer because the sheet_656 capture
 * pipeline delegates to them. The controller blocks direct CRUD access and
 * routes all writes through the sheet_656 validation/import flow.</p>
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
