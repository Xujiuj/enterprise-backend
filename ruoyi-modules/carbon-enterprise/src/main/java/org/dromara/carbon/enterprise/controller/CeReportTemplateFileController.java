package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeReportTemplateFileBo;
import org.dromara.carbon.enterprise.domain.vo.CeReportTemplateFileVo;
import org.dromara.carbon.enterprise.service.ICeReportTemplateFileService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Enterprise local report template download catalog API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/report-template-file")
public class CeReportTemplateFileController extends BaseController {

    private final ICeReportTemplateFileService reportTemplateFileService;

    @SaCheckPermission("enterprise:reportTemplateFile:list")
    @GetMapping("/list")
    public TableDataInfo<CeReportTemplateFileVo> list(CeReportTemplateFileBo bo, PageQuery pageQuery) {
        return reportTemplateFileService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:reportTemplateFile:query")
    @GetMapping("/{id}")
    public R<CeReportTemplateFileVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(reportTemplateFileService.queryById(id));
    }

    @SaCheckPermission("enterprise:reportTemplateFile:download")
    @GetMapping("/download/{id}")
    public void download(@NotNull(message = "id cannot be null") @PathVariable Long id, HttpServletResponse response) throws IOException {
        reportTemplateFileService.download(id, response);
    }

    @SaCheckPermission("enterprise:reportTemplateFile:add")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeReportTemplateFileBo bo) {
        return toAjax(reportTemplateFileService.insertByBo(bo));
    }

    @SaCheckPermission("enterprise:reportTemplateFile:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeReportTemplateFileBo bo) {
        return toAjax(reportTemplateFileService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:reportTemplateFile:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(reportTemplateFileService.deleteByIds(List.of(ids)));
    }
}
