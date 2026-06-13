package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataVo;
import org.dromara.carbon.enterprise.service.ICeActivityDataService;
import org.dromara.common.core.domain.R;
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

/**
 * Enterprise local activity data API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/activity-data")
public class CeActivityDataController extends BaseController {

    private static final String RAW_WRITE_DISABLED_MESSAGE = "活动数据写入必须通过 sheet_656 校验录入或导入接口";

    private final ICeActivityDataService activityDataService;

    @SaCheckPermission("enterprise:activityData:list")
    @GetMapping("/list")
    public TableDataInfo<CeActivityDataVo> list(CeActivityDataBo bo, PageQuery pageQuery) {
        return activityDataService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:activityData:query")
    @GetMapping("/{id}")
    public R<CeActivityDataVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(activityDataService.queryById(id));
    }

    @SaCheckPermission("enterprise:activityDataRaw:add")
    @PostMapping
    public R<Void> add(@RequestBody CeActivityDataBo bo) {
        return R.fail(RAW_WRITE_DISABLED_MESSAGE);
    }

    @SaCheckPermission("enterprise:activityDataRaw:edit")
    @PutMapping
    public R<Void> edit(@RequestBody CeActivityDataBo bo) {
        return R.fail(RAW_WRITE_DISABLED_MESSAGE);
    }

    @SaCheckPermission("enterprise:activityDataRaw:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return R.fail(RAW_WRITE_DISABLED_MESSAGE);
    }
}
