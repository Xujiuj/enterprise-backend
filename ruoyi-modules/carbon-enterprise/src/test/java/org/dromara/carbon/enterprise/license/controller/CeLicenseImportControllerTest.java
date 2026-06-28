package org.dromara.carbon.enterprise.license.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.license.domain.CeLicenseImportResult;
import org.dromara.carbon.enterprise.license.service.CeLicenseInstallIdProvider;
import org.dromara.carbon.enterprise.license.service.CeLicensePublicKeyProvider;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseImportService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private CeLicenseInstallIdProvider installIdProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        licenseImportService = mock(ICeLicenseImportService.class);
        publicKeyProvider = mock(CeLicensePublicKeyProvider.class);
        installIdProvider = mock(CeLicenseInstallIdProvider.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CeLicenseImportController(licenseImportService, publicKeyProvider, installIdProvider))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void returnsCurrentInstallIdAsDataField() throws Exception {
        when(installIdProvider.getExpectedInstallId()).thenReturn(EXPECTED_INSTALL_ID);

        mockMvc.perform(get("/enterprise/license-import/install-id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.msg", is("操作成功")))
            .andExpect(jsonPath("$.data.expectedInstallId", is(EXPECTED_INSTALL_ID)));
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
            .andExpect(jsonPath("$.data.message", is("授权文件校验通过")))
            .andExpect(jsonPath("$.data.licenseState.licenseId", is("LIC-TEST-VALID-001")))
            .andExpect(jsonPath("$.data.licenseState.customerId", is("CUST-001")))
            .andExpect(jsonPath("$.data.licenseState.installId", is(EXPECTED_INSTALL_ID)))
            .andExpect(jsonPath("$.data.licenseState.keyId", is("test-key-2026-01")))
            .andExpect(jsonPath("$.data.licenseState.algorithm", is("RS256")))
            .andExpect(jsonPath("$.data.licenseState.schemaVersion", is("license.v1")))
            .andExpect(jsonPath("$.data.licenseState.licenseStatus", is("VALID")))
            .andExpect(jsonPath("$.data.licenseState.id").doesNotExist())
            .andExpect(jsonPath("$.data.licenseState.lastVerifiedTime", is(1780531200000L)))
            .andExpect(jsonPath("$.data.licenseState.maxObservedTime", is(1780531200000L)));

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
            .andExpect(jsonPath("$.msg", is("请求参数格式错误：授权导入请求包含不支持的字段：publicKeyPem")));

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
            .andExpect(jsonPath("$.msg", is("授权文件内容不能为空")));

        verify(licenseImportService, never()).importLicense(any(), any(), any(), any());
    }

    @Test
    void rejectsBlankExpectedInstallId() throws Exception {
        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(LICENSE_CONTENT, " ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(500)))
            .andExpect(jsonPath("$.msg", is("部署指纹不能为空")));

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
            case "SIGNATURE_INVALID" -> "授权文件签名校验失败";
            case "EXPIRED" -> "授权已过期";
            case "INSTALL_ID_MISMATCH" -> "授权文件的部署指纹与本机不匹配";
            case "CLOCK_ROLLBACK" -> "系统时间早于最近授权校验时间";
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
