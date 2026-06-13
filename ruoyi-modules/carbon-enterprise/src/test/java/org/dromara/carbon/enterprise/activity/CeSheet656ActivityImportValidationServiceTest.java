package org.dromara.carbon.enterprise.activity;

import cn.idev.excel.FastExcel;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.service.ICeSheet656DerivedFieldResolver;
import org.dromara.carbon.enterprise.service.ICeSheet656ValidationService;
import org.dromara.carbon.enterprise.service.impl.CeSheet656ActivityImportValidationServiceImpl;
import org.dromara.carbon.enterprise.service.impl.CeSheet656ValidationServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
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
class CeSheet656ActivityImportValidationServiceTest {

    @Test
    void parsesXlsxRowsByHeaderCodeAndSkipsBlankRows() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        CeSheet656ImportValidationRequest request = service.parseImportFile(xlsxFile(
            frozenHeader().stream().map(CeSheet656FieldDescriptor::getSourceColumnCode).toList(),
            List.of(
                rowValues("SRC-001", "COMP-001", "Company One", "Factory One", "CAT-001", "Scope 1",
                    "Stationary Combustion", "Natural Gas Boiler", "Natural Gas", "Nm3",
                    "2026", "6", "2026-06-05", "12.5", "Production", "Meter", "Normal record", "EF-2026-001"),
                blankRowValues(),
                rowValues("SRC-001", "COMP-001", "Company One", "Factory One", "CAT-001", "Scope 1",
                    "Stationary Combustion", "Natural Gas Boiler", "Natural Gas", "Nm3",
                    "2026", "7", "2026-07-05", "18.5", "Production", "Meter", "Second record", "EF-2026-001")
            )
        ));

        assertEquals(18, request.getHeaderFields().size());
        assertEquals("PK_排放源识别编号", request.getHeaderFields().get(0).getSourceColumnName());
        assertEquals(2, request.getRows().size());
        assertEquals(2, request.getRows().get(0).getRowNumber());
        assertEquals(4, request.getRows().get(1).getRowNumber());
        assertEquals("12.5", fieldValue(request.getRows().get(0), "f014"));
        assertEquals("Second record", fieldValue(request.getRows().get(1), "f017"));
    }

    @Test
    void rejectsEmptyUploadFile() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.parseImportFile(new MockMultipartFile("file", "sheet_656.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0])));

        assertEquals("请上传非空的 sheet_656 Excel 文件", exception.getMessage());
    }

    @Test
    void rejectsNonXlsxUploadFile() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.parseImportFile(new MockMultipartFile("file", "sheet_656.csv", "text/csv", "a,b".getBytes())));

        assertEquals("sheet_656 仅支持上传 .xlsx 文件", exception.getMessage());
    }

    @Test
    void rejectsMissingRequiredHeaderDuringParse() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        List<String> headers = frozenHeader().stream()
            .map(CeSheet656FieldDescriptor::getSourceColumnName)
            .toList();

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.parseImportFile(xlsxFile(headers.subList(0, headers.size() - 1), List.of(
                rowValues("SRC-001", "COMP-001", "Company One", "Factory One", "CAT-001", "Scope 1",
                    "Stationary Combustion", "Natural Gas Boiler", "Natural Gas", "Nm3",
                    "2026", "6", "2026-06-05", "12.5", "Production", "Meter", "Normal record")
            ))));

        assertTrue(exception.getMessage().contains("缺少必要表头"));
        assertTrue(exception.getMessage().contains("FK_排放因子"));
    }

    @Test
    void validatesFrozenHeaderAndRows() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        CeSheet656ImportValidationResult result = service.validateImport(validRequest());

        assertTrue(result.isHeaderValid());
        assertTrue(result.isValid());
        assertFalse(result.isBlocking());
        assertEquals(1, result.getRowResults().size());
        assertTrue(result.getHeaderIssues().isEmpty());
        assertTrue(result.getRowResults().get(0).getIssues().isEmpty());
    }

    @Test
    void rejectsRenamedHeaderBeforeRowValidation() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        CeSheet656ImportValidationRequest request = validRequest();
        request.getHeaderFields().get(0).setSourceColumnName("renamed-header");

        CeSheet656ImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(List.of("HEADER_COLUMN_MISMATCH"), headerIssueCodes(result));
        assertTrue(result.getRowResults().isEmpty());
    }

    @Test
    void rowResultsPreserveSourceRowNumberAndColumnName() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        CeSheet656ImportValidationRequest request = validRequest();
        request.setRows(new ArrayList<>(request.getRows()));
        request.getRows().set(0, row(values -> values.put("f014", "0")));

        CeSheet656ImportValidationResult result = service.validateImport(request);

        assertTrue(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(9, result.getRowResults().get(0).getRowNumber());
        CeSheet656ValidationIssue issue = result.getRowResults().get(0).getIssues().get(0);
        assertEquals("f014", issue.getSourceColumnCode());
        assertEquals(9, issue.getRowNumber());
        assertEquals("INVALID_VALUE_DOMAIN", issue.getCode());
    }

    @Test
    void rejectsDuplicateRowNumbersBeforePersistCanUseThemAsKeys() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        CeSheet656ImportValidationRequest request = validRequest();
        request.setRows(List.of(row(values -> {
        }), row(values -> {
        })));

        CeSheet656ImportValidationResult result = service.validateImport(request);

        assertTrue(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(2, result.getRowResults().size());
        assertTrue(result.getRowResults().get(0).isValid());
        assertFalse(result.getRowResults().get(1).isValid());
        CeSheet656ValidationIssue issue = result.getRowResults().get(1).getIssues().get(0);
        assertEquals("DUPLICATE_ROW_NUMBER", issue.getCode());
        assertEquals(9, issue.getRowNumber());
        assertEquals("rowNumber", issue.getSourceColumnCode());
    }

    @Test
    void headerMismatchBlocksRowsAndDoesNotCallRowValidator() {
        ICeSheet656ValidationService rowValidator = mock(ICeSheet656ValidationService.class);
        when(rowValidator.listFrozenFields()).thenReturn(frozenHeader());

        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(rowValidator);
        CeSheet656ImportValidationRequest request = validRequest();
        request.getHeaderFields().get(0).setSourceColumnName("renamed-header");

        CeSheet656ImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertTrue(result.isBlocking());
        assertTrue(result.getRowResults().isEmpty());
        verify(rowValidator).listFrozenFields();
        verifyNoMoreInteractions(rowValidator);
    }

    @Test
    void failsFastWhenDerivedResolverBeanIsMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ICeSheet656DerivedFieldResolver> resolverProvider = mock(ObjectProvider.class);
        when(resolverProvider.getIfAvailable()).thenReturn(null);

        CeSheet656ActivityImportValidationServiceImpl service =
            new CeSheet656ActivityImportValidationServiceImpl(resolverProvider);

        CeSheet656ImportValidationResult result = service.validateImport(validRequest());

        assertTrue(result.isHeaderValid());
        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(1, result.getRowResults().size());
        CeSheet656ValidationIssue issue = result.getRowResults().get(0).getIssues().get(0);
        assertEquals("DERIVED_RESOLVER_UNAVAILABLE", issue.getCode());
        assertEquals(9, issue.getRowNumber());
        assertEquals("f001", issue.getSourceColumnCode());
        assertEquals(frozenHeader().get(0).getSourceColumnName(), issue.getSourceColumnName());
    }

    @Test
    void rejectsNullHeaderDescriptorInPlace() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        CeSheet656ImportValidationRequest request = validRequest();
        request.setHeaderFields(new ArrayList<>(request.getHeaderFields()));
        request.getHeaderFields().set(0, null);

        CeSheet656ImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertTrue(result.isBlocking());
        assertTrue(result.getRowResults().isEmpty());
        assertEquals("INVALID_HEADER_COLUMN", result.getHeaderIssues().get(0).getCode());
    }

    @Test
    void rejectsTrailingNullHeaderDescriptor() {
        CeSheet656ActivityImportValidationServiceImpl service = new CeSheet656ActivityImportValidationServiceImpl(
            new CeSheet656ValidationServiceImpl(fakeResolver())
        );

        CeSheet656ImportValidationRequest request = validRequest();
        List<CeSheet656FieldDescriptor> headerFields = new ArrayList<>(request.getHeaderFields());
        headerFields.add(null);
        request.setHeaderFields(headerFields);

        CeSheet656ImportValidationResult result = service.validateImport(request);

        assertFalse(result.isHeaderValid());
        assertTrue(result.isBlocking());
        assertTrue(result.getRowResults().isEmpty());
        assertEquals(List.of("INVALID_HEADER_COLUMN"), headerIssueCodes(result));
    }

    private List<String> headerIssueCodes(CeSheet656ImportValidationResult result) {
        return result.getHeaderIssues().stream()
            .map(CeSheet656ValidationIssue::getCode)
            .toList();
    }

    private CeSheet656ImportValidationRequest validRequest() {
        CeSheet656ImportValidationRequest request = new CeSheet656ImportValidationRequest();
        request.setHeaderFields(frozenHeader());
        request.setRows(List.of(row(values -> {
        })));
        return request;
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

    private List<CeSheet656FieldDescriptor> frozenHeader() {
        return CeSheet656ValidationServiceImpl.frozenFieldDescriptors().stream()
            .map(this::copyHeader)
            .toList();
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

    private String fieldValue(CeSheet656ValidationRequest row, String code) {
        return row.getFieldValues().stream()
            .filter(fieldValue -> code.equals(fieldValue.getSourceColumnCode()))
            .findFirst()
            .map(CeSheet656FieldValue::getValue)
            .orElse(null);
    }

    private MockMultipartFile xlsxFile(List<String> headers, List<List<String>> dataRows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        rows.addAll(dataRows);
        FastExcel.write(outputStream).sheet("sheet_656").doWrite(rows);
        return new MockMultipartFile(
            "file",
            "sheet_656.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            outputStream.toByteArray()
        );
    }

    private List<String> rowValues(String... values) {
        return List.of(values);
    }

    private List<String> blankRowValues() {
        return new ArrayList<>(Collections.nCopies(18, ""));
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
