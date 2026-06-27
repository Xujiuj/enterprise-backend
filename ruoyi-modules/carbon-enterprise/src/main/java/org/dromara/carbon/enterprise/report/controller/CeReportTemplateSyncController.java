package org.dromara.carbon.enterprise.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.domain.CeReportTemplateSyncResponse;
import org.dromara.carbon.enterprise.shared.service.ICeReportTemplateSyncService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise report template sync API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/report-template-sync")
public class CeReportTemplateSyncController {

    private final ICeReportTemplateSyncService reportTemplateSyncService;

    @SaCheckPermission("enterprise:reportTemplateSync:run")
    @PostMapping("/run")
    public R<CeReportTemplateSyncResponse> run() {
        return R.ok(reportTemplateSyncService.syncCurrentLicenseReportTemplates());
    }
}
