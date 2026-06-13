package org.dromara.carbon.enterprise.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.bo.CeGreenPowerCertificateBo;
import org.dromara.carbon.enterprise.domain.vo.CeGreenPowerCertificateVo;
import org.dromara.carbon.enterprise.service.ICeGreenPowerCertificateService;
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
 * Enterprise local green electricity and certificate proof API.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/enterprise/green-power-certificate")
public class CeGreenPowerCertificateController extends BaseController {

    private final ICeGreenPowerCertificateService greenPowerCertificateService;

    @SaCheckPermission("enterprise:greenPowerCertificate:list")
    @GetMapping("/list")
    public TableDataInfo<CeGreenPowerCertificateVo> list(CeGreenPowerCertificateBo bo, PageQuery pageQuery) {
        return greenPowerCertificateService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("enterprise:greenPowerCertificate:query")
    @GetMapping("/{id}")
    public R<CeGreenPowerCertificateVo> getInfo(@NotNull(message = "id cannot be null") @PathVariable Long id) {
        return R.ok(greenPowerCertificateService.queryById(id));
    }

    @SaCheckPermission("enterprise:greenPowerCertificate:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody CeGreenPowerCertificateBo bo) {
        return greenPowerCertificateService.insertByBo(bo)
            ? R.ok(bo.getId())
            : R.fail("新增绿电绿证数据失败：未写入任何数据，请确认单据编码是否重复或录入内容是否有效");
    }

    @SaCheckPermission("enterprise:greenPowerCertificate:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CeGreenPowerCertificateBo bo) {
        return toAjax(greenPowerCertificateService.updateByBo(bo));
    }

    @SaCheckPermission("enterprise:greenPowerCertificate:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "ids cannot be empty") @PathVariable Long[] ids) {
        return toAjax(greenPowerCertificateService.deleteByIds(List.of(ids)));
    }
}
