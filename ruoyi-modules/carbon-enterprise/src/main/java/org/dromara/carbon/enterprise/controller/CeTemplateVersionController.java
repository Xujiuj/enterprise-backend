package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateVersionVo;
import org.dromara.carbon.enterprise.service.ICeTemplateVersionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Enterprise Excel template version query endpoints.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/template-version")
public class CeTemplateVersionController extends BaseController {

    private final ICeTemplateVersionService templateVersionService;

    /**
     * List enterprise-local Excel template versions.
     */
    @SaCheckPermission("enterprise:templateVersion:list")
    @GetMapping("/list")
    public R<List<CeTemplateVersionVo>> list() {
        return R.ok(templateVersionService.listVersions());
    }

    /**
     * Get one enterprise-local Excel template version.
     *
     * @param id template version id
     */
    @SaCheckPermission("enterprise:templateVersion:query")
    @GetMapping("/{id}")
    public R<CeTemplateVersionVo> getInfo(@PathVariable Long id) {
        return R.ok(templateVersionService.getById(id));
    }
}
