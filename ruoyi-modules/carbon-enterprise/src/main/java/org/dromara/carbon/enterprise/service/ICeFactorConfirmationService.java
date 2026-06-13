package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeFactorConfirmationBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorConfirmationVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local emission factor confirmation service.
 */
public interface ICeFactorConfirmationService {

    TableDataInfo<CeFactorConfirmationVo> queryPageList(CeFactorConfirmationBo bo, PageQuery pageQuery);

    List<CeFactorConfirmationVo> queryList(CeFactorConfirmationBo bo);

    CeFactorConfirmationVo queryById(Long id);

    Boolean insertByBo(CeFactorConfirmationBo bo);

    Boolean updateByBo(CeFactorConfirmationBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
