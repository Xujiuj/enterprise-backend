package org.dromara.carbon.enterprise.license.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.license.domain.CeLicenseInstallIdResponse;
import org.dromara.carbon.enterprise.license.domain.CeLicenseImportRequest;
import org.dromara.carbon.enterprise.license.domain.CeLicenseImportResponse;
import org.dromara.carbon.enterprise.license.domain.CeLicenseImportResult;
import org.dromara.carbon.enterprise.license.service.CeLicensePublicKeyProvider;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseImportService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * Enterprise license import API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/license-import")
public class CeLicenseImportController extends BaseController {

    private final ICeLicenseImportService licenseImportService;
    private final CeLicensePublicKeyProvider publicKeyProvider;
    private final org.dromara.carbon.enterprise.license.service.CeLicenseInstallIdProvider installIdProvider;

    @GetMapping("/install-id")
    public R<CeLicenseInstallIdResponse> currentInstallId() {
        return R.ok(new CeLicenseInstallIdResponse(installIdProvider.getExpectedInstallId()));
    }

    @SaCheckPermission("enterprise:licenseImport:import")
    @PostMapping("/import")
    public R<CeLicenseImportResponse> importLicense(@Validated @RequestBody CeLicenseImportRequest request) {
        String publicKeyPem = publicKeyProvider.getPublicKeyPem();
        if (StringUtils.isBlank(publicKeyPem)) {
            return R.ok(CeLicenseImportResponse.publicKeyUnavailable());
        }

        CeLicenseImportResult result = licenseImportService.importLicense(
            request.getLicenseContent(),
            publicKeyPem,
            request.getExpectedInstallId(),
            new Date()
        );
        return R.ok(CeLicenseImportResponse.from(result));
    }
}
