package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeLicenseStateBo;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local license runtime state service.
 */
public interface ICeLicenseStateService {

    TableDataInfo<CeLicenseStateVo> queryPageList(CeLicenseStateBo bo, PageQuery pageQuery);

    List<CeLicenseStateVo> queryList(CeLicenseStateBo bo);

    CeLicenseStateVo queryById(Long id);

    CeLicenseStateVo queryCurrent();

    Boolean insertByBo(CeLicenseStateBo bo);

    Boolean updateByBo(CeLicenseStateBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
