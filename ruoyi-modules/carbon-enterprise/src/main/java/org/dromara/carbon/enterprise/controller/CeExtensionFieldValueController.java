package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeExtensionFieldValueBo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldValueVo;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldValueService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Enterprise extension field value API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/extension-field-value")
public class CeExtensionFieldValueController extends BaseController {

    private final ICeExtensionFieldValueService extensionFieldValueService;

    @SaCheckPermission("enterprise:extensionFieldValue:list")
    @GetMapping("/list")
    public TableDataInfo<CeExtensionFieldValueVo> list(CeExtensionFieldValueBo bo, PageQuery pageQuery) {
        return extensionFieldValueService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:extensionFieldValue:query")
    @GetMapping("/{id}")
    public R<CeExtensionFieldValueVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(extensionFieldValueService.queryById(id));
    }

    @SaCheckPermission("enterprise:extensionFieldValue:add")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeExtensionFieldValueBo bo) {
        return toAjax(extensionFieldValueService.insertByBo(bo));
    }

    @SaCheckPermission("enterprise:extensionFieldValue:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeExtensionFieldValueBo bo) {
        return toAjax(extensionFieldValueService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:extensionFieldValue:edit")
    @PutMapping("/batch")
    public R<Void> saveBatch(@RequestBody List<CeExtensionFieldValueBo> values) {
        return toAjax(extensionFieldValueService.saveBatch(values));
    }

    @SaCheckPermission("enterprise:extensionFieldValue:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(extensionFieldValueService.deleteByIds(List.of(ids)));
    }
}
