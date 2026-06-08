package org.dromara.carbon.enterprise.activity;

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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeSheet656ActivityImportValidationServiceTest {

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
