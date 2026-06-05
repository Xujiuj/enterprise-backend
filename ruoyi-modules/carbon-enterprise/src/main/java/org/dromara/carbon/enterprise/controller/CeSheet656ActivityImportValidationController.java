package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityImportValidationService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Validate-only import API for the frozen sheet_656 activity shape.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/activity-import/sheet-656")
public class CeSheet656ActivityImportValidationController extends BaseController {

    private final ICeSheet656ActivityImportValidationService activityImportValidationService;

    @SaCheckPermission("enterprise:activityImportValidation:validate")
    @PostMapping("/validate")
    public R<CeSheet656ImportValidationResult> validate(@RequestBody CeSheet656ImportValidationRequest request) {
        return R.ok(activityImportValidationService.validateImport(request));
    }
}
