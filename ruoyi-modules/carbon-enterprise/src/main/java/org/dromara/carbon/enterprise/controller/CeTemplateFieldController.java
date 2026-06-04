package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateFieldVo;
import org.dromara.carbon.enterprise.service.ICeTemplateFieldService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Enterprise original field preservation inventory query endpoints.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/template-field")
public class CeTemplateFieldController extends BaseController {

    private final ICeTemplateFieldService templateFieldService;

    /**
     * List enterprise-local Excel template fields.
     *
     * @param sheetId optional sheet id
     */
    @SaCheckPermission("enterprise:templateField:list")
    @GetMapping("/list")
    public R<List<CeTemplateFieldVo>> list(@RequestParam(required = false) Long sheetId) {
        return R.ok(templateFieldService.listFields(sheetId));
    }

    /**
     * Get one enterprise-local Excel template field.
     *
     * @param id field id
     */
    @SaCheckPermission("enterprise:templateField:query")
    @GetMapping("/{id}")
    public R<CeTemplateFieldVo> getInfo(@PathVariable Long id) {
        return R.ok(templateFieldService.getById(id));
    }
}
