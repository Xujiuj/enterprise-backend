package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.sync.CeDimensionSyncResponse;
import org.dromara.carbon.enterprise.domain.sync.CeDimensionSyncStatus;
import org.dromara.carbon.enterprise.service.ICeDimensionSyncService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业端维度同步API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/dimension-sync")
public class CeDimensionSyncController {

    private final ICeDimensionSyncService dimensionSyncService;

    /**
     * 同步指定维度.
     */
    @SaCheckPermission("enterprise:dimensionSync:refresh")
    @PostMapping("/refresh")
    public R<CeDimensionSyncResponse> refresh(@RequestParam String dimensionCode) {
        return R.ok(dimensionSyncService.syncDimension(dimensionCode));
    }

    /**
     * 同步全部厂商维度.
     */
    @SaCheckPermission("enterprise:dimensionSync:refresh")
    @PostMapping("/refresh-all")
    public R<List<CeDimensionSyncResponse>> refreshAll() {
        return R.ok(dimensionSyncService.syncAllVendorDimensions());
    }

    /**
     * 查询最近一次同步状态.
     */
    @SaCheckPermission("enterprise:dimensionSync:status")
    @GetMapping("/status")
    public R<CeDimensionSyncStatus> status() {
        return R.ok(dimensionSyncService.getLastSyncStatus());
    }
}
