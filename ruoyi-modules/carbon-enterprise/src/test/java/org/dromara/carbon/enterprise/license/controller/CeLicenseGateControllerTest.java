package org.dromara.carbon.enterprise.license.controller;

import org.dromara.carbon.enterprise.license.domain.CeLicenseGateResponse;
import org.dromara.carbon.enterprise.license.domain.CeLicenseGateResult;
import org.dromara.carbon.enterprise.license.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.license.service.CeLicenseInstallIdProvider;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseGateService;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class CeLicenseGateControllerTest {

    private static final String EXPECTED_INSTALL_ID = "INSTALL-ENTERPRISE-001";

    private ICeLicenseGateService licenseGateService;
    private CeLicenseInstallIdProvider installIdProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        licenseGateService = mock(ICeLicenseGateService.class);
        installIdProvider = mock(CeLicenseInstallIdProvider.class);
        when(installIdProvider.getExpectedInstallId()).thenReturn(EXPECTED_INSTALL_ID);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CeLicenseGateController(licenseGateService, installIdProvider))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void returnsAllowDecisionForCurrentValidLicense() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("ALLOW", "VALID", validState()));

        mockMvc.perform(get("/enterprise/license-gate/current")
                .param("expectedInstallId", EXPECTED_INSTALL_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.decision", is("ALLOW")))
            .andExpect(jsonPath("$.data.reason", is("VALID")))
            .andExpect(jsonPath("$.data.message", is("\u5f53\u524d\u6388\u6743\u6709\u6548")))
            .andExpect(jsonPath("$.data.licenseState.licenseId", is("LIC-TEST-VALID-001")))
            .andExpect(jsonPath("$.data.licenseState.installId", is(EXPECTED_INSTALL_ID)));
    }

    @Test
    void usesLocalInstallIdWhenRequestOmitsExpectedInstallId() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("DENY", "INSTALL_ID_MISMATCH", null));

        mockMvc.perform(get("/enterprise/license-gate/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.decision", is("DENY")))
            .andExpect(jsonPath("$.data.reason", is("INSTALL_ID_MISMATCH")))
            .andExpect(jsonPath("$.data.message", is("\u6388\u6743\u6587\u4ef6\u7684\u90e8\u7f72\u6307\u7eb9\u4e0e\u672c\u673a\u4e0d\u5339\u914d")));

        verify(installIdProvider).getExpectedInstallId();
        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class));
    }

    @Test
    void returnsDenyDecisionForMissingLicense() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("DENY", "NO_VALID_LICENSE", null));

        mockMvc.perform(get("/enterprise/license-gate/current")
                .param("expectedInstallId", EXPECTED_INSTALL_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.decision", is("DENY")))
            .andExpect(jsonPath("$.data.reason", is("NO_VALID_LICENSE")))
            .andExpect(jsonPath("$.data.message", is("\u5f53\u524d\u6ca1\u6709\u53ef\u7528\u7684\u4f01\u4e1a\u6388\u6743")))
            .andExpect(jsonPath("$.data.licenseState").doesNotExist());
    }

    private CeLicenseStateVo validState() {
        CeLicenseStateVo state = new CeLicenseStateVo();
        state.setLicenseId("LIC-TEST-VALID-001");
        state.setCustomerId("CUST-001");
        state.setInstallId(EXPECTED_INSTALL_ID);
        state.setKeyId("test-key-2026-01");
        state.setAlgorithm("RS256");
        state.setSchemaVersion("license.v1");
        state.setValidFrom(Date.from(Instant.parse("2026-01-01T00:00:00Z")));
        state.setValidTo(Date.from(Instant.parse("2026-12-31T00:00:00Z")));
        state.setLastVerifiedTime(Date.from(Instant.parse("2026-06-04T00:00:00Z")));
        state.setMaxObservedTime(Date.from(Instant.parse("2026-06-04T00:00:00Z")));
        state.setLicenseStatus("VALID");
        return state;
    }
}
