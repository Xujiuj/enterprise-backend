package org.dromara.carbon.enterprise.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.template.domain.CeTemplateField;
import org.dromara.carbon.enterprise.template.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityCaptureResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationResult;
import org.dromara.carbon.enterprise.activity.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityCaptureService;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityImportValidationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Persists validated emission_activity activity rows into enterprise-local ce_* capture tables.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class CeEmissionActivityCaptureServiceImpl implements ICeEmissionActivityCaptureService {

    private static final String TARGET_TABLE_CODE = "emission_activity";
    private static final String SOURCE_MODE_MANUAL = "manual";
    private static final String SOURCE_MODE_EXCEL_IMPORT = "excel_import";
    private static final String BATCH_STATUS_COMPLETED = "completed";
    private static final String VALIDATION_STATUS_VALID = "valid";
    private static final String ROW_STATUS_ACCEPTED = "accepted";
    private static final String VALUE_STATUS_VALID = "valid";
    private static final String DATA_STATUS_DRAFT = "draft";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ICeEmissionActivityImportValidationService importValidationService;
    private final CeTemplateSheetMapper templateSheetMapper;
    private final CeTemplateFieldMapper templateFieldMapper;
    private final CeCaptureBatchMapper captureBatchMapper;
    private final CeCaptureRowMapper captureRowMapper;
    private final CeCaptureCellMapper captureCellMapper;
    private final CeActivityDataMapper activityDataMapper;
    private final CeEmissionSourceMapper emissionSourceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CeEmissionActivityCaptureResult saveManual(CeEmissionActivityValidationRequest request) {
        CeEmissionActivityValidationRequest row = copyRowWithDefaultRowNumber(request);
        CeEmissionActivityImportValidationRequest importRequest = new CeEmissionActivityImportValidationRequest();
        importRequest.setHeaderFields(copyEntryHeader());
        importRequest.setRows(List.of(row));
        return validateAndPersist(importRequest, SOURCE_MODE_MANUAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CeEmissionActivityCaptureResult importRows(CeEmissionActivityImportValidationRequest request) {
        return validateAndPersist(request, SOURCE_MODE_EXCEL_IMPORT);
    }

    private CeEmissionActivityCaptureResult validateAndPersist(CeEmissionActivityImportValidationRequest request, String sourceMode) {
        log.info("emission_activity capture persistence started, sourceMode={}, rows={}", sourceMode,
            request == null || request.getRows() == null ? 0 : request.getRows().size());

        CeEmissionActivityImportValidationResult validation = importValidationService.validateImport(request);
        log.info("emission_activity validation result: valid={}, blocking={}, headerIssues={}, rowResults={}",
            validation.isValid(),
            validation.isBlocking(),
            validation.getHeaderIssues() == null ? 0 : validation.getHeaderIssues().size(),
            validation.getRowResults() == null ? 0 : validation.getRowResults().size());

        CeEmissionActivityCaptureResult result = new CeEmissionActivityCaptureResult();
        result.setValidationResult(validation);
        result.setPersisted(false);
        result.setPersistedRowCount(0);

        if (!validation.isValid()) {
            log.warn("emission_activity validation failed; persistence skipped");
            return result;
        }

        List<CeEmissionActivityValidationRequest> rows = request == null ? Collections.emptyList() : request.getRows();
        if (rows == null || rows.isEmpty()) {
            log.warn("emission_activity request has no rows; persistence skipped");
            return result;
        }

        try {
            CeTemplateSheet sheet = resolveSheet();
            log.info("emission_activity template resolved, sheetId={}", sheet.getId());

            Map<String, CeTemplateField> fieldsByCode = resolveFields(sheet.getId());
            log.info("emission_activity template fields resolved, count={}", fieldsByCode.size());

            CeCaptureBatch batch = insertBatch(sheet, sourceMode);
            log.info("emission_activity capture batch inserted, batchId={}", batch.getId());

            persistRows(batch.getId(), sheet.getId(), rows, validation.getRowResults(), fieldsByCode);
            persistActivityData(batch.getId(), rows, validation.getRowResults());
            log.info("emission_activity rows persisted, rowCount={}", rows.size());

            result.setPersisted(true);
            result.setBatchId(batch.getId());
            result.setPersistedRowCount(rows.size());
            return result;
        } catch (Exception e) {
            log.error("emission_activity persistence failed", e);
            throw e;
        }
    }

    private CeTemplateSheet resolveSheet() {
        List<CeTemplateSheet> sheets = templateSheetMapper.selectList(
            new LambdaQueryWrapper<CeTemplateSheet>()
                .eq(CeTemplateSheet::getTargetTableCode, TARGET_TABLE_CODE)
                .orderByDesc(CeTemplateSheet::getTemplateVersionId)
                .orderByDesc(CeTemplateSheet::getId)
        );
        if (sheets == null || sheets.isEmpty()) {
            throw new ServiceException("enterprise-local emission_activity template sheet is not configured");
        }
        return sheets.get(0);
    }

    private Map<String, CeTemplateField> resolveFields(Long sheetId) {
        List<CeTemplateField> fields = templateFieldMapper.selectList(
            new LambdaQueryWrapper<CeTemplateField>()
                .eq(CeTemplateField::getSheetId, sheetId)
                .orderByAsc(CeTemplateField::getFieldOrder)
        );
        Map<String, CeTemplateField> byCode = fields == null ? Collections.emptyMap() : fields.stream()
            .collect(Collectors.toMap(CeTemplateField::getBusinessFieldCode, Function.identity(), (left, right) -> left,
                LinkedHashMap::new));

        for (CeEmissionActivityFieldDescriptor descriptor : allFieldDescriptors()) {
            if (!byCode.containsKey(descriptor.getFieldCode())) {
                throw new ServiceException("enterprise-local emission_activity template field is missing: "
                    + descriptor.getFieldCode());
            }
        }
        return byCode;
    }

    private CeCaptureBatch insertBatch(CeTemplateSheet sheet, String sourceMode) {
        Date now = new Date();
        CeCaptureBatch batch = new CeCaptureBatch();
        batch.setTemplateVersionId(sheet.getTemplateVersionId());
        batch.setModuleCode(sheet.getModuleCode());
        batch.setSourceMode(sourceMode);
        batch.setBatchStatus(BATCH_STATUS_COMPLETED);
        batch.setValidationStatus(VALIDATION_STATUS_VALID);
        batch.setSubmittedTime(now);
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setRemark("emission_activity enterprise-local activity capture");
        captureBatchMapper.insert(batch);
        return batch;
    }

    private void persistRows(Long batchId, Long sheetId, List<CeEmissionActivityValidationRequest> rows,
                             List<CeEmissionActivityValidationResult> rowResults, Map<String, CeTemplateField> fieldsByCode) {
        for (int index = 0; index < rows.size(); index++) {
            CeEmissionActivityValidationRequest rowRequest = rows.get(index);
            CeCaptureRow row = insertRow(batchId, sheetId, rowRequest.getRowNumber());
            Map<String, String> valuesByCode = mergedValues(rowRequest, rowResultAt(rowResults, index));
            for (CeEmissionActivityFieldDescriptor descriptor : allFieldDescriptors()) {
                CeTemplateField field = fieldsByCode.get(descriptor.getFieldCode());
                insertCell(row.getId(), field, valuesByCode.get(descriptor.getFieldCode()));
            }
        }
    }

    private void persistActivityData(Long batchId, List<CeEmissionActivityValidationRequest> rows,
                                     List<CeEmissionActivityValidationResult> rowResults) {
        Date now = new Date();
        List<Map<String, String>> rowValues = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            rowValues.add(mergedValues(rows.get(index), rowResultAt(rowResults, index)));
        }
        Map<String, CeEmissionSource> sourcesByCode = loadEmissionSources(rowValues);
        for (int index = 0; index < rows.size(); index++) {
            Map<String, String> valuesByCode = rowValues.get(index);
            CeActivityData activityData = toActivityData(batchId, valuesByCode, sourcesByCode, now);
            if (activityData.getEmissionSourceId() == null) {
                throw new ServiceException("enterprise-local emission_activity emission source is missing: "
                    + valuesByCode.get("sourceIdentificationCode"));
            }
            activityDataMapper.insert(activityData);
        }
    }

    private Map<String, CeEmissionSource> loadEmissionSources(List<Map<String, String>> rowValues) {
        List<String> sourceCodes = rowValues.stream()
            .map(values -> normalize(values.get("sourceIdentificationCode")))
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        List<String> companyCodes = rowValues.stream()
            .map(values -> normalize(values.get("companyCode")))
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (sourceCodes.isEmpty() || companyCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CeEmissionSource> sources = emissionSourceMapper.selectList(
            new LambdaQueryWrapper<CeEmissionSource>()
                .select(CeEmissionSource::getId, CeEmissionSource::getCompanyCode, CeEmissionSource::getFactoryCode,
                    CeEmissionSource::getCompanyName, CeEmissionSource::getFactoryName, CeEmissionSource::getSourceIdentificationCode,
                    CeEmissionSource::getResponsibleDept, CeEmissionSource::getDataSource)
                .in(CeEmissionSource::getCompanyCode, companyCodes)
                .in(CeEmissionSource::getSourceIdentificationCode, sourceCodes)
        );
        return sources == null ? Collections.emptyMap() : sources.stream()
            .filter(source -> source.getId() != null
                && StringUtils.isNotBlank(source.getCompanyCode())
                && StringUtils.isNotBlank(source.getSourceIdentificationCode()))
            .collect(Collectors.toMap(
                source -> sourceKey(source.getCompanyCode(), source.getSourceIdentificationCode()),
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private CeActivityData toActivityData(Long batchId, Map<String, String> valuesByCode,
                                          Map<String, CeEmissionSource> sourcesByCode, Date now) {
        CeEmissionSource source = sourcesByCode.get(sourceKey(valuesByCode.get("companyCode"), valuesByCode.get("sourceIdentificationCode")));
        CeActivityData activityData = new CeActivityData();
        activityData.setBatchId(batchId);
        activityData.setEmissionSourceId(source == null ? null : source.getId());
        activityData.setSourceSheetCode(TARGET_TABLE_CODE);
        activityData.setSourceIdentificationCode(valuesByCode.get("sourceIdentificationCode"));
        activityData.setCompanyCode(valuesByCode.get("companyCode"));
        activityData.setCompanyName(firstNonBlank(valuesByCode.get("companyName"), source == null ? null : source.getCompanyName()));
        activityData.setFactoryCode(source == null ? null : source.getFactoryCode());
        activityData.setFactoryName(firstNonBlank(valuesByCode.get("factoryName"), source == null ? null : source.getFactoryName()));
        activityData.setSourceCategoryKey(valuesByCode.get("sourceCategoryKey"));
        activityData.setScopeName(valuesByCode.get("scopeName"));
        activityData.setScopeSubcategory(valuesByCode.get("scopeSubcategory"));
        activityData.setSourceIdentificationName(valuesByCode.get("sourceIdentificationName"));
        activityData.setEmissionSourceName(valuesByCode.get("emissionSourceName"));
        activityData.setActivityUnit(valuesByCode.get("activityUnit"));
        activityData.setActivityYear(toIntegerValue(valuesByCode.get("activityYear")));
        activityData.setActivityMonth(toIntegerValue(valuesByCode.get("activityMonth")));
        activityData.setActivityDate(toDateValue("activityDate", valuesByCode.get("activityDate")));
        activityData.setActivityValue(toDecimalValue("activityValue", valuesByCode.get("activityValue")));
        activityData.setResponsibleDept(firstNonBlank(valuesByCode.get("responsibleDept"), source == null ? null : source.getResponsibleDept()));
        activityData.setDataSource(firstNonBlank(valuesByCode.get("dataSource"), source == null ? null : source.getDataSource()));
        activityData.setSourceRemark(valuesByCode.get("sourceRemark"));
        activityData.setFactorKey(valuesByCode.get("factorKey"));
        activityData.setDataStatus(DATA_STATUS_DRAFT);
        activityData.setCreateTime(now);
        activityData.setUpdateTime(now);
        activityData.setRemark("emission_activity enterprise-local activity capture");
        return activityData;
    }

    private String sourceKey(String companyCode, String sourceCode) {
        return normalize(companyCode) + "|" + normalize(sourceCode);
    }

    private String firstNonBlank(String preferred, String fallback) {
        return StringUtils.isNotBlank(preferred) ? preferred : fallback;
    }

    private CeEmissionActivityValidationResult rowResultAt(List<CeEmissionActivityValidationResult> rowResults, int index) {
        return rowResults == null || index >= rowResults.size() ? null : rowResults.get(index);
    }

    private CeCaptureRow insertRow(Long batchId, Long sheetId, Integer sourceRowNo) {
        Date now = new Date();
        CeCaptureRow row = new CeCaptureRow();
        row.setBatchId(batchId);
        row.setSheetId(sheetId);
        row.setSourceRowNo(sourceRowNo == null ? 0 : sourceRowNo);
        row.setRowStatus(ROW_STATUS_ACCEPTED);
        row.setValidationLevel(VALIDATION_STATUS_VALID);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        captureRowMapper.insert(row);
        return row;
    }

    private void insertCell(Long rowId, CeTemplateField field, String value) {
        Date now = new Date();
        CeCaptureCell cell = new CeCaptureCell();
        cell.setRowId(rowId);
        cell.setFieldId(field.getId());
        cell.setTextValue(value);
        cell.setDecimalValue(toDecimalValue(field.getBusinessFieldCode(), value));
        cell.setDateValue(toDateValue(field.getBusinessFieldCode(), value));
        cell.setValueStatus(VALUE_STATUS_VALID);
        cell.setCreateTime(now);
        cell.setUpdateTime(now);
        captureCellMapper.insert(cell);
    }

    private Map<String, String> mergedValues(CeEmissionActivityValidationRequest rowRequest, CeEmissionActivityValidationResult rowResult) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rowRequest != null && rowRequest.getFieldValues() != null) {
            for (CeEmissionActivityFieldValue fieldValue : rowRequest.getFieldValues()) {
                if (fieldValue != null && StringUtils.isNotBlank(fieldValue.getFieldCode())) {
                    values.put(fieldValue.getFieldCode().trim(), normalize(fieldValue.getValue()));
                }
            }
        }
        if (rowResult != null && rowResult.getResolvedDerivedFieldValues() != null) {
            for (CeEmissionActivityFieldValue resolvedValue : rowResult.getResolvedDerivedFieldValues()) {
                if (resolvedValue != null && StringUtils.isNotBlank(resolvedValue.getFieldCode())) {
                    values.put(resolvedValue.getFieldCode().trim(), normalize(resolvedValue.getValue()));
                }
            }
        }
        return values;
    }

    private CeEmissionActivityValidationRequest copyRowWithDefaultRowNumber(CeEmissionActivityValidationRequest request) {
        CeEmissionActivityValidationRequest copy = new CeEmissionActivityValidationRequest();
        copy.setRowNumber(request == null || request.getRowNumber() == null ? 1 : request.getRowNumber());
        copy.setFieldValues(request == null ? Collections.emptyList() : copyFieldValues(request.getFieldValues()));
        return copy;
    }

    private List<CeEmissionActivityFieldValue> copyFieldValues(List<CeEmissionActivityFieldValue> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<CeEmissionActivityFieldValue> copies = new ArrayList<>(values.size());
        for (CeEmissionActivityFieldValue value : values) {
            if (value == null) {
                copies.add(null);
                continue;
            }
            CeEmissionActivityFieldValue copy = new CeEmissionActivityFieldValue();
            copy.setFieldCode(value.getFieldCode());
            copy.setFieldName(value.getFieldName());
            copy.setValue(value.getValue());
            copies.add(copy);
        }
        return copies;
    }

    private List<CeEmissionActivityFieldDescriptor> copyEntryHeader() {
        return CeEmissionActivityValidationServiceImpl.entryFieldDescriptors().stream()
            .map(this::copyDescriptor)
            .toList();
    }

    private List<CeEmissionActivityFieldDescriptor> allFieldDescriptors() {
        return CeEmissionActivityValidationServiceImpl.allFieldDescriptors();
    }

    private CeEmissionActivityFieldDescriptor copyDescriptor(CeEmissionActivityFieldDescriptor source) {
        CeEmissionActivityFieldDescriptor copy = new CeEmissionActivityFieldDescriptor();
        copy.setFieldOrder(source.getFieldOrder());
        copy.setFieldCode(source.getFieldCode());
        copy.setFieldName(source.getFieldName());
        copy.setSourceRequired(source.isSourceRequired());
        copy.setRowValueRequired(source.isRowValueRequired());
        copy.setDerivedField(source.isDerivedField());
        return copy;
    }

    private BigDecimal toDecimalValue(String fieldCode, String value) {
        if (!StringUtils.equals("activityValue", fieldCode) || StringUtils.isBlank(value)) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    private Date toDateValue(String fieldCode, String value) {
        if (!StringUtils.equals("activityDate", fieldCode) || StringUtils.isBlank(value)) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(value.trim(), DATE_FORMATTER);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate localDate = YearMonth.parse(value.trim(), YEAR_MONTH_FORMATTER).atDay(1);
                return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    private Integer toIntegerValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Integer.valueOf(value.trim());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
