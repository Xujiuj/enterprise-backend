package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.vo.CeTemplateFieldVo;

import java.util.List;

/**
 * Enterprise original field preservation inventory service.
 */
public interface ICeTemplateFieldService {

    /**
     * List fields, optionally scoped to one sheet.
     *
     * @param sheetId sheet id, optional
     * @return matching fields ordered by sheet and field order
     */
    List<CeTemplateFieldVo> listFields(Long sheetId);

    /**
     * Get one field by id.
     *
     * @param id field id
     * @return field or null
     */
    CeTemplateFieldVo getById(Long id);
}
