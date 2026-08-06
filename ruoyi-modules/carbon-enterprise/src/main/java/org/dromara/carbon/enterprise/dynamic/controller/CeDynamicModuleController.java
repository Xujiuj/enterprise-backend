package org.dromara.carbon.enterprise.dynamic.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.dynamic.domain.CeDynamicModels;
import org.dromara.carbon.enterprise.dynamic.service.CeDynamicModuleService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Excel-driven dynamic module management API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/dynamic-module")
public class CeDynamicModuleController {

    private final CeDynamicModuleService dynamicModuleService;

    @SaCheckPermission("enterprise:dynamicModule:preview")
    @PostMapping("/preview")
    public R<CeDynamicModels.WorkbookPreview> preview(@RequestPart("file") MultipartFile file) {
        return R.ok(dynamicModuleService.preview(file));
    }

    @SaCheckPermission("enterprise:dynamicModule:generate")
    @PostMapping("/generate")
    public R<CeDynamicModels.GenerateResult> generate(@RequestPart("file") MultipartFile file,
                                                       @RequestPart("definition") String definition) {
        CeDynamicModels.GenerateRequest request = JsonUtils.parseObject(definition, CeDynamicModels.GenerateRequest.class);
        if (request == null) {
            throw new ServiceException("页面生成配置格式不正确");
        }
        return R.ok(dynamicModuleService.generate(file, request));
    }

    @SaCheckPermission("enterprise:dynamicModule:list")
    @GetMapping("/list")
    public R<List<CeDynamicModels.ModuleSchema>> list() {
        return R.ok(dynamicModuleService.listModules());
    }

    @SaCheckPermission("enterprise:dynamicModule:remove")
    @DeleteMapping("/{moduleCodes}")
    public R<Void> remove(@PathVariable String[] moduleCodes) {
        dynamicModuleService.archiveModules(List.of(moduleCodes));
        return R.ok();
    }

    @SaCheckPermission("enterprise:dynamicModule:remove")
    @PutMapping("/{moduleCode}/restore")
    public R<CeDynamicModels.ModuleSchema> restore(@PathVariable String moduleCode) {
        return R.ok(dynamicModuleService.restoreModule(moduleCode));
    }
}
