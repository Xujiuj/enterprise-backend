package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.vo.CeReportContentVo;
import org.dromara.carbon.enterprise.service.ICeReportContentService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Enterprise local report content catalog endpoints.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/report-content")
public class CeReportContentController extends BaseController {

    private final ICeReportContentService reportContentService;

    @SaCheckPermission("enterprise:reports:view")
    @GetMapping("/list")
    public R<List<CeReportContentVo>> list() {
        return R.ok(reportContentService.listContent());
    }
}
