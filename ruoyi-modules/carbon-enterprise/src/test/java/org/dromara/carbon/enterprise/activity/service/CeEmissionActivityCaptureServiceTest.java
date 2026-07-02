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
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityCaptureResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityResolvedRow;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityImportValidationService;
import org.dromara.carbon.enterprise.activity.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityDerivedFieldResolver;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityCaptureServiceImpl;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityImportValidationServiceImpl;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityValidationServiceImpl;
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
class CeEmissionActivityCaptureServiceTest {

    private CeTemplateSheetMapper templateSheetMapper;
    private CeTemplateFieldMapper templateFieldMapper;
    private CeCaptureBatchMapper captureBatchMapper;
    private CeCaptureRowMapper captureRowMapper;
    private CeCaptureCellMapper captureCellMapper;
    private CeActivityDataMapper activityDataMapper;
    private CeEmissionSourceMapper emissionSourceMapper;
    private CeEmissionActivityCaptureServiceImpl service;

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

        CeEmissionActivityValidationServiceImpl rowValidator = new CeEmissionActivityValidationServiceImpl(fakeResolver());
        service = new CeEmissionActivityCaptureServiceImpl(
            new CeEmissionActivityImportValidationServiceImpl(rowValidator),
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
        CeEmissionActivityCaptureResult result = service.importRows(importRequest(row(values -> {
            values.put("activityValue", "0");
        })));

        assertFalse(result.isPersisted());
        assertNull(result.getBatchId());
        assertEquals(0, result.getPersistedRowCount());
        assertFalse(result.getValidationResult().isValid());
        CeEmissionActivityValidationIssue issue = result.getValidationResult().getRowResults().get(0).getIssues().get(0);
        assertEquals(9, issue.getRowNumber());
        assertEquals("activityValue", issue.getFieldCode());
        assertEquals("INVALID_VALUE_DOMAIN", issue.getCode());
        assertEquals("activity value must be greater than zero", issue.getMessage());
        verifyNoInteractions(templateSheetMapper, templateFieldMapper, captureBatchMapper, captureRowMapper,
            captureCellMapper, activityDataMapper);
    }

    @Test
    void manualSaveAndImportShareTheSameFieldValidation() {
        CeEmissionActivityValidationRequest invalidRow = row(values -> {
            values.put("activityMonth", "13");
        });

        CeEmissionActivityCaptureResult manualResult = service.saveManual(invalidRow);
        CeEmissionActivityCaptureResult importResult = service.importRows(importRequest(invalidRow));

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

        CeEmissionActivityCaptureResult result = service.importRows(importRequest(row(values -> values.put("factorKey", ""))));

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
        assertEquals("emission_activity", activityData.getSourceSheetCode());
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
        assertNull(activityData.getActivityPeriod());
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
    void sourceAActivitySampleHeadersParseBeforeImport() throws IOException {
        Path sample = findWorkspaceFile("source（A）/活动数据表/3 排放活动数据表10101.xlsx");
        ICeEmissionActivityImportValidationService parser = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = parser.parseImportFile(new MockMultipartFile(
            "file",
            sample.getFileName().toString(),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            Files.readAllBytes(sample)
        ));

        assertEquals(18, request.getHeaderFields().size());
        assertFalse(request.getRows().isEmpty());
        assertEquals("PK_排放源识别编号", request.getHeaderFields().get(0).getFieldName());
    }

    @Test
    void yearMonthDatePersistsTypedDateAsFirstDayOfMonth() {
        stubTemplateLookups();
        stubGeneratedIds();

        service.importRows(importRequest(row(values -> {
            values.put("activityDate", "2026-06");
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
        CeEmissionActivityValidationRequest first = row(values -> {
        });
        CeEmissionActivityValidationRequest second = row(values -> {
        });

        CeEmissionActivityCaptureResult result = service.importRows(importRequest(List.of(first, second)));

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
            values.put("sourceIdentificationCode", "UNKNOWN");
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
        sheet.setTargetTableCode("emission_activity");
        when(templateSheetMapper.selectList(any())).thenReturn(List.of(sheet));

        when(templateFieldMapper.selectList(any())).thenReturn(CeEmissionActivityValidationServiceImpl.allFieldDescriptors().stream()
            .map(this::templateField)
            .toList());
    }

    private CeTemplateField templateField(CeEmissionActivityFieldDescriptor descriptor) {
        CeTemplateField field = new CeTemplateField();
        field.setId(1000L + descriptor.getFieldOrder());
        field.setSheetId(50L);
        field.setFieldOrder(descriptor.getFieldOrder());
        field.setOriginalFieldName(descriptor.getFieldName());
        field.setBusinessFieldCode(descriptor.getFieldCode());
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

    private void stubEmissionSourceIds(List<CeEmissionActivityValidationRequest> rows) {
        AtomicLong sourceIds = new AtomicLong(4000L);
        List<CeEmissionSource> sources = rows.stream()
            .map(row -> row.getFieldValues().stream()
                .filter(field -> "sourceIdentificationCode".equals(field.getFieldCode()))
                .map(CeEmissionActivityFieldValue::getValue)
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

    private String companyCodeFor(List<CeEmissionActivityValidationRequest> rows, String sourceCode) {
        for (CeEmissionActivityValidationRequest row : rows) {
            String rowSourceCode = null;
            String companyCode = null;
            for (CeEmissionActivityFieldValue field : row.getFieldValues()) {
                if ("sourceIdentificationCode".equals(field.getFieldCode())) {
                    rowSourceCode = field.getValue();
                } else if ("companyCode".equals(field.getFieldCode())) {
                    companyCode = field.getValue();
                }
            }
            if (sourceCode.equals(rowSourceCode) && companyCode != null && !companyCode.isBlank()) {
                return companyCode;
            }
        }
        return null;
    }

    private List<String> rowIssueCodes(CeEmissionActivityCaptureResult result) {
        return result.getValidationResult().getRowResults().get(0).getIssues().stream()
            .map(CeEmissionActivityValidationIssue::getCode)
            .toList();
    }

    private String sampleIssueSummary(CeEmissionActivityImportValidationResult result) {
        return result.getRowResults().stream()
            .filter(row -> !row.isValid())
            .limit(5)
            .map(row -> row.getRowNumber() + ":" + row.getIssues().stream()
                .map(issue -> issue.getCode() + "/" + issue.getFieldCode())
                .toList())
            .toList()
            .toString();
    }

    private CeEmissionActivityImportValidationRequest importRequest(CeEmissionActivityValidationRequest row) {
        return importRequest(List.of(row));
    }

    private CeEmissionActivityImportValidationRequest importRequest(List<CeEmissionActivityValidationRequest> rows) {
        CeEmissionActivityImportValidationRequest request = new CeEmissionActivityImportValidationRequest();
        request.setHeaderFields(entryHeader());
        request.setRows(rows);
        return request;
    }

    private List<CeEmissionActivityFieldDescriptor> entryHeader() {
        return CeEmissionActivityValidationServiceImpl.entryFieldDescriptors().stream()
            .map(this::copyHeader)
            .toList();
    }

    private CeEmissionActivityFieldDescriptor copyHeader(CeEmissionActivityFieldDescriptor source) {
        CeEmissionActivityFieldDescriptor descriptor = new CeEmissionActivityFieldDescriptor();
        descriptor.setFieldOrder(source.getFieldOrder());
        descriptor.setFieldCode(source.getFieldCode());
        descriptor.setFieldName(source.getFieldName());
        descriptor.setSourceRequired(source.isSourceRequired());
        descriptor.setRowValueRequired(source.isRowValueRequired());
        descriptor.setDerivedField(source.isDerivedField());
        return descriptor;
    }

    private CeEmissionActivityValidationRequest row(Consumer<Map<String, String>> customizer) {
        Map<String, String> values = baseValues();
        customizer.accept(values);
        CeEmissionActivityValidationRequest request = new CeEmissionActivityValidationRequest();
        request.setRowNumber(9);
        request.setFieldValues(values.entrySet().stream()
            .map(entry -> field(entry.getKey(), entry.getValue()))
            .toList());
        return request;
    }

    private Map<String, String> baseValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sourceIdentificationCode", "SRC-001");
        values.put("companyCode", "COMP-001");
        values.put("companyName", "Company One");
        values.put("factoryName", "Factory One");
        values.put("sourceCategoryKey", "CAT-001");
        values.put("scopeName", "Scope 1");
        values.put("scopeSubcategory", "Stationary Combustion");
        values.put("sourceIdentificationName", "Natural Gas Boiler");
        values.put("emissionSourceName", "Natural Gas");
        values.put("activityUnit", "Nm3");
        values.put("activityYear", "2026");
        values.put("activityMonth", "6");
        values.put("activityDate", "2026-06-05");
        values.put("activityValue", "12.5");
        values.put("responsibleDept", "Production");
        values.put("dataSource", "Meter");
        values.put("sourceRemark", "Normal record");
        values.put("factorKey", "EF-2026-001");
        return values;
    }

    private CeEmissionActivityFieldValue field(String code, String value) {
        CeEmissionActivityFieldValue fieldValue = new CeEmissionActivityFieldValue();
        fieldValue.setFieldCode(code);
        fieldValue.setValue(value);
        return fieldValue;
    }

    private ICeEmissionActivityDerivedFieldResolver fakeResolver() {
        return code -> {
            if (!"SRC-001".equals(code)) {
                return Optional.empty();
            }
            CeEmissionActivityResolvedRow row = new CeEmissionActivityResolvedRow();
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

    private ICeEmissionActivityDerivedFieldResolver resolverFrom(CeEmissionActivityImportValidationRequest parsed) {
        Map<String, CeEmissionActivityResolvedRow> rowsByCode = new HashMap<>();
        for (CeEmissionActivityValidationRequest row : parsed.getRows()) {
            Map<String, String> values = new LinkedHashMap<>();
            for (CeEmissionActivityFieldValue fieldValue : row.getFieldValues()) {
                values.put(fieldValue.getFieldCode(), fieldValue.getValue());
            }
            CeEmissionActivityResolvedRow resolved = new CeEmissionActivityResolvedRow();
            resolved.setEmissionSourceCode(values.get("sourceIdentificationCode"));
            resolved.setCompanyCode(values.get("companyCode"));
            resolved.setCompanyName(values.get("companyName"));
            resolved.setFactoryName(values.get("factoryName"));
            resolved.setEmissionSourceCategoryCode(values.get("sourceCategoryKey"));
            resolved.setScope(values.get("scopeName"));
            resolved.setScopeSubcategory(values.get("scopeSubcategory"));
            resolved.setEmissionSourceIdentity(values.get("sourceIdentificationName"));
            resolved.setEmissionSourceName(values.get("emissionSourceName"));
            resolved.setUnit(values.get("activityUnit"));
            resolved.setEmissionFactorCode(values.get("factorKey"));
            rowsByCode.put(values.get("sourceIdentificationCode"), resolved);
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
