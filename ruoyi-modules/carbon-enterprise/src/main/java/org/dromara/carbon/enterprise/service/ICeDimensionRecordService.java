package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.domain.vo.CeDimensionRecordVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise dimension record service.
 */
public interface ICeDimensionRecordService {

    TableDataInfo<CeDimensionRecordVo> queryPageList(CeDimensionRecordBo bo, PageQuery pageQuery);

    List<CeDimensionRecordVo> queryList(CeDimensionRecordBo bo);

    CeDimensionRecordVo queryById(Long id);

    Boolean insertByBo(CeDimensionRecordBo bo);

    Boolean updateByBo(CeDimensionRecordBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
