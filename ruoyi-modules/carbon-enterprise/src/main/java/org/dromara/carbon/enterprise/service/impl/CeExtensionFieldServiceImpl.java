package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeExtensionField;
import org.dromara.carbon.enterprise.domain.bo.CeExtensionFieldBo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldVo;
import org.dromara.carbon.enterprise.mapper.CeExtensionFieldMapper;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Enterprise allowed extension fields service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeExtensionFieldServiceImpl implements ICeExtensionFieldService {

    private static final Set<String> ALLOWED_MODULE_CODES = Set.of(
        "activity_data",
        "green_electricity",
        "intensity_denominator"
    );

    private final CeExtensionFieldMapper extensionFieldMapper;

    @Override
    public TableDataInfo<CeExtensionFieldVo> queryPageList(CeExtensionFieldBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeExtensionField> wrapper = buildQueryWrapper(bo)
            .orderByAsc(CeExtensionField::getTemplateVersionId)
            .orderByAsc(CeExtensionField::getSheetId)
            .orderByAsc(CeExtensionField::getId);
        IPage<CeExtensionFieldVo> page = extensionFieldMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeExtensionFieldVo> queryList(CeExtensionFieldBo bo) {
        return extensionFieldMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeExtensionField::getTemplateVersionId)
            .orderByAsc(CeExtensionField::getSheetId)
            .orderByAsc(CeExtensionField::getId));
    }

    @Override
    public CeExtensionFieldVo queryById(Long id) {
        return extensionFieldMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeExtensionFieldBo bo) {
        validateModuleCode(bo.getModuleCode());
        CeExtensionField add = MapstructUtils.convert(bo, CeExtensionField.class);
        boolean flag = extensionFieldMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeExtensionFieldBo bo) {
        validateModuleCode(bo.getModuleCode());
        CeExtensionField update = MapstructUtils.convert(bo, CeExtensionField.class);
        return extensionFieldMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return extensionFieldMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeExtensionField> buildQueryWrapper(CeExtensionFieldBo bo) {
        return new LambdaQueryWrapper<CeExtensionField>()
            .eq(bo.getTemplateVersionId() != null, CeExtensionField::getTemplateVersionId, bo.getTemplateVersionId())
            .eq(StringUtils.isNotBlank(bo.getModuleCode()), CeExtensionField::getModuleCode, bo.getModuleCode())
            .eq(bo.getSheetId() != null, CeExtensionField::getSheetId, bo.getSheetId())
            .eq(StringUtils.isNotBlank(bo.getFieldCode()), CeExtensionField::getFieldCode, bo.getFieldCode())
            .eq(bo.getEnabledFlag() != null, CeExtensionField::getEnabledFlag, bo.getEnabledFlag());
    }

    private void validateModuleCode(String moduleCode) {
        if (!ALLOWED_MODULE_CODES.contains(moduleCode)) {
            throw new ServiceException("Unsupported enterprise extension module code: " + moduleCode);
        }
    }
}
