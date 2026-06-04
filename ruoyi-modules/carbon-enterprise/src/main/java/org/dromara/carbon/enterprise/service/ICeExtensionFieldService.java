package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeExtensionFieldBo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise allowed extension fields service.
 */
public interface ICeExtensionFieldService {

    /**
     * Page extension fields by query object.
     *
     * @param bo query object
     * @param pageQuery page query
     * @return matching extension fields
     */
    TableDataInfo<CeExtensionFieldVo> queryPageList(CeExtensionFieldBo bo, PageQuery pageQuery);

    /**
     * List extension fields by query object.
     *
     * @param bo query object
     * @return matching extension fields
     */
    List<CeExtensionFieldVo> queryList(CeExtensionFieldBo bo);

    /**
     * Get one extension field by id.
     *
     * @param id extension field id
     * @return extension field or null
     */
    CeExtensionFieldVo queryById(Long id);

    /**
     * Insert one extension field.
     *
     * @param bo extension field
     * @return whether inserted
     */
    Boolean insertByBo(CeExtensionFieldBo bo);

    /**
     * Update one extension field.
     *
     * @param bo extension field
     * @return whether updated
     */
    Boolean updateByBo(CeExtensionFieldBo bo);

    /**
     * Delete extension fields by ids.
     *
     * @param ids extension field ids
     * @return whether deleted
     */
    Boolean deleteByIds(Collection<Long> ids);
}
