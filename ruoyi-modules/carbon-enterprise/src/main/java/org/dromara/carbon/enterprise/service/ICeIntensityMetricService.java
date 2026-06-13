package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeIntensityMetricBo;
import org.dromara.carbon.enterprise.domain.vo.CeIntensityMetricVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local carbon intensity metric service.
 */
public interface ICeIntensityMetricService {

    TableDataInfo<CeIntensityMetricVo> queryPageList(CeIntensityMetricBo bo, PageQuery pageQuery);

    List<CeIntensityMetricVo> queryList(CeIntensityMetricBo bo);

    CeIntensityMetricVo queryById(Long id);

    Boolean insertByBo(CeIntensityMetricBo bo);

    Boolean updateByBo(CeIntensityMetricBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
