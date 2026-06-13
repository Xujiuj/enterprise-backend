package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeIntensityMetricBo;
import org.dromara.carbon.enterprise.domain.vo.CeIntensityMetricVo;
import org.dromara.carbon.enterprise.service.ICeIntensityMetricService;
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
 * Enterprise local carbon intensity metric API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/intensity-metric")
public class CeIntensityMetricController extends BaseController {

    private final ICeIntensityMetricService intensityMetricService;

    @SaCheckPermission("enterprise:intensityMetric:list")
    @GetMapping("/list")
    public TableDataInfo<CeIntensityMetricVo> list(CeIntensityMetricBo bo, PageQuery pageQuery) {
        return intensityMetricService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:intensityMetric:query")
    @GetMapping("/{id}")
    public R<CeIntensityMetricVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(intensityMetricService.queryById(id));
    }

    @SaCheckPermission("enterprise:intensityMetric:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody CeIntensityMetricBo bo) {
        return intensityMetricService.insertByBo(bo)
            ? R.ok(bo.getId())
            : R.fail("新增强度指标数据失败：未写入任何数据，请确认指标编码是否重复或录入内容是否有效");
    }

    @SaCheckPermission("enterprise:intensityMetric:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeIntensityMetricBo bo) {
        return toAjax(intensityMetricService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:intensityMetric:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(intensityMetricService.deleteByIds(List.of(ids)));
    }
}
