package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeFactorCacheRecordBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheRecordVo;
import org.dromara.carbon.enterprise.service.ICeFactorCacheRecordService;
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
 * Enterprise local factor cache record API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/factor-cache-record")
public class CeFactorCacheRecordController extends BaseController {

    private final ICeFactorCacheRecordService factorCacheRecordService;

    @SaCheckPermission("enterprise:factorCacheRecord:list")
    @GetMapping("/list")
    public TableDataInfo<CeFactorCacheRecordVo> list(CeFactorCacheRecordBo bo, PageQuery pageQuery) {
        return factorCacheRecordService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:factorCacheRecord:query")
    @GetMapping("/{id}")
    public R<CeFactorCacheRecordVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(factorCacheRecordService.queryById(id));
    }

    @SaCheckPermission("enterprise:factorCacheRecord:add")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeFactorCacheRecordBo bo) {
        return toAjax(factorCacheRecordService.insertByBo(bo));
    }

    @SaCheckPermission("enterprise:factorCacheRecord:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeFactorCacheRecordBo bo) {
        return toAjax(factorCacheRecordService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:factorCacheRecord:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(factorCacheRecordService.deleteByIds(List.of(ids)));
    }
}
