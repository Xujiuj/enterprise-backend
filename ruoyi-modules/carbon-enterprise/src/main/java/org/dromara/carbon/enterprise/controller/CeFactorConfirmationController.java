package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeFactorConfirmationBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorConfirmationVo;
import org.dromara.carbon.enterprise.service.ICeFactorConfirmationService;
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
 * Enterprise local emission factor confirmation API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/factor-confirmation")
public class CeFactorConfirmationController extends BaseController {

    private final ICeFactorConfirmationService factorConfirmationService;

    @SaCheckPermission("enterprise:factorConfirmation:list")
    @GetMapping("/list")
    public TableDataInfo<CeFactorConfirmationVo> list(CeFactorConfirmationBo bo, PageQuery pageQuery) {
        return factorConfirmationService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:factorConfirmation:query")
    @GetMapping("/{id}")
    public R<CeFactorConfirmationVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(factorConfirmationService.queryById(id));
    }

    @SaCheckPermission("enterprise:factorConfirmation:add")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeFactorConfirmationBo bo) {
        return toAjax(factorConfirmationService.insertByBo(bo));
    }

    @SaCheckPermission("enterprise:factorConfirmation:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeFactorConfirmationBo bo) {
        return toAjax(factorConfirmationService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:factorConfirmation:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(factorConfirmationService.deleteByIds(List.of(ids)));
    }
}
