package org.dromara.carbon.enterprise.activity.service;

import cn.idev.excel.FastExcel;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityResolvedRow;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityDerivedFieldResolver;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityValidationService;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityImportValidationServiceImpl;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityValidationServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeEmissionActivityImportValidationServiceTest {

    @Test
    void rejectsLegacyCustomerActivitySampleWhenRequiredSemanticHeadersAreMissing() throws IOException {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );
        Path sample = findWorkspaceFile("source（A）/活动数据表/3 排放活动数据表10101.xlsx");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.parseImportFile(new MockMultipartFile(
            "file",
            sample.getFileName().toString(),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            Files.readAllBytes(sample)
        )));

        assertTrue(exception.getMessage().contains("缺少必要表头"));
        assertTrue(exception.getMessage().contains("排放源名称"));
        assertTrue(exception.getMessage().contains("活动期间"));
    }

    @Test
    void parsesXlsxRowsByHeaderCodeAndSkipsBlankRows() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = service.parseImportFile(xlsxFile(
            entryHeader().stream().map(CeEmissionActivityFieldDescriptor::getFieldCode).toList(),
            List.of(
                rowValues("Company One", "Factory One", "Scope 1", "Stationary Combustion", "Natural Gas Boiler", "Natural Gas",
                    "2026-06", "2026-06-05", "12.5", "Production", "Meter"),
                blankRowValues(),
                rowValues("Company One", "Factory One", "Scope 1", "Stationary Combustion", "Natural Gas Boiler", "Natural Gas",
                    "2026-07", "2026-07-05", "18.5", "Production", "Meter")
            )
        ));

        assertEquals(11, request.getHeaderFields().size());
        assertEquals("公司名称", request.getHeaderFields().get(0).getFieldName());
        assertEquals(2, request.getRows().size());
        assertEquals(2, request.getRows().get(0).getRowNumber());
        assertEquals(3, request.getRows().get(1).getRowNumber());
        assertEquals("12.5", fieldValue(request.getRows().get(0), "activityValue"));
        assertEquals("", fieldValue(request.getRows().get(1), "sourceRemark"));
    }

    @Test
    void parsesCompactEntryTemplateAndLeavesDerivedFieldsForResolver() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = service.parseImportFile(xlsxFile(
            entryHeader().stream().map(CeEmissionActivityFieldDescriptor::getFieldName).toList(),
            List.of(rowValues(
                "Company One",
                "Factory One",
                "Scope 1",
                "Stationary Combustion",
                "Natural Gas Boiler",
                "Natural Gas",
                "2026-06",
                "2026-06-05",
                "12.5",
                "Production",
                "Meter"
            ))
        ));

        assertEquals(11, request.getHeaderFields().size());
        assertEquals(19, request.getRows().get(0).getFieldValues().size());
        assertEquals("", fieldValue(request.getRows().get(0), "sourceIdentificationCode"));
        assertEquals("Company One", fieldValue(request.getRows().get(0), "companyName"));
        assertEquals("Natural Gas Boiler", fieldValue(request.getRows().get(0), "sourceIdentificationName"));
        assertEquals("2026-06", fieldValue(request.getRows().get(0), "activityPeriod"));
        assertEquals("12.5", fieldValue(request.getRows().get(0), "activityValue"));
        assertEquals("", fieldValue(request.getRows().get(0), "companyCode"));
        assertEquals("", fieldValue(request.getRows().get(0), "factorKey"));

        CeEmissionActivityImportValidationResult result = service.validateImport(request);

        assertTrue(result.isHeaderValid());
        assertFalse(result.isBlocking());
        assertEquals("SRC-001", fieldValueFromResolved(result, "sourceIdentificationCode"));
        assertEquals("COMP-001", fieldValueFromResolved(result, "companyCode"));
        assertEquals("2026", fieldValueFromResolved(result, "activityYear"));
        assertEquals("6", fieldValueFromResolved(result, "activityMonth"));
        assertEquals("EF-2026-001", fieldValueFromResolved(result, "factorKey"));
    }

    @Test
    void rejectsEmptyUploadFile() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.parseImportFile(new MockMultipartFile("file", "emission_activity.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0])));

        assertEquals("请上传非空的 emission_activity Excel 文件", exception.getMessage());
    }

    @Test
    void rejectsNonXlsxUploadFile() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.parseImportFile(new MockMultipartFile("file", "emission_activity.csv", "text/csv", "a,b".getBytes())));

        assertEquals("emission_activity 仅支持上传 .xlsx 文件", exception.getMessage());
    }

    @Test
    void rejectsMissingRequiredHeaderDuringParse() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        List<String> headers = entryHeader().stream()
            .filter(field -> !"companyName".equals(field.getFieldCode()))
            .map(CeEmissionActivityFieldDescriptor::getFieldName)
            .toList();

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.parseImportFile(xlsxFile(headers, List.of(
                rowValues("Factory One", "Scope 1", "Stationary Combustion", "Natural Gas Boiler", "Natural Gas",
                    "2026-06", "2026-06-05", "12.5", "Production", "Meter")
            ))));

        assertTrue(exception.getMessage().contains("缺少必要表头"));
        assertTrue(exception.getMessage().contains("公司名称"));
    }

    @Test
    void validatesEntryHeaderAndRows() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationResult result = service.validateImport(validRequest());

        assertTrue(result.isHeaderValid());
        assertTrue(result.isValid());
        assertFalse(result.isBlocking());
        assertEquals(1, result.getRowResults().size());
        assertTrue(result.getHeaderIssues().isEmpty());
        assertTrue(result.getRowResults().get(0).getIssues().isEmpty());
    }

    @Test
    void rejectsRenamedHeaderBeforeRowValidation() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = validRequest();
        request.getHeaderFields().get(0).setFieldName("renamed-header");

        CeEmissionActivityImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(List.of("HEADER_COLUMN_MISMATCH"), headerIssueCodes(result));
        assertTrue(result.getRowResults().isEmpty());
    }

    @Test
    void rowResultsPreserveSourceRowNumberAndColumnName() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = validRequest();
        request.setRows(new ArrayList<>(request.getRows()));
        request.getRows().set(0, row(values -> values.put("activityValue", "0")));

        CeEmissionActivityImportValidationResult result = service.validateImport(request);

        assertTrue(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(9, result.getRowResults().get(0).getRowNumber());
        CeEmissionActivityValidationIssue issue = result.getRowResults().get(0).getIssues().get(0);
        assertEquals("activityValue", issue.getFieldCode());
        assertEquals(9, issue.getRowNumber());
        assertEquals("INVALID_VALUE_DOMAIN", issue.getCode());
    }

    @Test
    void rejectsDuplicateRowNumbersBeforePersistCanUseThemAsKeys() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = validRequest();
        request.setRows(List.of(row(values -> {
        }), row(values -> {
        })));

        CeEmissionActivityImportValidationResult result = service.validateImport(request);

        assertTrue(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(2, result.getRowResults().size());
        assertTrue(result.getRowResults().get(0).isValid());
        assertFalse(result.getRowResults().get(1).isValid());
        CeEmissionActivityValidationIssue issue = result.getRowResults().get(1).getIssues().get(0);
        assertEquals("DUPLICATE_ROW_NUMBER", issue.getCode());
        assertEquals(9, issue.getRowNumber());
        assertEquals("rowNumber", issue.getFieldCode());
    }

    @Test
    void headerMismatchBlocksRowsAndDoesNotCallRowValidator() {
        ICeEmissionActivityValidationService rowValidator = mock(ICeEmissionActivityValidationService.class);
        when(rowValidator.listEntryFields()).thenReturn(entryHeader());

        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(rowValidator);
        CeEmissionActivityImportValidationRequest request = validRequest();
        request.getHeaderFields().get(0).setFieldName("renamed-header");

        CeEmissionActivityImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertTrue(result.isBlocking());
        assertTrue(result.getRowResults().isEmpty());
        verify(rowValidator).listEntryFields();
        verifyNoMoreInteractions(rowValidator);
    }

    @Test
    void failsFastWhenDerivedResolverBeanIsMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ICeEmissionActivityDerivedFieldResolver> resolverProvider = mock(ObjectProvider.class);
        when(resolverProvider.getIfAvailable()).thenReturn(null);

        CeEmissionActivityImportValidationServiceImpl service =
            new CeEmissionActivityImportValidationServiceImpl(resolverProvider);

        CeEmissionActivityImportValidationResult result = service.validateImport(validRequest());

        assertTrue(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(1, result.getRowResults().size());
        CeEmissionActivityValidationIssue issue = result.getRowResults().get(0).getIssues().get(0);
        assertEquals("DERIVED_RESOLVER_UNAVAILABLE", issue.getCode());
        assertEquals(9, issue.getRowNumber());
        assertEquals("sourceIdentificationCode", issue.getFieldCode());
        assertEquals(allHeader().get(0).getFieldName(), issue.getFieldName());
    }

    @Test
    void rejectsNullHeaderDescriptorInPlace() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = validRequest();
        request.setHeaderFields(new ArrayList<>(request.getHeaderFields()));
        request.getHeaderFields().set(0, null);

        CeEmissionActivityImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertTrue(result.isBlocking());
        assertTrue(result.getRowResults().isEmpty());
        assertEquals("INVALID_HEADER_COLUMN", result.getHeaderIssues().get(0).getCode());
    }

    @Test
    void rejectsTrailingNullHeaderDescriptor() {
        CeEmissionActivityImportValidationServiceImpl service = new CeEmissionActivityImportValidationServiceImpl(
            new CeEmissionActivityValidationServiceImpl(fakeResolver())
        );

        CeEmissionActivityImportValidationRequest request = validRequest();
        List<CeEmissionActivityFieldDescriptor> headerFields = new ArrayList<>(request.getHeaderFields());
        headerFields.add(null);
        request.setHeaderFields(headerFields);

        CeEmissionActivityImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertTrue(result.isBlocking());
        assertTrue(result.getRowResults().isEmpty());
        assertEquals(List.of("INVALID_HEADER_COLUMN"), headerIssueCodes(result));
    }

    private List<String> headerIssueCodes(CeEmissionActivityImportValidationResult result) {
        return result.getHeaderIssues().stream()
            .map(CeEmissionActivityValidationIssue::getCode)
            .toList();
    }

    private CeEmissionActivityImportValidationRequest validRequest() {
        CeEmissionActivityImportValidationRequest request = new CeEmissionActivityImportValidationRequest();
        request.setHeaderFields(entryHeader());
        request.setRows(List.of(row(values -> {
        })));
        return request;
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

    private List<CeEmissionActivityFieldDescriptor> entryHeader() {
        return CeEmissionActivityValidationServiceImpl.entryFieldDescriptors().stream()
            .map(this::copyHeader)
            .toList();
    }

    private List<CeEmissionActivityFieldDescriptor> allHeader() {
        return CeEmissionActivityValidationServiceImpl.allFieldDescriptors().stream()
            .map(this::copyHeader)
            .toList();
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
        values.put("activityPeriod", "2026-06");
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

    private String fieldValue(CeEmissionActivityValidationRequest row, String code) {
        return row.getFieldValues().stream()
            .filter(fieldValue -> code.equals(fieldValue.getFieldCode()))
            .findFirst()
            .map(CeEmissionActivityFieldValue::getValue)
            .orElse(null);
    }

    private String fieldValueFromResolved(CeEmissionActivityImportValidationResult result, String code) {
        return result.getRowResults().get(0).getResolvedDerivedFieldValues().stream()
            .filter(fieldValue -> code.equals(fieldValue.getFieldCode()))
            .findFirst()
            .map(CeEmissionActivityFieldValue::getValue)
            .orElse(null);
    }

    private MockMultipartFile xlsxFile(List<String> headers, List<List<String>> dataRows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        rows.addAll(dataRows);
        FastExcel.write(outputStream).sheet("emission_activity").doWrite(rows);
        return new MockMultipartFile(
            "file",
            "emission_activity.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            outputStream.toByteArray()
        );
    }

    private List<String> rowValues(String... values) {
        return List.of(values);
    }

    private List<String> blankRowValues() {
        return new ArrayList<>(Collections.nCopies(11, ""));
    }

    private ICeEmissionActivityDerivedFieldResolver fakeResolver() {
        return new ICeEmissionActivityDerivedFieldResolver() {
            @Override
            public Optional<CeEmissionActivityResolvedRow> resolve(String code) {
                if (!"SRC-001".equals(code)) {
                    return Optional.empty();
                }
                return Optional.of(resolvedRow());
            }

            @Override
            public List<CeEmissionActivityResolvedRow> resolveByEntryFields(String companyName, String factoryName, String scope,
                                                                    String scopeSubcategory, String sourceIdentificationName,
                                                                    String emissionSourceName) {
                if ("Company One".equals(companyName)
                    && "Factory One".equals(factoryName)
                    && "Scope 1".equals(scope)
                    && "Stationary Combustion".equals(scopeSubcategory)
                    && "Natural Gas".equals(emissionSourceName)) {
                    return List.of(resolvedRow());
                }
                return List.of();
            }

            private CeEmissionActivityResolvedRow resolvedRow() {
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
                return row;
            }
        };
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
}
