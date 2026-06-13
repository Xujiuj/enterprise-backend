package org.dromara.carbon.enterprise.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.carbon.enterprise.service.ICeReportTemplateFileService;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class CeReportTemplateFileControllerTest {

    private ICeReportTemplateFileService reportTemplateFileService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reportTemplateFileService = mock(ICeReportTemplateFileService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CeReportTemplateFileController(reportTemplateFileService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void downloadDelegatesToService() throws Exception {
        byte[] body = "pbix-template".getBytes(StandardCharsets.UTF_8);
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("download-filename", "template.pbix");
            response.getOutputStream().write(body);
            return null;
        }).when(reportTemplateFileService).download(eq(7L), any(HttpServletResponse.class));

        mockMvc.perform(get("/enterprise/report-template-file/download/{id}", 7L))
            .andExpect(status().isOk())
            .andExpect(header().string("download-filename", "template.pbix"))
            .andExpect(content().bytes(body));

        verify(reportTemplateFileService).download(eq(7L), any(HttpServletResponse.class));
    }
}
