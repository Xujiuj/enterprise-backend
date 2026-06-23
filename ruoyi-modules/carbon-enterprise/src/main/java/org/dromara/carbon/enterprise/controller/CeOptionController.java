package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.vo.CeOptionVo;
import org.dromara.carbon.enterprise.service.ICeOptionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Enterprise dropdown options derived from existing enterprise business data.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/options")
public class CeOptionController extends BaseController {

    private final ICeOptionService optionService;

    @SaCheckPermission("enterprise:dimension:list")
    @GetMapping("/{optionCode}")
    public R<List<CeOptionVo>> list(
        @PathVariable String optionCode,
        @RequestParam(required = false) String dimensionCode,
        @RequestParam(required = false) String field
    ) {
        return R.ok(optionService.listOptions(optionCode, dimensionCode, field));
    }
}
