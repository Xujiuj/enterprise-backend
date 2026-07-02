package org.dromara.carbon.enterprise.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.domain.CeReportContentSyncResponse;
import org.dromara.carbon.enterprise.report.domain.bo.CeReportContentBo;
import org.dromara.carbon.enterprise.report.domain.vo.CeReportContentVo;
import org.dromara.carbon.enterprise.shared.service.ICeReportContentService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Enterprise local report content catalog endpoints.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/report-content")
public class CeReportContentController extends BaseController {

    private final ICeReportContentService reportContentService;

    @SaCheckPermission("enterprise:reports:view")
    @GetMapping("/list")
    public R<List<CeReportContentVo>> list() {
        return R.ok(reportContentService.listContent());
    }

    @SaCheckPermission("enterprise:reports:view")
    @GetMapping("/{id}")
    public R<CeReportContentVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(reportContentService.getContent(id));
    }

    @SaCheckPermission("enterprise:reports:view")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeReportContentBo bo) {
        return toAjax(reportContentService.insertContent(bo));
    }

    @SaCheckPermission("enterprise:reports:view")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeReportContentBo bo) {
        return toAjax(reportContentService.updateContent(bo));
    }

    @SaCheckPermission("enterprise:reports:view")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(reportContentService.deleteContent(ids));
    }

    @SaCheckPermission("enterprise:reports:view")
    @PostMapping("/sync")
    public R<CeReportContentSyncResponse> sync() {
        return R.ok(reportContentService.syncContent());
    }
}
