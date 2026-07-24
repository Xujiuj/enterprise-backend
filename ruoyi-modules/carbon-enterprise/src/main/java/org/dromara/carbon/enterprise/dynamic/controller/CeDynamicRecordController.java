package org.dromara.carbon.enterprise.dynamic.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.dynamic.domain.CeDynamicModels;
import org.dromara.carbon.enterprise.dynamic.service.CeDynamicModuleService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Generic CRUD API for generated enterprise management pages.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/dynamic/{moduleCode}")
public class CeDynamicRecordController {

    private final CeDynamicModuleService dynamicModuleService;

    @GetMapping("/schema")
    public R<CeDynamicModels.ModuleSchema> schema(@PathVariable String moduleCode) {
        return R.ok(dynamicModuleService.getSchema(moduleCode));
    }

    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(@PathVariable String moduleCode,
                                                    @RequestParam Map<String, String> params) {
        return dynamicModuleService.listRecords(moduleCode, params);
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getInfo(@PathVariable String moduleCode,
                                          @NotNull(message = "id不能为空") @PathVariable Long id) {
        return R.ok(dynamicModuleService.getRecord(moduleCode, id));
    }

    @PostMapping
    public R<Long> add(@PathVariable String moduleCode, @RequestBody Map<String, Object> payload) {
        return R.ok(dynamicModuleService.addRecord(moduleCode, payload));
    }

    @PutMapping
    public R<Void> edit(@PathVariable String moduleCode, @RequestBody Map<String, Object> payload) {
        dynamicModuleService.updateRecord(moduleCode, payload);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable String moduleCode,
                          @NotEmpty(message = "ids不能为空") @PathVariable Long[] ids) {
        dynamicModuleService.deleteRecords(moduleCode, ids);
        return R.ok();
    }

    @PostMapping("/import")
    public R<Integer> importRows(@PathVariable String moduleCode, @RequestPart("file") MultipartFile file) {
        return R.ok(dynamicModuleService.importRows(moduleCode, file));
    }
}
