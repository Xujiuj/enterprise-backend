package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeExtensionField;
import org.dromara.carbon.enterprise.domain.CeExtensionFieldValue;
import org.dromara.carbon.enterprise.domain.bo.CeExtensionFieldValueBo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldValueVo;
import org.dromara.carbon.enterprise.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.mapper.CeExtensionFieldMapper;
import org.dromara.carbon.enterprise.mapper.CeExtensionFieldValueMapper;
import org.dromara.carbon.enterprise.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.mapper.CeIntensityMetricMapper;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldValueService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Enterprise extension field value service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeExtensionFieldValueServiceImpl implements ICeExtensionFieldValueService {

    private static final Map<String, String> MODULE_OWNER_TABLES = Map.of(
        "activity_data", "ce_activity_data",
        "green_electricity", "ce_green_power_certificate",
        "intensity_denominator", "ce_intensity_metric"
    );

    private final CeExtensionFieldValueMapper extensionFieldValueMapper;
    private final CeExtensionFieldMapper extensionFieldMapper;
    private final CeActivityDataMapper activityDataMapper;
    private final CeGreenPowerCertificateMapper greenPowerCertificateMapper;
    private final CeIntensityMetricMapper intensityMetricMapper;

    @Override
    public TableDataInfo<CeExtensionFieldValueVo> queryPageList(CeExtensionFieldValueBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeExtensionFieldValue> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeExtensionFieldValue::getUpdateTime)
            .orderByDesc(CeExtensionFieldValue::getId);
        IPage<CeExtensionFieldValueVo> page = extensionFieldValueMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeExtensionFieldValueVo> queryList(CeExtensionFieldValueBo bo) {
        return extensionFieldValueMapper.selectVoList(buildQueryWrapper(bo)
            .orderByDesc(CeExtensionFieldValue::getUpdateTime)
            .orderByDesc(CeExtensionFieldValue::getId));
    }

    @Override
    public CeExtensionFieldValueVo queryById(Long id) {
        return extensionFieldValueMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeExtensionFieldValueBo bo) {
        validateExtensionFieldOwner(bo.getExtensionFieldId(), bo.getOwnerTableCode());
        validateOwnerRecordExists(bo.getOwnerTableCode(), bo.getOwnerRecordId());
        CeExtensionFieldValue add = toEntity(bo);
        boolean flag = extensionFieldValueMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeExtensionFieldValueBo bo) {
        validateExtensionFieldOwner(bo.getExtensionFieldId(), bo.getOwnerTableCode());
        validateOwnerRecordExists(bo.getOwnerTableCode(), bo.getOwnerRecordId());
        CeExtensionFieldValue update = toEntity(bo);
        return extensionFieldValueMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveBatch(List<CeExtensionFieldValueBo> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        for (CeExtensionFieldValueBo bo : values) {
            validateExtensionFieldOwner(bo.getExtensionFieldId(), bo.getOwnerTableCode());
            validateOwnerRecordExists(bo.getOwnerTableCode(), bo.getOwnerRecordId());
            CeExtensionFieldValue existing = selectExistingValue(bo);
            if (existing != null) {
                bo.setId(existing.getId());
                if (extensionFieldValueMapper.updateById(toEntity(bo)) <= 0) {
                    throw new ServiceException("extension field value batch update failed");
                }
            } else {
                CeExtensionFieldValue add = toEntity(bo);
                if (extensionFieldValueMapper.insert(add) <= 0) {
                    throw new ServiceException("extension field value batch insert failed");
                }
                bo.setId(add.getId());
            }
        }
        return true;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return extensionFieldValueMapper.deleteByIds(ids) > 0;
    }

    protected CeExtensionFieldValue toEntity(CeExtensionFieldValueBo bo) {
        return MapstructUtils.convert(bo, CeExtensionFieldValue.class);
    }

    private LambdaQueryWrapper<CeExtensionFieldValue> buildQueryWrapper(CeExtensionFieldValueBo bo) {
        return new LambdaQueryWrapper<CeExtensionFieldValue>()
            .eq(StringUtils.isNotBlank(bo.getOwnerTableCode()), CeExtensionFieldValue::getOwnerTableCode, bo.getOwnerTableCode())
            .eq(bo.getOwnerRecordId() != null, CeExtensionFieldValue::getOwnerRecordId, bo.getOwnerRecordId())
            .eq(bo.getExtensionFieldId() != null, CeExtensionFieldValue::getExtensionFieldId, bo.getExtensionFieldId());
    }

    private CeExtensionFieldValue selectExistingValue(CeExtensionFieldValueBo bo) {
        return extensionFieldValueMapper.selectOne(new LambdaQueryWrapper<CeExtensionFieldValue>()
            .eq(CeExtensionFieldValue::getOwnerTableCode, bo.getOwnerTableCode())
            .eq(CeExtensionFieldValue::getOwnerRecordId, bo.getOwnerRecordId())
            .eq(CeExtensionFieldValue::getExtensionFieldId, bo.getExtensionFieldId())
            .last("limit 1"));
    }

    private void validateExtensionFieldOwner(Long extensionFieldId, String ownerTableCode) {
        if (extensionFieldId == null) {
            throw new ServiceException("extension field id cannot be null");
        }
        if (StringUtils.isBlank(ownerTableCode)) {
            throw new ServiceException("extension field owner table cannot be blank");
        }
        CeExtensionField field = extensionFieldMapper.selectById(extensionFieldId);
        if (field == null) {
            throw new ServiceException("extension field does not exist");
        }
        if (Boolean.FALSE.equals(field.getEnabledFlag())) {
            throw new ServiceException("extension field is disabled");
        }
        String expectedOwnerTable = MODULE_OWNER_TABLES.get(field.getModuleCode());
        if (expectedOwnerTable == null) {
            throw new ServiceException("Unsupported enterprise extension module code: " + field.getModuleCode());
        }
        if (!expectedOwnerTable.equals(ownerTableCode)) {
            throw new ServiceException("extension field owner table does not match module code");
        }
    }

    private void validateOwnerRecordExists(String ownerTableCode, Long ownerRecordId) {
        if (ownerRecordId == null) {
            throw new ServiceException("extension field owner record id cannot be null");
        }
        Object owner = switch (ownerTableCode) {
            case "ce_activity_data" -> activityDataMapper.selectById(ownerRecordId);
            case "ce_green_power_certificate" -> greenPowerCertificateMapper.selectById(ownerRecordId);
            case "ce_intensity_metric" -> intensityMetricMapper.selectById(ownerRecordId);
            default -> throw new ServiceException("Unsupported enterprise extension owner table: " + ownerTableCode);
        };
        if (owner == null) {
            throw new ServiceException("extension field owner record does not exist");
        }
    }
}
