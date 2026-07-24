package org.dromara.carbon.enterprise.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.domain.CePowerBiConfig;
import org.dromara.carbon.enterprise.report.service.CePowerBiConfigService;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise Power BI configuration API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/report-config/powerbi")
public class CePowerBiConfigController {

    private final CePowerBiConfigService powerBiConfigService;

    @SaCheckPermission("enterprise:reports:view")
    @GetMapping
    public R<CePowerBiConfig> getConfig() {
        return R.ok(powerBiConfigService.getConfig());
    }

    @SaCheckPermission("enterprise:reports:powerbi:edit")
    @Log(title = "Power BI链接配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<CePowerBiConfig> updateConfig(@Valid @RequestBody CePowerBiConfig config) {
        return R.ok(powerBiConfigService.saveConfig(config));
    }
}
