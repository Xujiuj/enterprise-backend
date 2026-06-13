package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeExtensionFieldValueBo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldValueVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise extension field value service.
 */
public interface ICeExtensionFieldValueService {

    TableDataInfo<CeExtensionFieldValueVo> queryPageList(CeExtensionFieldValueBo bo, PageQuery pageQuery);

    List<CeExtensionFieldValueVo> queryList(CeExtensionFieldValueBo bo);

    CeExtensionFieldValueVo queryById(Long id);

    Boolean insertByBo(CeExtensionFieldValueBo bo);

    Boolean updateByBo(CeExtensionFieldValueBo bo);

    Boolean saveBatch(List<CeExtensionFieldValueBo> values);

    Boolean deleteByIds(Collection<Long> ids);
}
