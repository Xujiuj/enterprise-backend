package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.vo.CeTemplateSheetVo;

import java.util.List;

/**
 * Enterprise source workbook sheet inventory service.
 */
public interface ICeTemplateSheetService {

    /**
     * List sheets, optionally scoped to one template version.
     *
     * @param templateVersionId template version id, optional
     * @return matching sheets ordered by id
     */
    List<CeTemplateSheetVo> listSheets(Long templateVersionId);

    /**
     * Get one sheet by id.
     *
     * @param id sheet id
     * @return sheet or null
     */
    CeTemplateSheetVo getById(Long id);
}
