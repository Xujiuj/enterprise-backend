package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.domain.CeTemplateField;
import org.dromara.carbon.enterprise.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ActivityCaptureResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationResult;
import org.dromara.carbon.enterprise.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityCaptureService;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityImportValidationService;
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
 * Persists validated sheet_656 activity rows into enterprise-local ce_* capture tables.
 */
@RequiredArgsConstructor
@Service
public class CeSheet656ActivityCaptureServiceImpl implements ICeSheet656ActivityCaptureService {

    private static final String TARGET_TABLE_CODE = "sheet_656";
    private static final String SOURCE_MODE_MANUAL = "manual";
    private static final String SOURCE_MODE_EXCEL_IMPORT = "excel_import";
    private static final String BATCH_STATUS_COMPLETED = "completed";
    private static final String VALIDATION_STATUS_VALID = "valid";
    private static final String ROW_STATUS_ACCEPTED = "accepted";
    private static final String VALUE_STATUS_VALID = "valid";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ICeSheet656ActivityImportValidationService importValidationService;
    private final CeTemplateSheetMapper templateSheetMapper;
    private final CeTemplateFieldMapper templateFieldMapper;
    private final CeCaptureBatchMapper captureBatchMapper;
    private final CeCaptureRowMapper captureRowMapper;
    private final CeCaptureCellMapper captureCellMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CeSheet656ActivityCaptureResult saveManual(CeSheet656ValidationRequest request) {
        CeSheet656ValidationRequest row = copyRowWithDefaultRowNumber(request);
        CeSheet656ImportValidationRequest importRequest = new CeSheet656ImportValidationRequest();
        importRequest.setHeaderFields(copyFrozenHeader());
        importRequest.setRows(List.of(row));
        return validateAndPersist(importRequest, SOURCE_MODE_MANUAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CeSheet656ActivityCaptureResult importRows(CeSheet656ImportValidationRequest request) {
        return validateAndPersist(request, SOURCE_MODE_EXCEL_IMPORT);
    }

    private CeSheet656ActivityCaptureResult validateAndPersist(CeSheet656ImportValidationRequest request, String sourceMode) {
        CeSheet656ImportValidationResult validation = importValidationService.validateImport(request);
        CeSheet656ActivityCaptureResult result = new CeSheet656ActivityCaptureResult();
        result.setValidationResult(validation);
        result.setPersisted(false);
        result.setPersistedRowCount(0);

        if (!validation.isValid()) {
            return result;
        }

        List<CeSheet656ValidationRequest> rows = request == null ? Collections.emptyList() : request.getRows();
        if (rows == null || rows.isEmpty()) {
            return result;
        }

        CeTemplateSheet sheet = resolveSheet();
        Map<String, CeTemplateField> fieldsByCode = resolveFields(sheet.getId());
        CeCaptureBatch batch = insertBatch(sheet, sourceMode);
        persistRows(batch.getId(), sheet.getId(), rows, validation.getRowResults(), fieldsByCode);

        result.setPersisted(true);
        result.setBatchId(batch.getId());
        result.setPersistedRowCount(rows.size());
        return result;
    }

    private CeTemplateSheet resolveSheet() {
        List<CeTemplateSheet> sheets = templateSheetMapper.selectList(
            new LambdaQueryWrapper<CeTemplateSheet>()
                .eq(CeTemplateSheet::getTargetTableCode, TARGET_TABLE_CODE)
                .orderByDesc(CeTemplateSheet::getTemplateVersionId)
                .orderByDesc(CeTemplateSheet::getId)
        );
        if (sheets == null || sheets.isEmpty()) {
            throw new ServiceException("enterprise-local sheet_656 template sheet is not configured");
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
            .collect(Collectors.toMap(CeTemplateField::getTargetColumnCode, Function.identity(), (left, right) -> left,
                LinkedHashMap::new));

        for (CeSheet656FieldDescriptor descriptor : frozenFieldDescriptors()) {
            if (!byCode.containsKey(descriptor.getSourceColumnCode())) {
                throw new ServiceException("enterprise-local sheet_656 template field is missing: "
                    + descriptor.getSourceColumnCode());
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
        batch.setRemark("sheet_656 enterprise-local activity capture");
        captureBatchMapper.insert(batch);
        return batch;
    }

    private void persistRows(Long batchId, Long sheetId, List<CeSheet656ValidationRequest> rows,
                             List<CeSheet656ValidationResult> rowResults, Map<String, CeTemplateField> fieldsByCode) {
        for (int index = 0; index < rows.size(); index++) {
            CeSheet656ValidationRequest rowRequest = rows.get(index);
            CeCaptureRow row = insertRow(batchId, sheetId, rowRequest.getRowNumber());
            Map<String, String> valuesByCode = mergedValues(rowRequest, rowResultAt(rowResults, index));
            for (CeSheet656FieldDescriptor descriptor : frozenFieldDescriptors()) {
                CeTemplateField field = fieldsByCode.get(descriptor.getSourceColumnCode());
                insertCell(row.getId(), field, valuesByCode.get(descriptor.getSourceColumnCode()));
            }
        }
    }

    private CeSheet656ValidationResult rowResultAt(List<CeSheet656ValidationResult> rowResults, int index) {
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
        cell.setDecimalValue(toDecimalValue(field.getTargetColumnCode(), value));
        cell.setDateValue(toDateValue(field.getTargetColumnCode(), value));
        cell.setValueStatus(VALUE_STATUS_VALID);
        cell.setCreateTime(now);
        cell.setUpdateTime(now);
        captureCellMapper.insert(cell);
    }

    private Map<String, String> mergedValues(CeSheet656ValidationRequest rowRequest, CeSheet656ValidationResult rowResult) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rowRequest != null && rowRequest.getFieldValues() != null) {
            for (CeSheet656FieldValue fieldValue : rowRequest.getFieldValues()) {
                if (fieldValue != null && StringUtils.isNotBlank(fieldValue.getSourceColumnCode())) {
                    values.put(fieldValue.getSourceColumnCode().trim(), normalize(fieldValue.getValue()));
                }
            }
        }
        if (rowResult != null && rowResult.getResolvedDerivedFieldValues() != null) {
            for (CeSheet656FieldValue resolvedValue : rowResult.getResolvedDerivedFieldValues()) {
                if (resolvedValue != null && StringUtils.isNotBlank(resolvedValue.getSourceColumnCode())) {
                    values.put(resolvedValue.getSourceColumnCode().trim(), normalize(resolvedValue.getValue()));
                }
            }
        }
        return values;
    }

    private CeSheet656ValidationRequest copyRowWithDefaultRowNumber(CeSheet656ValidationRequest request) {
        CeSheet656ValidationRequest copy = new CeSheet656ValidationRequest();
        copy.setRowNumber(request == null || request.getRowNumber() == null ? 1 : request.getRowNumber());
        copy.setFieldValues(request == null ? Collections.emptyList() : copyFieldValues(request.getFieldValues()));
        return copy;
    }

    private List<CeSheet656FieldValue> copyFieldValues(List<CeSheet656FieldValue> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<CeSheet656FieldValue> copies = new ArrayList<>(values.size());
        for (CeSheet656FieldValue value : values) {
            if (value == null) {
                copies.add(null);
                continue;
            }
            CeSheet656FieldValue copy = new CeSheet656FieldValue();
            copy.setSourceColumnCode(value.getSourceColumnCode());
            copy.setSourceColumnName(value.getSourceColumnName());
            copy.setValue(value.getValue());
            copies.add(copy);
        }
        return copies;
    }

    private List<CeSheet656FieldDescriptor> copyFrozenHeader() {
        return frozenFieldDescriptors().stream()
            .map(this::copyDescriptor)
            .toList();
    }

    private List<CeSheet656FieldDescriptor> frozenFieldDescriptors() {
        return CeSheet656ValidationServiceImpl.frozenFieldDescriptors();
    }

    private CeSheet656FieldDescriptor copyDescriptor(CeSheet656FieldDescriptor source) {
        CeSheet656FieldDescriptor copy = new CeSheet656FieldDescriptor();
        copy.setFieldOrder(source.getFieldOrder());
        copy.setSourceColumnCode(source.getSourceColumnCode());
        copy.setSourceColumnName(source.getSourceColumnName());
        copy.setSourceRequired(source.isSourceRequired());
        copy.setRowValueRequired(source.isRowValueRequired());
        copy.setDerivedField(source.isDerivedField());
        return copy;
    }

    private BigDecimal toDecimalValue(String sourceColumnCode, String value) {
        if (!StringUtils.equals("f014", sourceColumnCode) || StringUtils.isBlank(value)) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    private Date toDateValue(String sourceColumnCode, String value) {
        if (!StringUtils.equals("f013", sourceColumnCode) || StringUtils.isBlank(value)) {
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

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
