package org.dromara.carbon.enterprise.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.domain.CeReportContentSyncResponse;
import org.dromara.carbon.enterprise.report.domain.vo.CeReportContentVo;
import org.dromara.carbon.enterprise.shared.service.ICeReportContentService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    @PostMapping("/sync")
    public R<CeReportContentSyncResponse> sync() {
        return R.ok(reportContentService.syncContent());
    }
}
