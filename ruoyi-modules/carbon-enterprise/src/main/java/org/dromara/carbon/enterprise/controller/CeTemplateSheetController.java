package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateSheetVo;
import org.dromara.carbon.enterprise.service.ICeTemplateSheetService;
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
 * Enterprise source workbook sheet inventory query endpoints.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/template-sheet")
public class CeTemplateSheetController extends BaseController {

    private final ICeTemplateSheetService templateSheetService;

    /**
     * List enterprise-local Excel template sheets.
     *
     * @param templateVersionId optional template version id
     */
    @SaCheckPermission("enterprise:templateSheet:list")
    @GetMapping("/list")
    public R<List<CeTemplateSheetVo>> list(@RequestParam(required = false) Long templateVersionId) {
        return R.ok(templateSheetService.listSheets(templateVersionId));
    }

    /**
     * Get one enterprise-local Excel template sheet.
     *
     * @param id sheet id
     */
    @SaCheckPermission("enterprise:templateSheet:query")
    @GetMapping("/{id}")
    public R<CeTemplateSheetVo> getInfo(@PathVariable Long id) {
        return R.ok(templateSheetService.getById(id));
    }
}
