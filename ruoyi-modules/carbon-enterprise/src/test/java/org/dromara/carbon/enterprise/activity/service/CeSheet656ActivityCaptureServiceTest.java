package org.dromara.carbon.enterprise.activity.service;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.template.domain.CeTemplateField;
import org.dromara.carbon.enterprise.template.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ActivityCaptureResult;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.shared.service.ICeSheet656ActivityImportValidationService;
import org.dromara.carbon.enterprise.activity.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.shared.service.ICeSheet656DerivedFieldResolver;
import org.dromara.carbon.enterprise.activity.service.impl.CeSheet656ActivityCaptureServiceImpl;
import org.dromara.carbon.enterprise.activity.service.impl.CeSheet656ActivityImportValidationServiceImpl;
import org.dromara.carbon.enterprise.activity.service.impl.CeSheet656ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private CeActivityDataMapper activityDataMapper;
    private CeEmissionSourceMapper emissionSourceMapper;
    private CeSheet656ActivityCaptureServiceImpl service;

    @BeforeAll
    static void initializeLambdaCache() {
        initializeEntityLambdaCache(CeEmissionSourceMapper.class, CeEmissionSource.class);
    }

    @BeforeEach
    void setUp() {
        templateSheetMapper = mock(CeTemplateSheetMapper.class);
        templateFieldMapper = mock(CeTemplateFieldMapper.class);
        captureBatchMapper = mock(CeCaptureBatchMapper.class);
        captureRowMapper = mock(CeCaptureRowMapper.class);
        captureCellMapper = mock(CeCaptureCellMapper.class);
        activityDataMapper = mock(CeActivityDataMapper.class);
        emissionSourceMapper = mock(CeEmissionSourceMapper.class);

        CeSheet656ValidationServiceImpl rowValidator = new CeSheet656ValidationServiceImpl(fakeResolver());
        service = new CeSheet656ActivityCaptureServiceImpl(
            new CeSheet656ActivityImportValidationServiceImpl(rowValidator),
            templateSheetMapper,
            templateFieldMapper,
            captureBatchMapper,
            captureRowMapper,
            captureCellMapper,
            activityDataMapper,
            emissionSourceMapper
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
            captureCellMapper, activityDataMapper);
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
            captureCellMapper, activityDataMapper);
    }

    @Test
    void validImportPersistsCaptureTablesAndActivityDataListTable() {
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
        ArgumentCaptor<CeActivityData> activityCaptor = ArgumentCaptor.forClass(CeActivityData.class);
        verify(captureBatchMapper).insert(batchCaptor.capture());
        verify(captureRowMapper).insert(rowCaptor.capture());
        verify(captureCellMapper, org.mockito.Mockito.times(18)).insert(cellCaptor.capture());
        verify(activityDataMapper).insert(activityCaptor.capture());
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

        CeActivityData activityData = activityCaptor.getValue();
        assertEquals(100L, activityData.getBatchId());
        assertEquals(3001L, activityData.getEmissionSourceId());
        assertEquals("sheet_656", activityData.getSourceSheetCode());
        assertEquals("SRC-001", activityData.getSourceIdentificationCode());
        assertEquals("COMP-001", activityData.getCompanyCode());
        assertEquals("Company One", activityData.getCompanyName());
        assertEquals("Factory One", activityData.getFactoryName());
        assertEquals("CAT-001", activityData.getSourceCategoryKey());
        assertEquals("Scope 1", activityData.getScopeName());
        assertEquals("Stationary Combustion", activityData.getScopeSubcategory());
        assertEquals("Natural Gas Boiler", activityData.getSourceIdentificationName());
        assertEquals("Natural Gas", activityData.getEmissionSourceName());
        assertEquals("Nm3", activityData.getActivityUnit());
        assertEquals(2026, activityData.getActivityYear());
        assertEquals(6, activityData.getActivityMonth());
        assertEquals(LocalDate.of(2026, 6, 5), activityData.getActivityDate()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate());
        assertEquals(new BigDecimal("12.5"), activityData.getActivityValue());
        assertEquals("Production", activityData.getResponsibleDept());
        assertEquals("Meter", activityData.getDataSource());
        assertEquals("Normal record", activityData.getSourceRemark());
        assertEquals("EF-2026-001", activityData.getFactorKey());
        assertEquals("draft", activityData.getDataStatus());
    }

    @Test
    void customerActivitySampleCanBeParsedValidatedAndImportedWithoutRowNumberBusinessField() throws IOException {
        Path sample = findWorkspaceFile("source（A）/活动数据表/3 排放活动数据表10101.xlsx");
        ICeSheet656ActivityImportValidationService parser = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );
        CeSheet656ImportValidationRequest parsed = parser.parseImportFile(new MockMultipartFile(
            "file",
            sample.getFileName().toString(),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            Files.readAllBytes(sample)
        ));
        assertEquals(1025, parsed.getRows().size());
        assertTrue(parsed.getRows().stream()
            .flatMap(row -> row.getFieldValues().stream())
            .noneMatch(field -> "rowNo".equals(field.getSourceColumnCode())
                || "row_no".equals(field.getSourceColumnCode())
                || "行号".equals(field.getSourceColumnName())));

        ICeSheet656ActivityImportValidationService importValidation = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(resolverFrom(parsed))
        );
        CeSheet656ImportValidationResult sampleValidation = importValidation.validateImport(parsed);
        Set<Integer> validRowNumbers = sampleValidation.getRowResults().stream()
            .filter(row -> row.isValid())
            .map(row -> row.getRowNumber())
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<CeSheet656ValidationRequest> importableRows = parsed.getRows().stream()
            .filter(row -> validRowNumbers.contains(row.getRowNumber()))
            .toList();
        assertTrue(!importableRows.isEmpty(), sampleIssueSummary(sampleValidation));
        CeSheet656ImportValidationRequest importableRequest = new CeSheet656ImportValidationRequest();
        importableRequest.setHeaderFields(parsed.getHeaderFields());
        importableRequest.setRows(importableRows);
        CeSheet656ActivityCaptureServiceImpl sampleImportService = new CeSheet656ActivityCaptureServiceImpl(
            importValidation,
            templateSheetMapper,
            templateFieldMapper,
            captureBatchMapper,
            captureRowMapper,
            captureCellMapper,
            activityDataMapper,
            emissionSourceMapper
        );
        stubTemplateLookups();
        stubGeneratedIds();
        stubEmissionSourceIds(importableRows);

        CeSheet656ActivityCaptureResult result = sampleImportService.importRows(importableRequest);

        assertTrue(result.isPersisted());
        assertEquals(100L, result.getBatchId());
        assertEquals(importableRows.size(), result.getPersistedRowCount());
        assertTrue(result.getValidationResult().isValid());

        ArgumentCaptor<CeCaptureRow> rowCaptor = ArgumentCaptor.forClass(CeCaptureRow.class);
        verify(captureRowMapper, org.mockito.Mockito.times(importableRows.size())).insert(rowCaptor.capture());
        verify(captureCellMapper, org.mockito.Mockito.times(importableRows.size() * 18)).insert(isA(CeCaptureCell.class));
        verify(activityDataMapper, org.mockito.Mockito.times(importableRows.size())).insert(isA(CeActivityData.class));
        assertEquals(importableRows.get(0).getRowNumber(), rowCaptor.getAllValues().get(0).getSourceRowNo());
        assertEquals(importableRows.get(importableRows.size() - 1).getRowNumber(),
            rowCaptor.getAllValues().get(importableRows.size() - 1).getSourceRowNo());
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
            captureCellMapper, activityDataMapper);
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
        verify(activityDataMapper, never()).insert(isA(CeActivityData.class));
    }

    @Test
    void invalidImportNeverCreatesABatchBeforeReturningValidationResult() {
        service.importRows(importRequest(row(values -> {
            values.put("f001", "UNKNOWN");
        })));

        verify(captureBatchMapper, never()).insert(isA(CeCaptureBatch.class));
        verify(captureRowMapper, never()).insert(isA(CeCaptureRow.class));
        verify(captureCellMapper, never()).insert(isA(CeCaptureCell.class));
        verify(activityDataMapper, never()).insert(isA(CeActivityData.class));
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
        doAnswer(invocation -> 1).when(activityDataMapper).insert(isA(CeActivityData.class));
        CeEmissionSource source = new CeEmissionSource();
        source.setId(3001L);
        source.setCompanyCode("COMP-001");
        source.setSourceIdentificationCode("SRC-001");
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of(source));
    }

    private void stubEmissionSourceIds(List<CeSheet656ValidationRequest> rows) {
        AtomicLong sourceIds = new AtomicLong(4000L);
        List<CeEmissionSource> sources = rows.stream()
            .map(row -> row.getFieldValues().stream()
                .filter(field -> "f001".equals(field.getSourceColumnCode()))
                .map(CeSheet656FieldValue::getValue)
                .findFirst()
                .orElse(null))
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .map(code -> {
                CeEmissionSource source = new CeEmissionSource();
                source.setId(sourceIds.getAndIncrement());
                source.setCompanyCode(companyCodeFor(rows, code));
                source.setSourceIdentificationCode(code);
                return source;
            })
            .toList();
        when(emissionSourceMapper.selectList(any())).thenReturn(sources);
    }

    private String companyCodeFor(List<CeSheet656ValidationRequest> rows, String sourceCode) {
        for (CeSheet656ValidationRequest row : rows) {
            String rowSourceCode = null;
            String companyCode = null;
            for (CeSheet656FieldValue field : row.getFieldValues()) {
                if ("f001".equals(field.getSourceColumnCode())) {
                    rowSourceCode = field.getValue();
                } else if ("f002".equals(field.getSourceColumnCode())) {
                    companyCode = field.getValue();
                }
            }
            if (sourceCode.equals(rowSourceCode) && companyCode != null && !companyCode.isBlank()) {
                return companyCode;
            }
        }
        return null;
    }

    private List<String> rowIssueCodes(CeSheet656ActivityCaptureResult result) {
        return result.getValidationResult().getRowResults().get(0).getIssues().stream()
            .map(CeSheet656ValidationIssue::getCode)
            .toList();
    }

    private String sampleIssueSummary(CeSheet656ImportValidationResult result) {
        return result.getRowResults().stream()
            .filter(row -> !row.isValid())
            .limit(5)
            .map(row -> row.getRowNumber() + ":" + row.getIssues().stream()
                .map(issue -> issue.getCode() + "/" + issue.getSourceColumnCode())
                .toList())
            .toList()
            .toString();
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

    private ICeSheet656DerivedFieldResolver resolverFrom(CeSheet656ImportValidationRequest parsed) {
        Map<String, CeSheet656ResolvedRow> rowsByCode = new HashMap<>();
        for (CeSheet656ValidationRequest row : parsed.getRows()) {
            Map<String, String> values = new LinkedHashMap<>();
            for (CeSheet656FieldValue fieldValue : row.getFieldValues()) {
                values.put(fieldValue.getSourceColumnCode(), fieldValue.getValue());
            }
            CeSheet656ResolvedRow resolved = new CeSheet656ResolvedRow();
            resolved.setEmissionSourceCode(values.get("f001"));
            resolved.setCompanyCode(values.get("f002"));
            resolved.setCompanyName(values.get("f003"));
            resolved.setFactoryName(values.get("f004"));
            resolved.setEmissionSourceCategoryCode(values.get("f005"));
            resolved.setScope(values.get("f006"));
            resolved.setScopeSubcategory(values.get("f007"));
            resolved.setEmissionSourceIdentity(values.get("f008"));
            resolved.setEmissionSourceName(values.get("f009"));
            resolved.setUnit(values.get("f010"));
            resolved.setEmissionFactorCode(values.get("f018"));
            rowsByCode.put(values.get("f001"), resolved);
        }
        return code -> Optional.ofNullable(rowsByCode.get(code));
    }

    private Path findWorkspaceFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        Assumptions.assumeTrue(false, "external customer sample is not available in this checkout: " + relativePath);
        throw new IllegalStateException("unreachable");
    }

    private static void initializeEntityLambdaCache(Class<?> mapperType, Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, mapperType.getName());
        assistant.setCurrentNamespace(mapperType.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, entityType);
        LambdaUtils.installCache(tableInfo);
    }
}
