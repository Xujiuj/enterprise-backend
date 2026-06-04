package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportRequest;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResponse;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResult;
import org.dromara.carbon.enterprise.service.CeLicensePublicKeyProvider;
import org.dromara.carbon.enterprise.service.ICeLicenseImportService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
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
