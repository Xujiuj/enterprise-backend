package org.dromara.carbon.enterprise.activity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ActivityCaptureResult;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ValidationResult;
import org.dromara.carbon.enterprise.shared.service.ICeSheet656ActivityCaptureService;
import org.dromara.carbon.enterprise.shared.service.ICeSheet656ActivityImportValidationService;
import org.dromara.carbon.enterprise.shared.service.ICeSheet656DerivedFieldResolver;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class CeSheet656ActivityImportValidationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ICeSheet656ActivityImportValidationService activityImportValidationService;
    private ICeSheet656ActivityCaptureService activityCaptureService;
    private ICeSheet656DerivedFieldResolver derivedFieldResolver;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        activityImportValidationService = mock(ICeSheet656ActivityImportValidationService.class);
        activityCaptureService = mock(ICeSheet656ActivityCaptureService.class);
        derivedFieldResolver = mock(ICeSheet656DerivedFieldResolver.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CeSheet656ActivityImportValidationController(
                activityImportValidationService,
                activityCaptureService,
                derivedFieldResolver
            ))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void returnsValidateOnlyImportResults() throws Exception {
        when(activityImportValidationService.validateImport(any())).thenReturn(serviceResult());

        mockMvc.perform(post("/enterprise/activity-import/sheet-656/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CeSheet656ImportValidationRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.headerValid", is(false)))
            .andExpect(jsonPath("$.data.valid", is(false)))
            .andExpect(jsonPath("$.data.blocking", is(true)))
            .andExpect(jsonPath("$.data.headerIssues[0].code", is("HEADER_COLUMN_MISMATCH")))
            .andExpect(jsonPath("$.data.rowResults[0].rowNumber", is(9)))
            .andExpect(jsonPath("$.data.rowResults[0].issues[0].sourceColumnCode", is("f014")));

        verify(activityImportValidationService).validateImport(any());
    }

    @Test
    void delegatesMultipartParseAndReturnsRequestPayload() throws Exception {
        when(activityImportValidationService.parseImportFile(any())).thenReturn(parsedRequest());

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sheet_656.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "stub".getBytes()
        );

        mockMvc.perform(multipart("/enterprise/activity-import/sheet-656/parse-file").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.headerFields[0].sourceColumnCode", is("f001")))
            .andExpect(jsonPath("$.data.rows[0].rowNumber", is(2)))
            .andExpect(jsonPath("$.data.rows[0].fieldValues[0].value", is("SRC-001")));

        verify(activityImportValidationService).parseImportFile(any());
    }

    @Test
    void delegatesManualSaveAndReturnsCaptureResult() throws Exception {
        when(activityCaptureService.saveManual(any())).thenReturn(captureResult());

        mockMvc.perform(post("/enterprise/activity-import/sheet-656/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CeSheet656ValidationRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.persisted", is(true)))
            .andExpect(jsonPath("$.data.batchId", is(7001)))
            .andExpect(jsonPath("$.data.persistedRowCount", is(1)))
            .andExpect(jsonPath("$.data.validationResult.valid", is(true)));

        verify(activityCaptureService).saveManual(any());
    }

    @Test
    void resolvesActivitySourceDerivedFields() throws Exception {
        when(derivedFieldResolver.resolve("SRC-001")).thenReturn(Optional.of(resolvedRow()));

        mockMvc.perform(get("/enterprise/activity-import/sheet-656/source/SRC-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.emissionSourceCode", is("SRC-001")))
            .andExpect(jsonPath("$.data.companyCode", is("COMP-001")))
            .andExpect(jsonPath("$.data.companyName", is("Company One")))
            .andExpect(jsonPath("$.data.factoryName", is("Factory One")))
            .andExpect(jsonPath("$.data.emissionSourceCategoryCode", is("CAT-001")))
            .andExpect(jsonPath("$.data.scope", is("Scope 1")))
            .andExpect(jsonPath("$.data.scopeSubcategory", is("Stationary Combustion")))
            .andExpect(jsonPath("$.data.emissionSourceIdentity", is("Natural Gas Boiler")))
            .andExpect(jsonPath("$.data.emissionSourceName", is("Natural Gas")))
            .andExpect(jsonPath("$.data.unit", is("Nm3")))
            .andExpect(jsonPath("$.data.emissionFactorCode", is("EF-2026-001")));

        verify(derivedFieldResolver).resolve("SRC-001");
    }

    @Test
    void delegatesImportRowsAndReturnsCaptureResult() throws Exception {
        when(activityCaptureService.importRows(any())).thenReturn(captureResult());

        mockMvc.perform(post("/enterprise/activity-import/sheet-656/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CeSheet656ImportValidationRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.persisted", is(true)))
            .andExpect(jsonPath("$.data.batchId", is(7001)))
            .andExpect(jsonPath("$.data.validationResult.headerValid", is(true)));

        verify(activityCaptureService).importRows(any());
    }

    private CeSheet656ActivityCaptureResult captureResult() {
        CeSheet656ImportValidationResult validationResult = new CeSheet656ImportValidationResult();
        validationResult.setHeaderValid(true);
        validationResult.setValid(true);
        validationResult.setBlocking(false);
        validationResult.setHeaderIssues(List.of());
        validationResult.setRowResults(List.of());

        CeSheet656ActivityCaptureResult result = new CeSheet656ActivityCaptureResult();
        result.setPersisted(true);
        result.setBatchId(7001L);
        result.setPersistedRowCount(1);
        result.setValidationResult(validationResult);
        return result;
    }

    private CeSheet656ImportValidationResult serviceResult() {
        CeSheet656ValidationIssue headerIssue = new CeSheet656ValidationIssue();
        headerIssue.setSeverity("ERROR");
        headerIssue.setCode("HEADER_COLUMN_MISMATCH");
        headerIssue.setSourceColumnCode("f001");
        headerIssue.setSourceColumnName("f001");

        CeSheet656ValidationIssue rowIssue = new CeSheet656ValidationIssue();
        rowIssue.setSeverity("ERROR");
        rowIssue.setCode("INVALID_VALUE_DOMAIN");
        rowIssue.setRowNumber(9);
        rowIssue.setSourceColumnCode("f014");
        rowIssue.setSourceColumnName("f014");

        CeSheet656ValidationResult rowResult = new CeSheet656ValidationResult();
        rowResult.setRowNumber(9);
        rowResult.setValid(false);
        rowResult.setBlocking(true);
        rowResult.setDraftSavable(false);
        rowResult.setIssues(List.of(rowIssue));

        CeSheet656ImportValidationResult result = new CeSheet656ImportValidationResult();
        result.setHeaderValid(false);
        result.setValid(false);
        result.setBlocking(true);
        result.setHeaderIssues(List.of(headerIssue));
        result.setRowResults(List.of(rowResult));
        return result;
    }

    private CeSheet656ImportValidationRequest parsedRequest() {
        CeSheet656FieldDescriptor headerField = new CeSheet656FieldDescriptor();
        headerField.setFieldOrder(1);
        headerField.setSourceColumnCode("f001");
        headerField.setSourceColumnName("PK_排放源识别编号");

        CeSheet656FieldValue fieldValue = new CeSheet656FieldValue();
        fieldValue.setSourceColumnCode("f001");
        fieldValue.setSourceColumnName("PK_排放源识别编号");
        fieldValue.setValue("SRC-001");

        CeSheet656ValidationRequest row = new CeSheet656ValidationRequest();
        row.setRowNumber(2);
        row.setFieldValues(List.of(fieldValue));

        CeSheet656ImportValidationRequest request = new CeSheet656ImportValidationRequest();
        request.setHeaderFields(List.of(headerField));
        request.setRows(List.of(row));
        return request;
    }

    private CeSheet656ResolvedRow resolvedRow() {
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
        return row;
    }
}
