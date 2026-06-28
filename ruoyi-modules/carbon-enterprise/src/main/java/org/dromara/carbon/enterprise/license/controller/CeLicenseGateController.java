package org.dromara.carbon.enterprise.license.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.license.domain.CeLicenseGateResponse;
import org.dromara.carbon.enterprise.license.domain.CeLicenseGateResult;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseGateService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * Read-only enterprise license gate API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/license-gate")
public class CeLicenseGateController extends BaseController {

    private final ICeLicenseGateService licenseGateService;

    @SaCheckPermission("enterprise:licenseState:query")
    @GetMapping("/current")
    public R<CeLicenseGateResponse> current(@NotBlank(message = "部署指纹不能为空")
                                            @RequestParam String expectedInstallId,
                                            @RequestParam(required = false) String featureCode) {
        CeLicenseGateResult result = StringUtils.isBlank(featureCode)
            ? licenseGateService.evaluateCurrent(expectedInstallId, new Date())
            : licenseGateService.evaluateCurrent(expectedInstallId, new Date(), featureCode);
        return R.ok(CeLicenseGateResponse.from(result));
    }
}
