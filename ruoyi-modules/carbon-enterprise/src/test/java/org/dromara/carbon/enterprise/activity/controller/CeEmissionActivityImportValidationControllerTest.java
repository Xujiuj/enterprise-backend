package org.dromara.carbon.enterprise.activity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityCaptureResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationResult;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityCaptureService;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityImportValidationService;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityValidationService;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

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
class CeEmissionActivityImportValidationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ICeEmissionActivityImportValidationService activityImportValidationService;
    private ICeEmissionActivityCaptureService activityCaptureService;
    private ICeEmissionActivityValidationService activityValidationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        activityImportValidationService = mock(ICeEmissionActivityImportValidationService.class);
        activityCaptureService = mock(ICeEmissionActivityCaptureService.class);
        activityValidationService = mock(ICeEmissionActivityValidationService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CeEmissionActivityImportValidationController(
                activityImportValidationService,
                activityCaptureService,
                activityValidationService
            ))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void returnsServerEntryFieldDescriptors() throws Exception {
        when(activityValidationService.listEntryFields()).thenReturn(List.of(field("companyName", "公司名称")));

        mockMvc.perform(get("/enterprise/activity-import/emission-activity/fields"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data[0].fieldCode", is("companyName")))
            .andExpect(jsonPath("$.data[0].fieldName", is("公司名称")));

        verify(activityValidationService).listEntryFields();
    }

    @Test
    void returnsValidateOnlyImportResults() throws Exception {
        when(activityImportValidationService.validateImport(any())).thenReturn(serviceResult());

        mockMvc.perform(post("/enterprise/activity-import/emission-activity/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CeEmissionActivityImportValidationRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.headerValid", is(false)))
            .andExpect(jsonPath("$.data.valid", is(false)))
            .andExpect(jsonPath("$.data.blocking", is(true)))
            .andExpect(jsonPath("$.data.headerIssues[0].code", is("HEADER_COLUMN_MISMATCH")))
            .andExpect(jsonPath("$.data.rowResults[0].rowNumber", is(9)))
            .andExpect(jsonPath("$.data.rowResults[0].issues[0].fieldCode", is("activityValue")));

        verify(activityImportValidationService).validateImport(any());
    }

    @Test
    void delegatesMultipartParseAndReturnsRequestPayload() throws Exception {
        when(activityImportValidationService.parseImportFile(any())).thenReturn(parsedRequest());

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "emission_activity.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "stub".getBytes()
        );

        mockMvc.perform(multipart("/enterprise/activity-import/emission-activity/parse-file").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.headerFields[0].fieldCode", is("companyName")))
            .andExpect(jsonPath("$.data.rows[0].rowNumber", is(2)))
            .andExpect(jsonPath("$.data.rows[0].fieldValues[0].value", is("Company One")));

        verify(activityImportValidationService).parseImportFile(any());
    }

    @Test
    void delegatesManualSaveAndReturnsCaptureResult() throws Exception {
        when(activityCaptureService.saveManual(any())).thenReturn(captureResult());

        mockMvc.perform(post("/enterprise/activity-import/emission-activity/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CeEmissionActivityValidationRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.persisted", is(true)))
            .andExpect(jsonPath("$.data.batchId", is(7001)))
            .andExpect(jsonPath("$.data.persistedRowCount", is(1)))
            .andExpect(jsonPath("$.data.validationResult.valid", is(true)));

        verify(activityCaptureService).saveManual(any());
    }

    @Test
    void delegatesImportRowsAndReturnsCaptureResult() throws Exception {
        when(activityCaptureService.importRows(any())).thenReturn(captureResult());

        mockMvc.perform(post("/enterprise/activity-import/emission-activity/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CeEmissionActivityImportValidationRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.persisted", is(true)))
            .andExpect(jsonPath("$.data.batchId", is(7001)))
            .andExpect(jsonPath("$.data.validationResult.headerValid", is(true)));

        verify(activityCaptureService).importRows(any());
    }

    private CeEmissionActivityCaptureResult captureResult() {
        CeEmissionActivityImportValidationResult validationResult = new CeEmissionActivityImportValidationResult();
        validationResult.setHeaderValid(true);
        validationResult.setValid(true);
        validationResult.setBlocking(false);
        validationResult.setHeaderIssues(List.of());
        validationResult.setRowResults(List.of());

        CeEmissionActivityCaptureResult result = new CeEmissionActivityCaptureResult();
        result.setPersisted(true);
        result.setBatchId(7001L);
        result.setPersistedRowCount(1);
        result.setValidationResult(validationResult);
        return result;
    }

    private CeEmissionActivityImportValidationResult serviceResult() {
        CeEmissionActivityValidationIssue headerIssue = new CeEmissionActivityValidationIssue();
        headerIssue.setSeverity("ERROR");
        headerIssue.setCode("HEADER_COLUMN_MISMATCH");
        headerIssue.setFieldCode("sourceIdentificationCode");
        headerIssue.setFieldName("sourceIdentificationCode");

        CeEmissionActivityValidationIssue rowIssue = new CeEmissionActivityValidationIssue();
        rowIssue.setSeverity("ERROR");
        rowIssue.setCode("INVALID_VALUE_DOMAIN");
        rowIssue.setRowNumber(9);
        rowIssue.setFieldCode("activityValue");
        rowIssue.setFieldName("activityValue");

        CeEmissionActivityValidationResult rowResult = new CeEmissionActivityValidationResult();
        rowResult.setRowNumber(9);
        rowResult.setValid(false);
        rowResult.setBlocking(true);
        rowResult.setDraftSavable(false);
        rowResult.setIssues(List.of(rowIssue));

        CeEmissionActivityImportValidationResult result = new CeEmissionActivityImportValidationResult();
        result.setHeaderValid(false);
        result.setValid(false);
        result.setBlocking(true);
        result.setHeaderIssues(List.of(headerIssue));
        result.setRowResults(List.of(rowResult));
        return result;
    }

    private CeEmissionActivityImportValidationRequest parsedRequest() {
        CeEmissionActivityFieldDescriptor headerField = field("companyName", "公司名称");

        CeEmissionActivityFieldValue fieldValue = new CeEmissionActivityFieldValue();
        fieldValue.setFieldCode("companyName");
        fieldValue.setFieldName("公司名称");
        fieldValue.setValue("Company One");

        CeEmissionActivityValidationRequest row = new CeEmissionActivityValidationRequest();
        row.setRowNumber(2);
        row.setFieldValues(List.of(fieldValue));

        CeEmissionActivityImportValidationRequest request = new CeEmissionActivityImportValidationRequest();
        request.setHeaderFields(List.of(headerField));
        request.setRows(List.of(row));
        return request;
    }

    private CeEmissionActivityFieldDescriptor field(String code, String name) {
        CeEmissionActivityFieldDescriptor headerField = new CeEmissionActivityFieldDescriptor();
        headerField.setFieldOrder(1);
        headerField.setFieldCode(code);
        headerField.setFieldName(name);
        return headerField;
    }

}
