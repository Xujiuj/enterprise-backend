package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeLicenseStateBo;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise local license runtime state API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/license-state")
public class CeLicenseStateController extends BaseController {

    private final ICeLicenseStateService licenseStateService;

    /**
     * List enterprise license states.
     */
    @SaCheckPermission("enterprise:licenseState:list")
    @GetMapping("/list")
    public TableDataInfo<CeLicenseStateVo> list(CeLicenseStateBo bo, PageQuery pageQuery) {
        return licenseStateService.queryPageList(bo, pageQuery);
    }

    /**
     * Get latest enterprise license state.
     */
    @SaCheckPermission("enterprise:licenseState:query")
    @GetMapping("/current")
    public R<CeLicenseStateVo> current() {
        return R.ok(licenseStateService.queryCurrent());
    }

    /**
     * Get enterprise license state details.
     *
     * @param id license state id
     */
    @SaCheckPermission("enterprise:licenseState:query")
    @GetMapping("/{id}")
    public R<CeLicenseStateVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(licenseStateService.queryById(id));
    }
}
