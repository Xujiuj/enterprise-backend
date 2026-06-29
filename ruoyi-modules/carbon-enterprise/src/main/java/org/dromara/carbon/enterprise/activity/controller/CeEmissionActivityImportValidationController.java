package org.dromara.carbon.enterprise.activity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityCaptureResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityCaptureService;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityImportValidationService;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityValidationService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Import and manual-entry API for emission activity data.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/activity-import/emission-activity")
public class CeEmissionActivityImportValidationController extends BaseController {

    private final ICeEmissionActivityImportValidationService activityImportValidationService;
    private final ICeEmissionActivityCaptureService activityCaptureService;
    private final ICeEmissionActivityValidationService activityValidationService;

    @SaCheckPermission("enterprise:activityImportValidation:validate")
    @GetMapping("/fields")
    public R<List<CeEmissionActivityFieldDescriptor>> fields() {
        return R.ok(activityValidationService.listEntryFields());
    }

    @SaCheckPermission("enterprise:activityImportValidation:validate")
    @PostMapping("/validate")
    public R<CeEmissionActivityImportValidationResult> validate(@RequestBody CeEmissionActivityImportValidationRequest request) {
        return R.ok(activityImportValidationService.validateImport(request));
    }

    @SaCheckPermission("enterprise:activityImportValidation:validate")
    @PostMapping("/parse-file")
    public R<CeEmissionActivityImportValidationRequest> parseFile(@RequestPart("file") MultipartFile file) {
        return R.ok(activityImportValidationService.parseImportFile(file));
    }

    @SaCheckPermission("enterprise:activity:save")
    @PostMapping("/save")
    public R<CeEmissionActivityCaptureResult> save(@RequestBody CeEmissionActivityValidationRequest request) {
        return R.ok(activityCaptureService.saveManual(request));
    }

    @SaCheckPermission("enterprise:activityImport:import")
    @PostMapping("/import")
    public R<CeEmissionActivityCaptureResult> importRows(@RequestBody CeEmissionActivityImportValidationRequest request) {
        return R.ok(activityCaptureService.importRows(request));
    }
}
