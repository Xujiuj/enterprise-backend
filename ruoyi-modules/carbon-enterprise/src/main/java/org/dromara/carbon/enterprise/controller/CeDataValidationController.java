package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.service.ICeActivityDataService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise activity-data validation dashboard API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/data-validation")
public class CeDataValidationController extends BaseController {

    private final ICeActivityDataService activityDataService;

    @SaCheckPermission("enterprise:dataValidation:view")
    @GetMapping("/dashboard")
    public R<CeActivityDataValidationDashboardVo> dashboard(CeActivityDataBo bo) {
        return R.ok(activityDataService.queryValidationDashboard(bo));
    }

    @SaCheckPermission("enterprise:dataValidation:view")
    @GetMapping("/summary")
    public R<CeActivityDataValidationDashboardVo> summary(CeActivityDataBo bo) {
        return R.ok(activityDataService.queryValidationDashboard(bo));
    }

    @SaCheckPermission("enterprise:dataValidation:view")
    @GetMapping("/submissions")
    public R<CeActivityDataValidationDashboardVo> submissions(CeActivityDataBo bo) {
        return R.ok(activityDataService.queryValidationDashboard(bo));
    }

    @SaCheckPermission("enterprise:dataValidation:view")
    @GetMapping("/issues")
    public R<CeActivityDataValidationDashboardVo> issues(CeActivityDataBo bo) {
        return R.ok(activityDataService.queryValidationDashboard(bo));
    }
}
