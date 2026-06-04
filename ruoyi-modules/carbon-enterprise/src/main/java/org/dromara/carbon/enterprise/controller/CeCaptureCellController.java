package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeCaptureCellBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureCellVo;
import org.dromara.carbon.enterprise.service.ICeCaptureCellService;
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
 * Enterprise local data capture cell read API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/capture-cell")
public class CeCaptureCellController extends BaseController {

    private final ICeCaptureCellService captureCellService;

    /**
     * List enterprise local data capture cells.
     */
    @SaCheckPermission("enterprise:captureCell:list")
    @GetMapping("/list")
    public TableDataInfo<CeCaptureCellVo> list(CeCaptureCellBo cell, PageQuery pageQuery) {
        return captureCellService.selectPageCellList(cell, pageQuery);
    }

    /**
     * Get enterprise local data capture cell details.
     *
     * @param id cell id
     */
    @SaCheckPermission("enterprise:captureCell:query")
    @GetMapping("/{id}")
    public R<CeCaptureCellVo> getInfo(@PathVariable Long id) {
        return R.ok(captureCellService.selectCellById(id));
    }
}
