package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.vo.CeWorkbenchOverviewVo;
import org.dromara.carbon.enterprise.service.ICeWorkbenchService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise home workbench API.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/workbench")
public class CeWorkbenchController extends BaseController {

    private final ICeWorkbenchService workbenchService;

    @SaCheckPermission("enterprise:workbench:overview")
    @GetMapping("/overview")
    public R<CeWorkbenchOverviewVo> overview() {
        return R.ok(workbenchService.overview());
    }
}
