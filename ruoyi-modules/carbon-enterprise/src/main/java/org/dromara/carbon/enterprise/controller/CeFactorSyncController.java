package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.sync.CeFactorSyncResponse;
import org.dromara.carbon.enterprise.service.ICeFactorSyncService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise factor sync API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/factor-sync")
public class CeFactorSyncController {

    private final ICeFactorSyncService factorSyncService;

    @SaCheckPermission("enterprise:factorSync:run")
    @PostMapping("/run")
    public R<CeFactorSyncResponse> run() {
        return R.ok(factorSyncService.syncCurrentLicenseFactors());
    }
}
