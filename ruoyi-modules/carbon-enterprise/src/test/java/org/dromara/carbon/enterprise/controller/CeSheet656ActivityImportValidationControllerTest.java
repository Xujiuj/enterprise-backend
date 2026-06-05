package org.dromara.carbon.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationResult;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityImportValidationService;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class CeSheet656ActivityImportValidationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ICeSheet656ActivityImportValidationService activityImportValidationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        activityImportValidationService = mock(ICeSheet656ActivityImportValidationService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CeSheet656ActivityImportValidationController(activityImportValidationService))
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

    private CeSheet656ImportValidationResult serviceResult() {
        CeSheet656ValidationIssue headerIssue = new CeSheet656ValidationIssue();
        headerIssue.setSeverity("ERROR");
        headerIssue.setCode("HEADER_COLUMN_MISMATCH");
        headerIssue.setSourceColumnCode("f001");
        headerIssue.setSourceColumnName("PK_排放源识别编号");

        CeSheet656ValidationIssue rowIssue = new CeSheet656ValidationIssue();
        rowIssue.setSeverity("ERROR");
        rowIssue.setCode("INVALID_VALUE_DOMAIN");
        rowIssue.setRowNumber(9);
        rowIssue.setSourceColumnCode("f014");
        rowIssue.setSourceColumnName("活动数据");

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
}
