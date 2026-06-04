package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeCaptureRowBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureRowVo;
import org.dromara.carbon.enterprise.service.ICeCaptureRowService;
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
 * Enterprise local data capture row read API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/capture-row")
public class CeCaptureRowController extends BaseController {

    private final ICeCaptureRowService captureRowService;

    /**
     * List enterprise local data capture rows.
     */
    @SaCheckPermission("enterprise:captureRow:list")
    @GetMapping("/list")
    public TableDataInfo<CeCaptureRowVo> list(CeCaptureRowBo row, PageQuery pageQuery) {
        return captureRowService.selectPageRowList(row, pageQuery);
    }

    /**
     * Get enterprise local data capture row details.
     *
     * @param id row id
     */
    @SaCheckPermission("enterprise:captureRow:query")
    @GetMapping("/{id}")
    public R<CeCaptureRowVo> getInfo(@PathVariable Long id) {
        return R.ok(captureRowService.selectRowById(id));
    }
}
