package org.dromara.carbon.enterprise.extension.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.extension.domain.CeExtensionField;
import org.dromara.carbon.enterprise.extension.domain.bo.CeExtensionFieldBo;
import org.dromara.carbon.enterprise.extension.domain.vo.CeExtensionFieldVo;
import org.dromara.carbon.enterprise.extension.mapper.CeExtensionFieldMapper;
import org.dromara.carbon.enterprise.extension.support.CeExtensionModuleRegistry;
import org.dromara.carbon.enterprise.shared.service.ICeExtensionFieldService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Enterprise allowed extension fields service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeExtensionFieldServiceImpl implements ICeExtensionFieldService {

    private static final String FIELD_CODE_PATTERN = "^[A-Za-z][A-Za-z0-9_]{1,63}$";

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
        validateExtensionFieldDefinition(bo);
        CeExtensionField add = toEntity(bo);
        add.setFieldCode(normalizeFieldCode(bo.getFieldCode()));
        if (add.getValueType() == null) {
            add.setValueType("text");
        }
        if (add.getEnabledFlag() == null) {
            add.setEnabledFlag(true);
        }
        boolean flag = extensionFieldMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeExtensionFieldBo bo) {
        validateExtensionFieldDefinition(bo);
        CeExtensionField update = toEntity(bo);
        update.setFieldCode(normalizeFieldCode(bo.getFieldCode()));
        if (update.getValueType() == null) {
            update.setValueType("text");
        }
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

    protected CeExtensionField toEntity(CeExtensionFieldBo bo) {
        return MapstructUtils.convert(bo, CeExtensionField.class);
    }

    private void validateExtensionFieldDefinition(CeExtensionFieldBo bo) {
        CeExtensionModuleRegistry.ModuleDefinition module = CeExtensionModuleRegistry.require(bo.getModuleCode());
        CeExtensionModuleRegistry.validateValueType(bo.getValueType());
        String fieldCode = normalizeFieldCode(bo.getFieldCode());
        if (!fieldCode.matches(FIELD_CODE_PATTERN)) {
            throw new ServiceException("extension field code must start with a letter and contain only letters, numbers, and underscores");
        }
        if (module.isReservedField(fieldCode)) {
            throw new ServiceException("extension field code cannot override original field: " + fieldCode);
        }
        CeExtensionField existing = extensionFieldMapper.selectOne(new LambdaQueryWrapper<CeExtensionField>()
            .eq(CeExtensionField::getModuleCode, bo.getModuleCode())
            .eq(CeExtensionField::getFieldCode, fieldCode)
            .ne(bo.getId() != null, CeExtensionField::getId, bo.getId()), false);
        if (existing != null) {
            throw new ServiceException("extension field code already exists in module: " + fieldCode);
        }
    }

    private String normalizeFieldCode(String fieldCode) {
        return StringUtils.trimToEmpty(fieldCode).toLowerCase(Locale.ROOT);
    }
}
