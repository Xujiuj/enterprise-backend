package org.dromara.carbon.enterprise.shared.option.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.dimension.domain.CeCompanyFactory;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.factor.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.factor.domain.CeFactorConfirmation;
import org.dromara.carbon.enterprise.greenpower.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityMetric;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.report.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.shared.option.domain.bo.CeOptionQueryBo;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.shared.option.domain.vo.CeOptionVo;
import org.dromara.carbon.enterprise.activity.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeCompanyFactoryMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeFactorConfirmationMapper;
import org.dromara.carbon.enterprise.greenpower.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.intensity.mapper.CeIntensityDenominatorFactMapper;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.intensity.mapper.CeIntensityMetricMapper;
import org.dromara.carbon.enterprise.report.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.shared.service.ICeOptionService;
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
@Slf4j
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
        "intensity-tolerance"
    );

    private static final Set<String> ALLOWED_DIMENSION_FIELDS = Set.of(
        "recordCode",
        "recordName",
        "parentCode",
        "companySk",
        "factoryName",
        "provinceCode",
        "provinceName",
        "factoryType",
        "industrySectionCode",
        "industrySectionName",
        "industryDivisionCode",
        "industryDivisionName",
        "industryGroupCode",
        "industryGroupName",
        "industryClassCode",
        "industryClassName",
        "activeFlag",
        "ghgScope",
        "ghgScopeCategory",
        "currentBaseFlag",
        "factorVersion",
        "divisionCode",
        "divisionName",
        "scopeKey",
        "scopeName",
        "enabledText",
        "dataSource",
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
    private final CeLicenseStateMapper licenseStateMapper;
    private final CeDimensionProjectionMapper dimensionProjectionMapper;

    @Override
    public List<CeOptionVo> listOptions(String optionCode, CeOptionQueryBo query) {
        CeOptionQueryBo safeQuery = query == null ? new CeOptionQueryBo() : query;
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
                collectDistinctWithLabel(options, activityDataMapper, CeActivityData::getFactoryCode, CeActivityData::getFactoryName);
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
                collectSourceCategoryOptionsFromRows(
                    options,
                    activityDataMapper,
                    CeActivityData::getSourceCategoryKey,
                    CeActivityData::getScopeName,
                    CeActivityData::getScopeSubcategory
                );
                collectSourceCategoryOptionsFromRows(
                    options,
                    emissionSourceMapper,
                    CeEmissionSource::getSourceCategoryKey,
                    CeEmissionSource::getScopeName,
                    CeEmissionSource::getScopeSubcategory
                );
                collectSourceCategoryOptionsFromRows(
                    options,
                    greenPowerCertificateMapper,
                    CeGreenPowerCertificate::getSourceCategoryKey,
                    CeGreenPowerCertificate::getScopeName,
                    CeGreenPowerCertificate::getScopeSubcategory
                );
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
            case "intensity-rule-code" -> {
                collectDistinct(options, intensityMetricMapper, CeIntensityMetric::getRuleCode, this::labelForRaw);
                collectDistinct(options, denominatorFactMapper, CeIntensityDenominatorFact::getDenominatorMetricName, this::labelForRaw);
            }
            case "denominator-unit" -> {
                collectDistinct(options, intensityMetricMapper, CeIntensityMetric::getDenominatorUnit, this::labelForRaw);
                collectDistinct(options, denominatorFactMapper, CeIntensityDenominatorFact::getUnitName, this::labelForRaw);
            }
            case "intensity-target-code" -> collectIntensityTargetOptions(options);
            case "electricity-type" -> collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getElectricityType, this::labelForRaw);
            case "proof-status" -> collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getProofStatus, this::labelForStatus);
            case "intensity-metric-status" -> collectDistinct(options, intensityMetricMapper, CeIntensityMetric::getMetricStatus, this::labelForStatus);
            case "factor-confirmation-status" -> collectDistinct(options, factorConfirmationMapper, CeFactorConfirmation::getConfirmationStatus, this::labelForStatus);
            case "template-type" -> collectDistinct(options, reportTemplateFileMapper, CeReportTemplateFile::getTemplateType, this::labelForTemplateType);
            case "validation-status" -> collectDistinct(options, captureBatchMapper, CeCaptureBatch::getValidationStatus, this::labelForStatus);
            case "record-status" -> collectDimensionStatusOptions(options);
            case "power-grid-region" -> collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getPowerGridRegion, this::labelForRaw);
            case "offset-power-source" -> collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getOffsetPowerSource, this::labelForRaw);
            case "issuing-org" -> collectDistinct(options, greenPowerCertificateMapper, CeGreenPowerCertificate::getIssuingOrg, this::labelForRaw);
            case "confirmed-by" -> collectDistinct(options, factorConfirmationMapper, CeFactorConfirmation::getConfirmedBy, this::labelForRaw);
            case "license-id" -> collectDistinct(options, licenseStateMapper, CeLicenseState::getLicenseId, this::labelForRaw);
            case "activity-entry-emission-source-name" -> collectEmissionSourceFieldOptions(options, CeEmissionSource::getEmissionSourceName, safeQuery);
            case "activity-entry-source-company" -> collectEmissionSourceFieldOptions(options, CeEmissionSource::getCompanyName, safeQuery);
            case "activity-entry-source-factory" -> collectEmissionSourceFieldOptions(options, CeEmissionSource::getFactoryName, safeQuery);
            case "activity-entry-source-scope" -> collectEmissionSourceFieldOptions(options, CeEmissionSource::getScopeName, safeQuery);
            case "activity-entry-source-subcategory" -> collectEmissionSourceFieldOptions(options, CeEmissionSource::getScopeSubcategory, safeQuery);
            case "activity-entry-source-identification" -> collectEmissionSourceFieldOptions(options, CeEmissionSource::getSourceIdentificationName, safeQuery);
            case "activity-entry-source-category" -> collectSourceCategoryOptions(options);
            case "activity-entry-source-leaf" -> collectEmissionSourceLeafOptions(options, safeQuery);
            case DIMENSION_FIELD_OPTION -> collectDimensionFieldOptions(options, safeQuery.getDimensionCode(), safeQuery.getField());
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
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("scopeName", row.getGhgScope());
            record.put("scopeSubcategory", row.getGhgScopeCategory());
            addOption(target, label, row.getCategorySk(), record);
        }
    }

    private <T> void collectSourceCategoryOptionsFromRows(List<CeOptionVo> target, BaseMapper<T> mapper,
                                                          SFunction<T, ?> valueColumn,
                                                          SFunction<T, ?> scopeColumn,
                                                          SFunction<T, ?> subcategoryColumn) {
        List<T> rows = mapper.selectList(new LambdaQueryWrapper<T>()
            .select(valueColumn, scopeColumn, subcategoryColumn)
            .isNotNull(valueColumn)
            .groupBy(valueColumn, scopeColumn, subcategoryColumn)
            .orderByAsc(valueColumn));
        for (T row : rows) {
            Object value = valueColumn.apply(row);
            String label = Stream.of(scopeColumn.apply(row), subcategoryColumn.apply(row))
                .map(this::normalizeValue)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .reduce((left, right) -> left + " / " + right)
                .orElse(normalizeValue(value));
            addOption(target, label, value);
        }
    }

    private void collectDimensionStatusOptions(List<CeOptionVo> target) {
        for (String dimensionCode : ALLOWED_DIMENSION_CODES) {
            for (CeDimensionRecordVo record : dimensionProjectionMapper.selectByDimensionCode(dimensionCode)) {
                addOption(target, labelForStatus(record.getStatus()), record.getStatus());
            }
        }
    }

    private void collectIntensityTargetOptions(List<CeOptionVo> target) {
        for (CeDimensionRecordVo record : dimensionProjectionMapper.selectByDimensionCode("intensity-target")) {
            String factoryType = normalizeValue(record.getRecordCode());
            String targetYear = normalizeValue(record.getRecordName());
            if (StringUtils.isBlank(factoryType) || StringUtils.isBlank(targetYear)) {
                continue;
            }
            String value = factoryType + ":" + targetYear;
            String label = Stream.of(factoryType, targetYear, record.getTargetValue(), record.getUnitName())
                .map(this::normalizeValue)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .reduce((left, right) -> left + " / " + right)
                .orElse(value);
            addOption(target, label, value);
        }
    }

    private void collectEmissionSourceFieldOptions(List<CeOptionVo> target, SFunction<CeEmissionSource, ?> valueColumn,
                                                   CeOptionQueryBo query) {
        for (CeEmissionSource row : selectEnabledEmissionSources(query)) {
            addOption(target, labelForRaw(valueColumn.apply(row)), valueColumn.apply(row));
        }
    }

    private void collectEmissionSourceNameOptions(List<CeOptionVo> target, CeOptionQueryBo query) {
        Map<String, CeOptionVo> optionsByLabel = new LinkedHashMap<>();
        List<CeEmissionSource> rows = selectEnabledEmissionSources(query);
        Map<String, String> factorNameCache = loadFactorDisplayNameCache(rows);
        for (CeEmissionSource row : rows) {
            String label = emissionSourceLabel(row);
            if (StringUtils.isBlank(label)) {
                continue;
            }
            optionsByLabel.putIfAbsent(label, new CeOptionVo(label, label, emissionSourceRecord(row, factorNameCache)));
        }
        target.addAll(optionsByLabel.values());
    }

    private void collectEmissionSourceLeafOptions(List<CeOptionVo> target, CeOptionQueryBo query) {
        Map<String, CeOptionVo> optionsByCode = new LinkedHashMap<>();
        List<CeEmissionSource> rows = selectEnabledEmissionSources(query);
        log.info("[Leaf选项] 查询到 {} 条排放源记录, 查询条件: company={}, factory={}, category={}",
            rows.size(), query.getCompanyName(), query.getFactoryName(), query.getSourceCategoryKey());
        Map<String, String> factorNameCache = loadFactorDisplayNameCache(rows);
        boolean firstRow = true;
        for (CeEmissionSource row : rows) {
            String value = normalizeValue(row.getSourceIdentificationCode());
            String label = emissionSourceLabel(row);
            if (StringUtils.isBlank(value) || StringUtils.isBlank(label)) {
                continue;
            }
            Map<String, Object> record = emissionSourceRecord(row, factorNameCache);
            if (firstRow) {
                log.info("[Leaf选项] 首条记录: code={}, name={}, scope={}, unit={}, factor={}, factorDisplay={}",
                    value, row.getEmissionSourceName(), row.getScopeName(),
                    row.getSourceUnit(), row.getFactorKey(), record.get("factorDisplayName"));
                firstRow = false;
            }
            CeOptionVo candidate = new CeOptionVo(label, value, record);
            CeOptionVo existing = optionsByCode.get(value);
            if (existing == null || compareOptionValue(value, normalizeValue(existing.getValue())) < 0) {
                optionsByCode.put(value, candidate);
            }
        }
        target.addAll(optionsByCode.values());
    }

    private List<CeEmissionSource> selectEnabledEmissionSources(CeOptionQueryBo query) {
        LambdaQueryWrapper<CeEmissionSource> wrapper = new LambdaQueryWrapper<CeEmissionSource>()
            .eq(CeEmissionSource::getEnabledFlag, Boolean.TRUE)
            .orderByAsc(CeEmissionSource::getCompanyName)
            .orderByAsc(CeEmissionSource::getFactoryName)
            .orderByAsc(CeEmissionSource::getSourceCategoryKey)
            .orderByAsc(CeEmissionSource::getSourceIdentificationCode);
        applyEmissionSourceFilter(wrapper, CeEmissionSource::getCompanyName, query.getCompanyName());
        applyEmissionSourceFilter(wrapper, CeEmissionSource::getFactoryName, query.getFactoryName());
        applyEmissionSourceFilter(wrapper, CeEmissionSource::getSourceCategoryKey, query.getSourceCategoryKey());
        applyEmissionSourceFilter(wrapper, CeEmissionSource::getScopeName, query.getScopeName());
        applyEmissionSourceFilter(wrapper, CeEmissionSource::getScopeSubcategory, query.getScopeSubcategory());
        applyEmissionSourceFilter(wrapper, CeEmissionSource::getSourceIdentificationName, query.getSourceIdentificationName());
        applyEmissionSourceFilter(wrapper, CeEmissionSource::getEmissionSourceName, query.getEmissionSourceName());
        return emissionSourceMapper.selectList(wrapper);
    }

    private void applyEmissionSourceFilter(LambdaQueryWrapper<CeEmissionSource> wrapper, SFunction<CeEmissionSource, ?> column,
                                           String value) {
        String normalized = normalizeValue(value);
        if (StringUtils.isNotBlank(normalized)) {
            wrapper.eq(column, normalized);
        }
    }

    private String emissionSourceLabel(CeEmissionSource source) {
        return Stream.of(source.getEmissionSourceName(), source.getSourceIdentificationName(), source.getSourceIdentificationCode())
            .map(this::normalizeValue)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse("");
    }

    private Map<String, Object> emissionSourceRecord(CeEmissionSource source, Map<String, String> factorNameCache) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", source.getId());
        record.put("companyCode", source.getCompanyCode());
        record.put("companyName", source.getCompanyName());
        record.put("factoryName", source.getFactoryName());
        record.put("sourceCategoryKey", source.getSourceCategoryKey());
        record.put("scopeName", source.getScopeName());
        record.put("scopeSubcategory", source.getScopeSubcategory());
        record.put("sourceIdentificationCode", source.getSourceIdentificationCode());
        record.put("sourceIdentificationName", source.getSourceIdentificationName());
        record.put("emissionSourceName", source.getEmissionSourceName());
        record.put("responsibleDept", source.getResponsibleDept());
        record.put("dataSource", source.getDataSource());
        record.put("factorKey", source.getFactorKey());
        record.put("factorDisplayName", factorNameCache.getOrDefault(normalizeValue(source.getFactorKey()), source.getFactorKey()));
        record.put("sourceUnit", source.getSourceUnit());
        record.put("enabledFlag", source.getEnabledFlag());
        return record;
    }

    /**
     * 批量加载因子显示名称缓存。一次查询所有因子缓存记录，避免 N+1 问题。
     *
     * @param sources 排放源列表（用于提取 factorKey 集合）
     * @return factorKey → 显示名称的映射
     */
    private Map<String, String> loadFactorDisplayNameCache(List<CeEmissionSource> sources) {
        Set<String> factorKeys = sources.stream()
            .map(CeEmissionSource::getFactorKey)
            .map(this::normalizeValue)
            .filter(StringUtils::isNotBlank)
            .collect(java.util.stream.Collectors.toSet());
        if (factorKeys.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Map<String, String> cache = new LinkedHashMap<>();
        for (CeFactorCacheRecord rec : factorCacheRecordMapper.selectList(
            new LambdaQueryWrapper<CeFactorCacheRecord>()
                .select(CeFactorCacheRecord::getFactorKey, CeFactorCacheRecord::getFactorName,
                    CeFactorCacheRecord::getEmissionSourceName, CeFactorCacheRecord::getFactorUnit)
                .in(CeFactorCacheRecord::getFactorKey, factorKeys))) {
            String key = normalizeValue(rec.getFactorKey());
            if (StringUtils.isBlank(key)) continue;
            String name = Stream.of(rec.getEmissionSourceName(), rec.getFactorName())
                .map(this::normalizeValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(key);
            String unit = normalizeValue(rec.getFactorUnit());
            cache.put(key, StringUtils.isNotBlank(unit) ? name + " (" + unit + ")" : name);
        }
        // 特殊处理
        cache.putIfAbsent("电力因子表", "电力因子表");
        return cache;
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
            case "companySk" -> record.getCompanySk();
            case "factoryName" -> record.getFactoryName();
            case "provinceCode" -> record.getProvinceCode();
            case "provinceName" -> record.getProvinceName();
            case "factoryType" -> record.getFactoryType();
            case "industrySectionCode" -> record.getIndustrySectionCode();
            case "industrySectionName" -> record.getIndustrySectionName();
            case "industryDivisionCode" -> record.getIndustryDivisionCode();
            case "industryDivisionName" -> record.getIndustryDivisionName();
            case "industryGroupCode" -> record.getIndustryGroupCode();
            case "industryGroupName" -> record.getIndustryGroupName();
            case "industryClassCode" -> record.getIndustryClassCode();
            case "industryClassName" -> record.getIndustryClassName();
            case "activeFlag" -> record.getActiveFlag();
            case "ghgScope" -> record.getGhgScope();
            case "ghgScopeCategory" -> record.getGhgScopeCategory();
            case "currentBaseFlag" -> record.getCurrentBaseFlag();
            case "factorVersion" -> record.getFactorVersion();
            case "divisionCode" -> record.getDivisionCode();
            case "divisionName" -> record.getDivisionName();
            case "scopeKey" -> record.getScopeKey();
            case "scopeName" -> record.getScopeName();
            case "enabledText" -> record.getEnabledText();
            case "dataSource" -> record.getDataSource();
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

    private void addOption(List<CeOptionVo> target, String label, Object value, Map<String, Object> record) {
        String normalized = normalizeValue(value);
        if (StringUtils.isBlank(normalized)) {
            return;
        }
        target.add(new CeOptionVo(StringUtils.isBlank(label) ? normalized : label, value, record));
    }

    private List<CeOptionVo> dedupeAndSort(List<CeOptionVo> options) {
        Map<String, CeOptionVo> unique = new LinkedHashMap<>();
        for (CeOptionVo option : options) {
            String key = normalizeValue(option.getValue());
            if (StringUtils.isNotBlank(key)) {
                CeOptionVo existing = unique.get(key);
                if (existing == null || isMoreReadableLabel(option, existing)) {
                    unique.put(key, option);
                }
            }
        }
        return unique.values().stream()
            .sorted(Comparator.comparing(option -> normalizeValue(option.getValue()), this::compareOptionValue))
            .toList();
    }

    private boolean isMoreReadableLabel(CeOptionVo candidate, CeOptionVo existing) {
        String value = normalizeValue(candidate.getValue());
        String candidateLabel = normalizeValue(candidate.getLabel());
        String existingLabel = normalizeValue(existing.getLabel());
        return existingLabel.equals(value) && StringUtils.isNotBlank(candidateLabel) && !candidateLabel.equals(value);
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
