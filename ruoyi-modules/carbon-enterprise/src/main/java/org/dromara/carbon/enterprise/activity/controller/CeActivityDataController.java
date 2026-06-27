package org.dromara.carbon.enterprise.activity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.activity.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.activity.domain.bo.CeActivityDataStatusBo;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataVo;
import org.dromara.carbon.enterprise.shared.service.ICeActivityDataService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.core.validate.EditGroup;
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
import java.util.Set;

/**
 * Enterprise local activity data API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/activity-data")
public class CeActivityDataController extends BaseController {

    private static final String RAW_WRITE_DISABLED_MESSAGE = "活动数据写入必须通过 emission_activity 校验录入或导入接口";
    private static final Set<String> ALLOWED_DATA_STATUSES = Set.of("draft", "submitted", "locked");

    private final ICeActivityDataService activityDataService;

    @SaCheckPermission("enterprise:activityData:list")
    @GetMapping("/list")
    public TableDataInfo<CeActivityDataVo> list(CeActivityDataBo bo, PageQuery pageQuery) {
        return activityDataService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:activityData:query")
    @GetMapping("/{id}")
    public R<CeActivityDataVo> getInfo(@NotNull(message = "ID不能为空") @PathVariable Long id) {
        return R.ok(activityDataService.queryById(id));
    }

    @SaCheckPermission("enterprise:activityDataRaw:add")
    @PostMapping
    public R<Void> add(@RequestBody CeActivityDataBo bo) {
        return R.fail(RAW_WRITE_DISABLED_MESSAGE);
    }

    @SaCheckPermission("enterprise:activityDataRaw:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeActivityDataBo bo) {
        return toAjax(activityDataService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:activityDataRaw:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "请选择要删除的数据") @PathVariable Long[] ids) {
        return toAjax(activityDataService.deleteByIds(List.of(ids)));
    }

    @SaCheckPermission("enterprise:activityDataRaw:edit")
    @PutMapping("/status")
    public R<Void> updateStatus(@Validated @RequestBody CeActivityDataStatusBo bo) {
        if (!ALLOWED_DATA_STATUSES.contains(bo.getDataStatus())) {
            return R.fail("不支持的数据状态");
        }
        return toAjax(activityDataService.updateStatusByIds(bo.getIds(), bo.getDataStatus()));
    }
}
