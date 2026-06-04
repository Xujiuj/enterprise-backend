package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeCaptureBatchBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureBatchVo;
import org.dromara.carbon.enterprise.service.ICeCaptureBatchService;
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
 * Enterprise local data capture batch read API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/capture-batch")
public class CeCaptureBatchController extends BaseController {

    private final ICeCaptureBatchService captureBatchService;

    /**
     * List enterprise local data capture batches.
     */
    @SaCheckPermission("enterprise:captureBatch:list")
    @GetMapping("/list")
    public TableDataInfo<CeCaptureBatchVo> list(CeCaptureBatchBo batch, PageQuery pageQuery) {
        return captureBatchService.selectPageBatchList(batch, pageQuery);
    }

    /**
     * Get enterprise local data capture batch details.
     *
     * @param id batch id
     */
    @SaCheckPermission("enterprise:captureBatch:query")
    @GetMapping("/{id}")
    public R<CeCaptureBatchVo> getInfo(@PathVariable Long id) {
        return R.ok(captureBatchService.selectBatchById(id));
    }
}
