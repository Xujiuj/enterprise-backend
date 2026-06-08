package org.dromara.carbon.enterprise.activity;

import org.dromara.carbon.enterprise.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.domain.CeTemplateField;
import org.dromara.carbon.enterprise.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ActivityCaptureResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.service.ICeSheet656DerivedFieldResolver;
import org.dromara.carbon.enterprise.service.impl.CeSheet656ActivityCaptureServiceImpl;
import org.dromara.carbon.enterprise.service.impl.CeSheet656ActivityImportValidationServiceImpl;
import org.dromara.carbon.enterprise.service.impl.CeSheet656ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeSheet656ActivityCaptureServiceTest {

    private CeTemplateSheetMapper templateSheetMapper;
    private CeTemplateFieldMapper templateFieldMapper;
    private CeCaptureBatchMapper captureBatchMapper;
    private CeCaptureRowMapper captureRowMapper;
    private CeCaptureCellMapper captureCellMapper;
    private CeSheet656ActivityCaptureServiceImpl service;

    @BeforeEach
    void setUp() {
        templateSheetMapper = mock(CeTemplateSheetMapper.class);
        templateFieldMapper = mock(CeTemplateFieldMapper.class);
        captureBatchMapper = mock(CeCaptureBatchMapper.class);
        captureRowMapper = mock(CeCaptureRowMapper.class);
        captureCellMapper = mock(CeCaptureCellMapper.class);

        CeSheet656ValidationServiceImpl rowValidator = new CeSheet656ValidationServiceImpl(fakeResolver());
        service = new CeSheet656ActivityCaptureServiceImpl(
            new CeSheet656ActivityImportValidationServiceImpl(rowValidator),
            templateSheetMapper,
            templateFieldMapper,
            captureBatchMapper,
            captureRowMapper,
            captureCellMapper
        );
    }

    @Test
    void failedImportReturnsRowFieldReasonAndDoesNotPersistPartialBusinessData() {
        CeSheet656ActivityCaptureResult result = service.importRows(importRequest(row(values -> {
            values.put("f014", "0");
        })));

        assertFalse(result.isPersisted());
        assertNull(result.getBatchId());
        assertEquals(0, result.getPersistedRowCount());
        assertFalse(result.getValidationResult().isValid());
        CeSheet656ValidationIssue issue = result.getValidationResult().getRowResults().get(0).getIssues().get(0);
        assertEquals(9, issue.getRowNumber());
        assertEquals("f014", issue.getSourceColumnCode());
        assertEquals("INVALID_VALUE_DOMAIN", issue.getCode());
        assertEquals("activity value must be greater than zero", issue.getMessage());
        verifyNoInteractions(templateSheetMapper, templateFieldMapper, captureBatchMapper, captureRowMapper,
            captureCellMapper);
    }

    @Test
    void manualSaveAndImportShareTheSameFieldValidation() {
        CeSheet656ValidationRequest invalidRow = row(values -> {
            values.put("f012", "13");
        });

        CeSheet656ActivityCaptureResult manualResult = service.saveManual(invalidRow);
        CeSheet656ActivityCaptureResult importResult = service.importRows(importRequest(invalidRow));

        assertFalse(manualResult.isPersisted());
        assertFalse(importResult.isPersisted());
        assertEquals(List.of("INVALID_VALUE_DOMAIN"), rowIssueCodes(manualResult));
        assertEquals(rowIssueCodes(manualResult), rowIssueCodes(importResult));
        verifyNoInteractions(templateSheetMapper, templateFieldMapper, captureBatchMapper, captureRowMapper,
            captureCellMapper);
    }

    @Test
    void validImportPersistsOnlyEnterpriseLocalCaptureTables() {
        stubTemplateLookups();
        stubGeneratedIds();

        CeSheet656ActivityCaptureResult result = service.importRows(importRequest(row(values -> {
            values.put("f003", "");
            values.put("f018", "");
        })));

        assertTrue(result.isPersisted());
        assertEquals(100L, result.getBatchId());
        assertEquals(1, result.getPersistedRowCount());
        assertTrue(result.getValidationResult().isValid());

        ArgumentCaptor<CeCaptureBatch> batchCaptor = ArgumentCaptor.forClass(CeCaptureBatch.class);
        ArgumentCaptor<CeCaptureRow> rowCaptor = ArgumentCaptor.forClass(CeCaptureRow.class);
        ArgumentCaptor<CeCaptureCell> cellCaptor = ArgumentCaptor.forClass(CeCaptureCell.class);
        verify(captureBatchMapper).insert(batchCaptor.capture());
        verify(captureRowMapper).insert(rowCaptor.capture());
        verify(captureCellMapper, org.mockito.Mockito.times(18)).insert(cellCaptor.capture());
        verify(templateSheetMapper).selectList(any());
        verify(templateFieldMapper).selectList(any());

        assertEquals(1L, batchCaptor.getValue().getTemplateVersionId());
        assertEquals("03-activity", batchCaptor.getValue().getModuleCode());
        assertEquals("excel_import", batchCaptor.getValue().getSourceMode());
        assertEquals("completed", batchCaptor.getValue().getBatchStatus());
        assertEquals(100L, rowCaptor.getValue().getBatchId());
        assertEquals(50L, rowCaptor.getValue().getSheetId());
        assertEquals(9, rowCaptor.getValue().getSourceRowNo());

        Map<Long, CeCaptureCell> cellsByFieldId = cellCaptor.getAllValues().stream()
            .collect(java.util.stream.Collectors.toMap(CeCaptureCell::getFieldId, cell -> cell));
        assertEquals("Company One", cellsByFieldId.get(1003L).getTextValue());
        assertEquals("EF-2026-001", cellsByFieldId.get(1018L).getTextValue());
        assertEquals(new BigDecimal("12.5"), cellsByFieldId.get(1014L).getDecimalValue());
    }

    @Test
    void yearMonthDatePersistsTypedDateAsFirstDayOfMonth() {
        stubTemplateLookups();
        stubGeneratedIds();

        service.importRows(importRequest(row(values -> {
            values.put("f013", "2026-06");
        })));

        ArgumentCaptor<CeCaptureCell> cellCaptor = ArgumentCaptor.forClass(CeCaptureCell.class);
        verify(captureCellMapper, org.mockito.Mockito.times(18)).insert(cellCaptor.capture());

        Map<Long, CeCaptureCell> cellsByFieldId = cellCaptor.getAllValues().stream()
            .collect(java.util.stream.Collectors.toMap(CeCaptureCell::getFieldId, cell -> cell));
        assertEquals("2026-06", cellsByFieldId.get(1013L).getTextValue());
        assertEquals(LocalDate.of(2026, 6, 1), cellsByFieldId.get(1013L).getDateValue()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate());
    }

    @Test
    void duplicateRowNumbersAreRejectedBeforeAnyPersistence() {
        CeSheet656ValidationRequest first = row(values -> {
        });
        CeSheet656ValidationRequest second = row(values -> {
        });

        CeSheet656ActivityCaptureResult result = service.importRows(importRequest(List.of(first, second)));

        assertFalse(result.isPersisted());
        assertEquals("DUPLICATE_ROW_NUMBER",
            result.getValidationResult().getRowResults().get(1).getIssues().get(0).getCode());
        verifyNoInteractions(templateSheetMapper, templateFieldMapper, captureBatchMapper, captureRowMapper,
            captureCellMapper);
    }

    @Test
    void mapperExceptionPropagatesAndDoesNotWriteCellsAfterFailedRowInsert() {
        stubTemplateLookups();
        doAnswer(invocation -> {
            CeCaptureBatch batch = invocation.getArgument(0);
            batch.setId(100L);
            return 1;
        }).when(captureBatchMapper).insert(isA(CeCaptureBatch.class));
        doAnswer(invocation -> {
            throw new RuntimeException("row insert failed");
        }).when(captureRowMapper).insert(isA(CeCaptureRow.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.importRows(importRequest(row(values -> {
        }))));

        assertEquals("row insert failed", exception.getMessage());
        verify(captureBatchMapper).insert(isA(CeCaptureBatch.class));
        verify(captureRowMapper).insert(isA(CeCaptureRow.class));
        verify(captureCellMapper, never()).insert(isA(CeCaptureCell.class));
    }

    @Test
    void invalidImportNeverCreatesABatchBeforeReturningValidationResult() {
        service.importRows(importRequest(row(values -> {
            values.put("f001", "UNKNOWN");
        })));

        verify(captureBatchMapper, never()).insert(isA(CeCaptureBatch.class));
        verify(captureRowMapper, never()).insert(isA(CeCaptureRow.class));
        verify(captureCellMapper, never()).insert(isA(CeCaptureCell.class));
    }

    private void stubTemplateLookups() {
        CeTemplateSheet sheet = new CeTemplateSheet();
        sheet.setId(50L);
        sheet.setTemplateVersionId(1L);
        sheet.setModuleCode("03-activity");
        sheet.setTargetTableCode("sheet_656");
        when(templateSheetMapper.selectList(any())).thenReturn(List.of(sheet));

        when(templateFieldMapper.selectList(any())).thenReturn(CeSheet656ValidationServiceImpl.frozenFieldDescriptors().stream()
            .map(this::templateField)
            .toList());
    }

    private CeTemplateField templateField(CeSheet656FieldDescriptor descriptor) {
        CeTemplateField field = new CeTemplateField();
        field.setId(1000L + descriptor.getFieldOrder());
        field.setSheetId(50L);
        field.setFieldOrder(descriptor.getFieldOrder());
        field.setOriginalFieldName(descriptor.getSourceColumnName());
        field.setTargetColumnCode(descriptor.getSourceColumnCode());
        field.setValueType("text");
        return field;
    }

    private void stubGeneratedIds() {
        AtomicLong rowIds = new AtomicLong(200L);
        doAnswer(invocation -> {
            CeCaptureBatch batch = invocation.getArgument(0);
            batch.setId(100L);
            return 1;
        }).when(captureBatchMapper).insert(isA(CeCaptureBatch.class));
        doAnswer(invocation -> {
            CeCaptureRow row = invocation.getArgument(0);
            row.setId(rowIds.getAndIncrement());
            return 1;
        }).when(captureRowMapper).insert(isA(CeCaptureRow.class));
        doAnswer(invocation -> 1).when(captureCellMapper).insert(isA(CeCaptureCell.class));
    }

    private List<String> rowIssueCodes(CeSheet656ActivityCaptureResult result) {
        return result.getValidationResult().getRowResults().get(0).getIssues().stream()
            .map(CeSheet656ValidationIssue::getCode)
            .toList();
    }

    private CeSheet656ImportValidationRequest importRequest(CeSheet656ValidationRequest row) {
        return importRequest(List.of(row));
    }

    private CeSheet656ImportValidationRequest importRequest(List<CeSheet656ValidationRequest> rows) {
        CeSheet656ImportValidationRequest request = new CeSheet656ImportValidationRequest();
        request.setHeaderFields(frozenHeader());
        request.setRows(rows);
        return request;
    }

    private List<CeSheet656FieldDescriptor> frozenHeader() {
        return CeSheet656ValidationServiceImpl.frozenFieldDescriptors().stream()
            .map(this::copyHeader)
            .toList();
    }

    private CeSheet656FieldDescriptor copyHeader(CeSheet656FieldDescriptor source) {
        CeSheet656FieldDescriptor descriptor = new CeSheet656FieldDescriptor();
        descriptor.setFieldOrder(source.getFieldOrder());
        descriptor.setSourceColumnCode(source.getSourceColumnCode());
        descriptor.setSourceColumnName(source.getSourceColumnName());
        descriptor.setSourceRequired(source.isSourceRequired());
        descriptor.setRowValueRequired(source.isRowValueRequired());
        descriptor.setDerivedField(source.isDerivedField());
        return descriptor;
    }

    private CeSheet656ValidationRequest row(Consumer<Map<String, String>> customizer) {
        Map<String, String> values = baseValues();
        customizer.accept(values);
        CeSheet656ValidationRequest request = new CeSheet656ValidationRequest();
        request.setRowNumber(9);
        request.setFieldValues(values.entrySet().stream()
            .map(entry -> field(entry.getKey(), entry.getValue()))
            .toList());
        return request;
    }

    private Map<String, String> baseValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("f001", "SRC-001");
        values.put("f002", "COMP-001");
        values.put("f003", "Company One");
        values.put("f004", "Factory One");
        values.put("f005", "CAT-001");
        values.put("f006", "Scope 1");
        values.put("f007", "Stationary Combustion");
        values.put("f008", "Natural Gas Boiler");
        values.put("f009", "Natural Gas");
        values.put("f010", "Nm3");
        values.put("f011", "2026");
        values.put("f012", "6");
        values.put("f013", "2026-06-05");
        values.put("f014", "12.5");
        values.put("f015", "Production");
        values.put("f016", "Meter");
        values.put("f017", "Normal record");
        values.put("f018", "EF-2026-001");
        return values;
    }

    private CeSheet656FieldValue field(String code, String value) {
        CeSheet656FieldValue fieldValue = new CeSheet656FieldValue();
        fieldValue.setSourceColumnCode(code);
        fieldValue.setValue(value);
        return fieldValue;
    }

    private ICeSheet656DerivedFieldResolver fakeResolver() {
        return code -> {
            if (!"SRC-001".equals(code)) {
                return Optional.empty();
            }
            CeSheet656ResolvedRow row = new CeSheet656ResolvedRow();
            row.setEmissionSourceCode("SRC-001");
            row.setCompanyCode("COMP-001");
            row.setCompanyName("Company One");
            row.setFactoryName("Factory One");
            row.setEmissionSourceCategoryCode("CAT-001");
            row.setScope("Scope 1");
            row.setScopeSubcategory("Stationary Combustion");
            row.setEmissionSourceIdentity("Natural Gas Boiler");
            row.setEmissionSourceName("Natural Gas");
            row.setUnit("Nm3");
            row.setEmissionFactorCode("EF-2026-001");
            return Optional.of(row);
        };
    }
}
