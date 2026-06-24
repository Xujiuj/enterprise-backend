package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.sourcea.CeSourceAImportResult;
import org.dromara.carbon.enterprise.service.ICeSourceAImportService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Imports the multi-workbook Source(A) dataset into enterprise-local ce_* tables.
 *
 * <p>This is intentionally a composite importer rather than generated CRUD:
 * Source(A) spans many workbooks and requires ordered FK validation.</p>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/source-a")
public class CeSourceAImportController extends BaseController {

    private final ICeSourceAImportService sourceAImportService;

    @SaCheckPermission("enterprise:sourceA:import")
    @PostMapping("/import")
    public R<CeSourceAImportResult> importFiles(@RequestPart("files") MultipartFile[] files) {
        List<MultipartFile> sourceFiles = files == null ? List.of() : Arrays.asList(files);
        return R.ok(sourceAImportService.importFiles(sourceFiles));
    }

    @SaCheckPermission("enterprise:sourceA:import")
    @PostMapping("/import-directory")
    public R<CeSourceAImportResult> importDirectory(@RequestParam("path") String sourceDirectory) {
        return R.ok(sourceAImportService.importDirectory(sourceDirectory));
    }

    @SaCheckPermission("enterprise:sourceA:validate")
    @GetMapping("/validate")
    public R<CeSourceAImportResult> validateCurrentData() {
        return R.ok(sourceAImportService.validateCurrentData());
    }
}
