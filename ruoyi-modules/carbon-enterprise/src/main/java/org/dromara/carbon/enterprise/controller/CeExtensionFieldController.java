package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeExtensionFieldBo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldVo;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldService;
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
 * Enterprise allowed extension fields API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/extension-field")
public class CeExtensionFieldController extends BaseController {

    private final ICeExtensionFieldService extensionFieldService;

    /**
     * List enterprise extension fields.
     */
    @SaCheckPermission("enterprise:extensionField:list")
    @GetMapping("/list")
    public TableDataInfo<CeExtensionFieldVo> list(CeExtensionFieldBo bo, PageQuery pageQuery) {
        return extensionFieldService.queryPageList(bo, pageQuery);
    }

    /**
     * Get enterprise extension field details.
     *
     * @param id extension field id
     */
    @SaCheckPermission("enterprise:extensionField:query")
    @GetMapping("/{id}")
    public R<CeExtensionFieldVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(extensionFieldService.queryById(id));
    }

    /**
     * Add enterprise extension field.
     */
    @SaCheckPermission("enterprise:extensionField:add")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeExtensionFieldBo bo) {
        return toAjax(extensionFieldService.insertByBo(bo));
    }

    /**
     * Edit enterprise extension field.
     */
    @SaCheckPermission("enterprise:extensionField:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeExtensionFieldBo bo) {
        return toAjax(extensionFieldService.updateByBo(bo));
    }

    /**
     * Delete enterprise extension fields.
     *
     * @param ids extension field ids
     */
    @SaCheckPermission("enterprise:extensionField:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(extensionFieldService.deleteByIds(List.of(ids)));
    }
}
