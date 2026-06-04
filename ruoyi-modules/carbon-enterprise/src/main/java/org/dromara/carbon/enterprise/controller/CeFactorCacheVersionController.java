package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeFactorCacheVersionBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheVersionVo;
import org.dromara.carbon.enterprise.service.ICeFactorCacheVersionService;
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
 * Enterprise local factor cache version API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/factor-cache-version")
public class CeFactorCacheVersionController extends BaseController {

    private final ICeFactorCacheVersionService factorCacheVersionService;

    /**
     * List enterprise factor cache versions.
     */
    @SaCheckPermission("enterprise:factorCacheVersion:list")
    @GetMapping("/list")
    public TableDataInfo<CeFactorCacheVersionVo> list(CeFactorCacheVersionBo bo, PageQuery pageQuery) {
        return factorCacheVersionService.queryPageList(bo, pageQuery);
    }

    /**
     * Get enterprise factor cache version details.
     *
     * @param id factor cache version id
     */
    @SaCheckPermission("enterprise:factorCacheVersion:query")
    @GetMapping("/{id}")
    public R<CeFactorCacheVersionVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(factorCacheVersionService.queryById(id));
    }
}
