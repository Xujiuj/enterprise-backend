package org.dromara.carbon.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResult;
import org.dromara.carbon.enterprise.service.CeLicensePublicKeyProvider;
import org.dromara.carbon.enterprise.service.ICeLicenseImportService;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class CeLicenseImportControllerTest {

    private static final String LICENSE_CONTENT = "{\"schemaVersion\":\"license.v1\"}";
    private static final String EXPECTED_INSTALL_ID = "INSTALL-ENTERPRISE-001";
    private static final String CONFIGURED_PUBLIC_KEY = "configured-public-key";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ICeLicenseImportService licenseImportService;
    private CeLicensePublicKeyProvider publicKeyProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        licenseImportService = mock(ICeLicenseImportService.class);
        publicKeyProvider = mock(CeLicensePublicKeyProvider.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CeLicenseImportController(licenseImportService, publicKeyProvider))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void importsLicenseWithConfiguredPublicKey() throws Exception {
        when(publicKeyProvider.getPublicKeyPem()).thenReturn(CONFIGURED_PUBLIC_KEY);
        when(licenseImportService.importLicense(eq(LICENSE_CONTENT), eq(CONFIGURED_PUBLIC_KEY),
            eq(EXPECTED_INSTALL_ID), any(Date.class))).thenReturn(CeLicenseImportResult.valid(validState()));

        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.valid", is(true)))
            .andExpect(jsonPath("$.data.status", is("VALID")))
            .andExpect(jsonPath("$.data.message", is("license is valid")))
            .andExpect(jsonPath("$.data.licenseState.licenseId", is("LIC-TEST-VALID-001")))
            .andExpect(jsonPath("$.data.licenseState.customerId", is("CUST-001")))
            .andExpect(jsonPath("$.data.licenseState.installId", is(EXPECTED_INSTALL_ID)))
            .andExpect(jsonPath("$.data.licenseState.keyId", is("test-key-2026-01")))
            .andExpect(jsonPath("$.data.licenseState.algorithm", is("RS256")))
            .andExpect(jsonPath("$.data.licenseState.schemaVersion", is("license.v1")))
            .andExpect(jsonPath("$.data.licenseState.licenseStatus", is("VALID")))
            .andExpect(jsonPath("$.data.licenseState.id").doesNotExist())
            .andExpect(jsonPath("$.data.licenseState.lastVerifiedTime").doesNotExist())
            .andExpect(jsonPath("$.data.licenseState.maxObservedTime").doesNotExist());

        verify(licenseImportService).importLicense(eq(LICENSE_CONTENT), eq(CONFIGURED_PUBLIC_KEY),
            eq(EXPECTED_INSTALL_ID), any(Date.class));
    }

    @Test
    void returnsPublicKeyUnavailableWithoutCallingImportService() throws Exception {
        when(publicKeyProvider.getPublicKeyPem()).thenReturn(" ");

        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.valid", is(false)))
            .andExpect(jsonPath("$.data.status", is("PUBLIC_KEY_UNAVAILABLE")))
            .andExpect(jsonPath("$.data.licenseState").doesNotExist());

        verify(licenseImportService, never()).importLicense(any(), any(), any(), any());
    }

    @Test
    void rejectsRequestSuppliedPublicKeyPem() throws Exception {
        when(publicKeyProvider.getPublicKeyPem()).thenReturn(CONFIGURED_PUBLIC_KEY);

        Map<String, Object> request = requestMap();
        request.put("publicKeyPem", "request-supplied-public-key");

        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(400)))
            .andExpect(jsonPath("$.msg", is("\u8bf7\u6c42\u53c2\u6570\u683c\u5f0f\u9519\u8bef\uff1aunsupported license import request field: publicKeyPem")));

        verify(licenseImportService, never()).importLicense(any(), any(), any(), any());
    }

    @Test
    void returnsBusinessFailureStatusFromImportService() throws Exception {
        assertBusinessFailureStatus("SIGNATURE_INVALID");
    }

    @Test
    void returnsExpiredStatusFromImportService() throws Exception {
        assertBusinessFailureStatus("EXPIRED");
    }

    @Test
    void returnsInstallIdMismatchStatusFromImportService() throws Exception {
        assertBusinessFailureStatus("INSTALL_ID_MISMATCH");
    }

    @Test
    void returnsClockRollbackStatusFromImportService() throws Exception {
        assertBusinessFailureStatus("CLOCK_ROLLBACK");
    }

    @Test
    void rejectsBlankLicenseContent() throws Exception {
        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(" ", EXPECTED_INSTALL_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(500)))
            .andExpect(jsonPath("$.msg", is("licenseContent cannot be blank")));

        verify(licenseImportService, never()).importLicense(any(), any(), any(), any());
    }

    @Test
    void rejectsBlankExpectedInstallId() throws Exception {
        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(LICENSE_CONTENT, " ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(500)))
            .andExpect(jsonPath("$.msg", is("expectedInstallId cannot be blank")));

        verify(licenseImportService, never()).importLicense(any(), any(), any(), any());
    }

    private String requestJson() throws Exception {
        return objectMapper.writeValueAsString(requestMap());
    }

    private String requestJson(String licenseContent, String expectedInstallId) throws Exception {
        return objectMapper.writeValueAsString(requestMap(licenseContent, expectedInstallId));
    }

    private Map<String, Object> requestMap() {
        return requestMap(LICENSE_CONTENT, EXPECTED_INSTALL_ID);
    }

    private Map<String, Object> requestMap(String licenseContent, String expectedInstallId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("licenseContent", licenseContent);
        request.put("expectedInstallId", expectedInstallId);
        return request;
    }

    private void assertBusinessFailureStatus(String status) throws Exception {
        when(publicKeyProvider.getPublicKeyPem()).thenReturn(CONFIGURED_PUBLIC_KEY);
        when(licenseImportService.importLicense(eq(LICENSE_CONTENT), eq(CONFIGURED_PUBLIC_KEY),
            eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(CeLicenseImportResult.invalid(status, "license import failed"));

        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.valid", is(false)))
            .andExpect(jsonPath("$.data.status", is(status)))
            .andExpect(jsonPath("$.data.message", is(expectedMessage(status))));
    }

    private String expectedMessage(String status) {
        return switch (status) {
            case "SIGNATURE_INVALID" -> "license signature is invalid";
            case "EXPIRED" -> "license has expired";
            case "INSTALL_ID_MISMATCH" -> "license installId does not match local installId";
            case "CLOCK_ROLLBACK" -> "system time is earlier than the last observed license verification time";
            default -> "license import failed";
        };
    }

    private CeLicenseState validState() {
        CeLicenseState state = new CeLicenseState();
        state.setId(1L);
        state.setLicenseId("LIC-TEST-VALID-001");
        state.setCustomerId("CUST-001");
        state.setInstallId(EXPECTED_INSTALL_ID);
        state.setKeyId("test-key-2026-01");
        state.setAlgorithm("RS256");
        state.setSchemaVersion("license.v1");
        state.setValidFrom(new Date(1767225600000L));
        state.setValidTo(new Date(1798761600000L));
        state.setLastVerifiedTime(new Date(1780531200000L));
        state.setMaxObservedTime(new Date(1780531200000L));
        state.setLicenseStatus("VALID");
        return state;
    }
}
