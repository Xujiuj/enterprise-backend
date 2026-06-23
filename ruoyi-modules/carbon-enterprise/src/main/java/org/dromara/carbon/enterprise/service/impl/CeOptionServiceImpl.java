package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeActivityData;
import org.dromara.carbon.enterprise.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.domain.CeCompanyFactory;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.domain.CeFactorConfirmation;
import org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.domain.CeIntensityMetric;
import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.domain.vo.CeOptionVo;
import org.dromara.carbon.enterprise.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.mapper.CeCompanyFactoryMapper;
import org.dromara.carbon.enterprise.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorConfirmationMapper;
import org.dromara.carbon.enterprise.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.mapper.CeIntensityDenominatorFactMapper;
import org.dromara.carbon.enterprise.mapper.CeIntensityMetricMapper;
import org.dromara.carbon.enterprise.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.service.ICeOptionService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Enterprise option service implementation.
 *
 * <p>Options are whitelisted and derived from existing enterprise business
 * rows. This avoids sys_dict coupling and prevents arbitrary table/column
 * reads from the frontend.</p>
 */
@RequiredArgsConstructor
@Service
public class CeOptionServiceImpl implements ICeOptionService {

    private static final String DIMENSION_FIELD_OPTION = "dimension-field";

    private static final Set<String> ALLOWED_DIMENSION_CODES = Set.of(
        "admin-division",
        "company",
        "emission-source-category",
        "base-year",
        "ef-factor",
        "ef-electricity-factor",
        "ef-electricity-version",
        "ef-electricity-scope",
        "greenhouse-gas",
        "intensity-denominator",
        "intensity-target",
        "denominator-fact",
        "intensity-tolerance",
        "report-template-download"
    );

    private static final Set<String> ALLOWED_DIMENSION_FIELDS = Set.of(
        "recordCode",
        "recordName",
        "parentCode",
        "field01",
        "field02",
        "field03",
        "field04",
        "field05",
        "field06",
        "field07",
        "field08",
        "field09",
        "field10",
        "field11",
        "field12",
        "field13",
        "field14",
        "field15",
        "field16",
        "field17",
        "field18",
        "field19",
        "field20",
        "field21",
        "field22",
        "status"
    );

    private final CeActivityDataMapper activityDataMapper;
    private final CeCompanyFactoryMapper companyFactoryMapper;
    private final CeEmissionSourceMapper emissionSourceMapper;
    private final CeEmissionSourceCategoryMapper emissionSourceCategoryMapper;
    private final CeGreenPowerCertificateMapper greenPowerCertificateMapper;
    private final CeIntensityDenominatorFactMapper denominatorFactMapper;
    private final CeFactorCacheRecordMapper factorCacheRecordMapper;
    private final CeFactorConfirmationMapper factorConfirmationMapper;
    private final CeIntensityMetricMapper intensityMetricMapper;
    private final CeReportTemplateFileMapper reportTemplateFileMapper;
    private final CeCaptureBatchMapper captureBatchMapper;
    private final CeDimensionProjectionMapper dimensionProjectionMapper;

    @Override
    public List<CeOptionVo> listOptions(String optionCode, String dimensionCode, String field) {
        List<CeOptionVo> options = new ArrayList<>();
        switch (optionCode) {
            case "company-code" -> {
                collectDistinctWithLabel(options, companyFactoryMapper, CeCompanyFactory::getCompanyCode, CeCompanyFactory::getCompanyName);
                collectDistinctWithLabel(options, activityDataMapper, CeActivityData::getCompanyCode, CeActivityData::getCompanyName);
                collectDistinctWithLabel(options, emissionSourceMapper, CeEmissionSource::getCompanyCode, CeEmissionSource::getCompanyName);
            }
            case "company-name" -> {
                collectDistinct(options, companyFactoryMapper, CeCompanyFactory::getCompanyName, this::labelForRaw);
                collectDistinct(options, activityDataMapper, CeActivityData::getCompanyName, this::labelForRaw);
                collectDistinct(options, emissionSourceMapper, CeEmissionSource::getCompanyName, this::labelForRaw);
            }
            case "factory-code" -> {
                collectDistinctWithLabel(options, companyFactoryMapper, CeCompanyFactory::getFactoryCode, CeCompanyFactory::getFactoryName);
                collectDistinctWithLabel(options, activityDataMapper, CeActivityData::getCompanyCode, CeActivityData::getFactoryName);
                collectDistinctWithLabel(options, emissionSourceMapper, CeEmissionSource::getCompanyCode, CeEmissionSource::getFactoryName);
                collectDistinctWithLabel(
                    options,
                    greenPowerCertificateMapper,
                    CeGreenPowerCertificate::getFactoryCode,
                    CeGreenPowerCertificate::getFactoryName
                );
            }
            case "factory-name" -> {
                collectDistinct(options, companyFactoryMapper, CeCompanyFactory::getFactoryName, this::labelForRaw);
                collectDistinct(options, activityDataMapper, CeActivityData::getFactoryName, this::labelForRaw);
                collectDistinct(options, emissionSourceMapper, CeEmissionSource::getFactoryName, this::labelForRaw);
                collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getFactoryName, this::labelForRaw);
            }
            case "source-category-key" -> {
                collectSourceCategoryOptions(options);
                collectDistinct(options, activityDataMapper, CeActivityData::getSourceCategoryKey, this::labelForRaw);
                collectDistinct(options, emissionSourceMapper, CeEmissionSource::getSourceCategoryKey, this::labelForRaw);
                collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getSourceCategoryKey, this::labelForRaw);
            }
            case "responsible-dept" -> {
                collectDistinct(options, activityDataMapper, CeActivityData::getResponsibleDept, this::labelForRaw);
                collectDistinct(options, emissionSourceMapper, CeEmissionSource::getResponsibleDept, this::labelForRaw);
            }
            case "emission-source-code" -> collectDistinct(
                options,
                emissionSourceMapper,
                CeEmissionSource::getSourceIdentificationCode,
                this::labelForRaw
            );
            case "data-source" -> {
                collectDistinct(options, activityDataMapper, CeActivityData::getDataSource, this::labelForRaw);
                collectDistinct(options, emissionSourceMapper, CeEmissionSource::getDataSource, this::labelForRaw);
                collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getDataSource, this::labelForRaw);
                collectDistinct(options, denominatorFactMapper, CeIntensityDenominatorFact::getDataSource, this::labelForRaw);
            }
            case "activity-data-status" -> collectDistinct(options, activityDataMapper, CeActivityData::getDataStatus, this::labelForStatus);
            case "boolean-status" -> {
                collectDistinct(options, emissionSourceMapper, CeEmissionSource::getEnabledFlag, this::labelForBoolean);
                collectDistinct(options, factorCacheRecordMapper, CeFactorCacheRecord::getEnabledFlag, this::labelForBoolean);
                collectDistinct(options, reportTemplateFileMapper, CeReportTemplateFile::getEnabledFlag, this::labelForBoolean);
            }
            case "factor-table-code" -> collectDistinct(options, factorCacheRecordMapper, CeFactorCacheRecord::getFactorTableCode, this::labelForRaw);
            case "activity-year" -> {
                collectDistinct(options, activityDataMapper, CeActivityData::getActivityYear, this::labelForYear);
                collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getActivityYear, this::labelForYear);
                collectDistinct(options, denominatorFactMapper, CeIntensityDenominatorFact::getFactYear, this::labelForYear);
            }
            case "activity-month" -> {
                collectDistinct(options, activityDataMapper, CeActivityData::getActivityMonth, this::labelForMonth);
                collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getActivityMonth, this::labelForMonth);
                collectDistinct(options, denominatorFactMapper, CeIntensityDenominatorFact::getFactMonth, this::labelForMonth);
            }
            case "electricity-type" -> collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getElectricityType, this::labelForRaw);
            case "proof-status" -> collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getProofStatus, this::labelForStatus);
            case "intensity-metric-status" -> collectDistinct(options, intensityMetricMapper, CeIntensityMetric::getMetricStatus, this::labelForStatus);
            case "factor-confirmation-status" -> collectDistinct(options, factorConfirmationMapper, CeFactorConfirmation::getConfirmationStatus, this::labelForStatus);
            case "template-type" -> collectDistinct(options, reportTemplateFileMapper, CeReportTemplateFile::getTemplateType, this::labelForTemplateType);
            case "validation-status" -> collectDistinct(options, captureBatchMapper, CeCaptureBatch::getValidationStatus, this::labelForStatus);
            case "record-status" -> collectDimensionStatusOptions(options);
            case DIMENSION_FIELD_OPTION -> collectDimensionFieldOptions(options, dimensionCode, field);
            default -> throw new ServiceException("不支持的企业选项编码：" + optionCode);
        }
        return dedupeAndSort(options);
    }

    private <T> void collectDistinct(List<CeOptionVo> target, BaseMapper<T> mapper, SFunction<T, ?> column,
                                     Function<Object, String> labelResolver) {
        List<Object> values = mapper.selectObjs(new LambdaQueryWrapper<T>()
            .select(column)
            .isNotNull(column)
            .groupBy(column)
            .orderByAsc(column));
        for (Object value : values) {
            addOption(target, labelResolver.apply(value), value);
        }
    }

    private <T> void collectDistinctWithLabel(List<CeOptionVo> target, BaseMapper<T> mapper, SFunction<T, ?> valueColumn,
                                              SFunction<T, ?> labelColumn) {
        List<T> rows = mapper.selectList(new LambdaQueryWrapper<T>()
            .select(valueColumn, labelColumn)
            .isNotNull(valueColumn)
            .groupBy(valueColumn, labelColumn)
            .orderByAsc(valueColumn));
        for (T row : rows) {
            Object value = valueColumn.apply(row);
            String rawLabel = normalizeValue(labelColumn.apply(row));
            String rawValue = normalizeValue(value);
            String label = StringUtils.isBlank(rawLabel) || rawLabel.equals(rawValue)
                ? rawValue
                : rawValue + " / " + rawLabel;
            addOption(target, label, value);
        }
    }

    private void collectSourceCategoryOptions(List<CeOptionVo> target) {
        List<CeEmissionSourceCategory> rows = emissionSourceCategoryMapper.selectList(new LambdaQueryWrapper<CeEmissionSourceCategory>()
            .select(
                CeEmissionSourceCategory::getCategorySk,
                CeEmissionSourceCategory::getGhgScope,
                CeEmissionSourceCategory::getGhgScopeCategory
            )
            .isNotNull(CeEmissionSourceCategory::getCategorySk)
            .orderByAsc(CeEmissionSourceCategory::getCategorySk));
        for (CeEmissionSourceCategory row : rows) {
            String label = Stream.of(row.getGhgScope(), row.getGhgScopeCategory())
                .map(this::normalizeValue)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .reduce((left, right) -> left + " / " + right)
                .orElse(normalizeValue(row.getCategorySk()));
            addOption(target, label, row.getCategorySk());
        }
    }

    private void collectDimensionStatusOptions(List<CeOptionVo> target) {
        for (String dimensionCode : ALLOWED_DIMENSION_CODES) {
            for (CeDimensionRecordVo record : dimensionProjectionMapper.selectByDimensionCode(dimensionCode)) {
                addOption(target, labelForStatus(record.getStatus()), record.getStatus());
            }
        }
    }

    private void collectDimensionFieldOptions(List<CeOptionVo> target, String dimensionCode, String field) {
        validateDimensionOptionRequest(dimensionCode, field);
        for (CeDimensionRecordVo record : dimensionProjectionMapper.selectByDimensionCode(dimensionCode)) {
            Object value = dimensionValue(record, field);
            addOption(target, labelForStatus(value), value);
        }
    }

    private void validateDimensionOptionRequest(String dimensionCode, String field) {
        if (StringUtils.isBlank(dimensionCode) || !ALLOWED_DIMENSION_CODES.contains(dimensionCode)) {
            throw new ServiceException("不支持的维度编码：" + dimensionCode);
        }
        if (StringUtils.isBlank(field) || !ALLOWED_DIMENSION_FIELDS.contains(field)) {
            throw new ServiceException("不支持的维度字段：" + field);
        }
    }

    private Object dimensionValue(CeDimensionRecordVo record, String field) {
        return switch (field) {
            case "recordCode" -> record.getRecordCode();
            case "recordName" -> record.getRecordName();
            case "parentCode" -> record.getParentCode();
            case "field01" -> record.getField01();
            case "field02" -> record.getField02();
            case "field03" -> record.getField03();
            case "field04" -> record.getField04();
            case "field05" -> record.getField05();
            case "field06" -> record.getField06();
            case "field07" -> record.getField07();
            case "field08" -> record.getField08();
            case "field09" -> record.getField09();
            case "field10" -> record.getField10();
            case "field11" -> record.getField11();
            case "field12" -> record.getField12();
            case "field13" -> record.getField13();
            case "field14" -> record.getField14();
            case "field15" -> record.getField15();
            case "field16" -> record.getField16();
            case "field17" -> record.getField17();
            case "field18" -> record.getField18();
            case "field19" -> record.getField19();
            case "field20" -> record.getField20();
            case "field21" -> record.getField21();
            case "field22" -> record.getField22();
            case "status" -> record.getStatus();
            default -> null;
        };
    }

    private void addOption(List<CeOptionVo> target, String label, Object value) {
        String normalized = normalizeValue(value);
        if (StringUtils.isBlank(normalized)) {
            return;
        }
        target.add(new CeOptionVo(StringUtils.isBlank(label) ? normalized : label, value));
    }

    private List<CeOptionVo> dedupeAndSort(List<CeOptionVo> options) {
        Map<String, CeOptionVo> unique = new LinkedHashMap<>();
        for (CeOptionVo option : options) {
            String key = normalizeValue(option.getValue());
            if (StringUtils.isNotBlank(key)) {
                unique.putIfAbsent(key, option);
            }
        }
        return unique.values().stream()
            .sorted(Comparator.comparing(option -> normalizeValue(option.getValue()), this::compareOptionValue))
            .toList();
    }

    private int compareOptionValue(String left, String right) {
        Double leftNumber = parseNumber(left);
        Double rightNumber = parseNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber);
        }
        return left.compareToIgnoreCase(right);
    }

    private Double parseNumber(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String labelForBoolean(Object value) {
        if (Boolean.TRUE.equals(value)) {
            return "启用";
        }
        if (Boolean.FALSE.equals(value)) {
            return "停用";
        }
        return normalizeValue(value);
    }

    private String labelForRaw(Object value) {
        return normalizeValue(value);
    }

    private String labelForYear(Object value) {
        String text = normalizeValue(value);
        return StringUtils.isBlank(text) ? text : text + "年";
    }

    private String labelForMonth(Object value) {
        String text = normalizeValue(value);
        return StringUtils.isBlank(text) ? text : text + "月";
    }

    private String labelForTemplateType(Object value) {
        return switch (normalizeValue(value)) {
            case "power_bi" -> "Power BI";
            case "excel" -> "Excel";
            case "pdf" -> "PDF";
            default -> normalizeValue(value);
        };
    }

    private String labelForStatus(Object value) {
        return switch (normalizeValue(value)) {
            case "0" -> "启用";
            case "1" -> "停用";
            case "draft" -> "草稿";
            case "submitted" -> "已提交";
            case "locked" -> "已锁定";
            case "missing" -> "未提交";
            case "active" -> "生效";
            case "archived" -> "归档";
            case "pending" -> "待确认";
            case "confirmed" -> "已确认";
            case "rejected" -> "已退回";
            case "verified" -> "已核验";
            case "voided" -> "已作废";
            case "PASS" -> "通过";
            case "FAIL" -> "失败";
            case "WARN" -> "警告";
            case "Y", "是" -> "是";
            case "N", "否" -> "否";
            default -> normalizeValue(value);
        };
    }
}
