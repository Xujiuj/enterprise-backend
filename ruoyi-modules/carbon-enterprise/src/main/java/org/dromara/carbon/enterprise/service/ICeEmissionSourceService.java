package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeEmissionSourceBo;
import org.dromara.carbon.enterprise.domain.vo.CeEmissionSourceVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local emission source service.
 */
public interface ICeEmissionSourceService {

    TableDataInfo<CeEmissionSourceVo> queryPageList(CeEmissionSourceBo bo, PageQuery pageQuery);

    List<CeEmissionSourceVo> queryList(CeEmissionSourceBo bo);

    CeEmissionSourceVo queryById(Long id);

    Boolean insertByBo(CeEmissionSourceBo bo);

    Boolean updateByBo(CeEmissionSourceBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
