package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.service.ICeDimensionRecordService;
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
 * Enterprise dimension record API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/dimension-record")
public class CeDimensionRecordController extends BaseController {

    private final ICeDimensionRecordService dimensionRecordService;

    /**
     * List enterprise dimension records.
     */
    @SaCheckPermission("enterprise:dimension:list")
    @GetMapping("/list")
    public TableDataInfo<CeDimensionRecordVo> list(CeDimensionRecordBo bo, PageQuery pageQuery) {
        return dimensionRecordService.queryPageList(bo, pageQuery);
    }

    /**
     * Get enterprise dimension record details.
     */
    @SaCheckPermission("enterprise:dimension:query")
    @GetMapping("/{id}")
    public R<CeDimensionRecordVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(dimensionRecordService.queryById(id));
    }

    /**
     * Add enterprise dimension record.
     */
    @SaCheckPermission("enterprise:dimension:add")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeDimensionRecordBo bo) {
        return toAjax(dimensionRecordService.insertByBo(bo));
    }

    /**
     * Edit enterprise dimension record.
     */
    @SaCheckPermission("enterprise:dimension:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeDimensionRecordBo bo) {
        return toAjax(dimensionRecordService.updateByBo(bo));
    }

    /**
     * Delete enterprise dimension records.
     */
    @SaCheckPermission("enterprise:dimension:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(dimensionRecordService.deleteByIds(List.of(ids)));
    }
}
