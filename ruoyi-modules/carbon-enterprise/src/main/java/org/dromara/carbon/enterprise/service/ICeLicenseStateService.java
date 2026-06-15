package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeLicenseStateBo;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Date;
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

    /**
     * Updates expired {@code VALID} license state rows to {@code EXPIRED}.
     *
     * @param evaluationTime evaluation timestamp used to identify rows where {@code valid_to < evaluationTime};
     *                       must not be {@code null}
     * @return number of rows updated
     * @throws NullPointerException if {@code evaluationTime} is {@code null}
     */
    int expireValidLicenses(Date evaluationTime);

    Boolean insertByBo(CeLicenseStateBo bo);

    Boolean updateByBo(CeLicenseStateBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
