package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeEmissionSourceBo;
import org.dromara.carbon.enterprise.domain.vo.CeEmissionSourceVo;
import org.dromara.carbon.enterprise.service.ICeEmissionSourceService;
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

import java.util.List;

/**
 * Enterprise local emission source API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/emission-source")
public class CeEmissionSourceController extends BaseController {

    private final ICeEmissionSourceService emissionSourceService;

    @SaCheckPermission("enterprise:emissionSource:list")
    @GetMapping("/list")
    public TableDataInfo<CeEmissionSourceVo> list(CeEmissionSourceBo bo, PageQuery pageQuery) {
        return emissionSourceService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:emissionSource:query")
    @GetMapping("/{id}")
    public R<CeEmissionSourceVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(emissionSourceService.queryById(id));
    }

    @SaCheckPermission("enterprise:emissionSource:add")
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CeEmissionSourceBo bo) {
        return toAjax(emissionSourceService.insertByBo(bo));
    }

    @SaCheckPermission("enterprise:emissionSource:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeEmissionSourceBo bo) {
        return toAjax(emissionSourceService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:emissionSource:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(emissionSourceService.deleteByIds(List.of(ids)));
    }
}
