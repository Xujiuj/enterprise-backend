package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ActivityCaptureResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityCaptureService;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityImportValidationService;
import org.dromara.carbon.enterprise.service.ICeSheet656DerivedFieldResolver;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validate-only import API for the frozen sheet_656 activity shape.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/activity-import/sheet-656")
public class CeSheet656ActivityImportValidationController extends BaseController {

    private final ICeSheet656ActivityImportValidationService activityImportValidationService;
    private final ICeSheet656ActivityCaptureService activityCaptureService;
    private final ICeSheet656DerivedFieldResolver derivedFieldResolver;

    @SaCheckPermission("enterprise:activityImportValidation:validate")
    @PostMapping("/validate")
    public R<CeSheet656ImportValidationResult> validate(@RequestBody CeSheet656ImportValidationRequest request) {
        return R.ok(activityImportValidationService.validateImport(request));
    }

    @SaCheckPermission("enterprise:activityImportValidation:validate")
    @PostMapping("/parse-file")
    public R<CeSheet656ImportValidationRequest> parseFile(@RequestPart("file") MultipartFile file) {
        return R.ok(activityImportValidationService.parseImportFile(file));
    }

    @SaCheckPermission("enterprise:activity:save")
    @PostMapping("/save")
    public R<CeSheet656ActivityCaptureResult> save(@RequestBody CeSheet656ValidationRequest request) {
        return R.ok(activityCaptureService.saveManual(request));
    }

    @SaCheckPermission("enterprise:activity:save")
    @GetMapping("/source/{sourceIdentificationCode}")
    public R<CeSheet656ResolvedRow> resolveSource(@PathVariable String sourceIdentificationCode) {
        CeSheet656ResolvedRow row = derivedFieldResolver.resolve(sourceIdentificationCode)
            .orElseThrow(() -> new ServiceException("排放源识别编号不存在或未启用：" + sourceIdentificationCode));
        return R.ok(row);
    }

    @SaCheckPermission("enterprise:activityImport:import")
    @PostMapping("/import")
    public R<CeSheet656ActivityCaptureResult> importRows(@RequestBody CeSheet656ImportValidationRequest request) {
        return R.ok(activityCaptureService.importRows(request));
    }
}
