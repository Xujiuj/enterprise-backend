package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.vo.CeTemplateVersionVo;

import java.util.List;

/**
 * Enterprise Excel template version service.
 */
public interface ICeTemplateVersionService {

    /**
     * List template versions available in the enterprise-local database.
     *
     * @return template versions ordered by latest first
     */
    List<CeTemplateVersionVo> listVersions();

    /**
     * Get one template version by id.
     *
     * @param id template version id
     * @return template version or null
     */
    CeTemplateVersionVo getById(Long id);
}
