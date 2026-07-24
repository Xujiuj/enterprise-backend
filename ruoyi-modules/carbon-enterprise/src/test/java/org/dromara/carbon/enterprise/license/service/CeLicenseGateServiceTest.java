package org.dromara.carbon.enterprise.license.service;

import org.dromara.carbon.enterprise.license.domain.CeLicenseGateResult;
import org.dromara.carbon.enterprise.license.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseStateService;
import org.dromara.carbon.enterprise.license.service.impl.CeLicenseGateServiceImpl;
import org.dromara.carbon.enterprise.vendor.client.CeVendorLicenseOpenClient;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorLicenseCurrentResponse;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeLicenseGateServiceTest {

    private static final String EXPECTED_INSTALL_ID = "INSTALL-ENTERPRISE-001";
    private static final Date EVALUATION_TIME = Date.from(Instant.parse("2026-06-05T00:00:00Z"));

    @Test
    void allowsCurrentValidLicense() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeVendorLicenseOpenClient vendorClient = activeVendorClient();
        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, vendorClient);

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("ALLOW", result.getDecision());
        assertEquals("VALID", result.getReason());
        assertSame(currentState, result.getLicenseState());
        verify(vendorClient).currentLicense(eq("LIC-TEST-VALID-001"), eq(EXPECTED_INSTALL_ID),
            eq("test-key-2026-01"), any());
    }

    @Test
    void cachesCurrentLicenseStateForRepeatedGateChecks() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, activeVendorClient());

        service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);
        service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME, "report-gate");

        verify(stateService, times(1)).queryCurrent();
    }

    @Test
    void allowsWhenRequiredFeatureIsPresent() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, activeVendorClient());

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME, "report-gate");

        assertEquals("ALLOW", result.getDecision());
        assertEquals("VALID", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesWhenRequiredFeatureIsMissing() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, activeVendorClient());

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME, "report-template-download");

        assertEquals("DENY", result.getDecision());
        assertEquals("FEATURE_NOT_ENABLED", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesWhenNoLicenseStateExists() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        when(stateService.queryCurrent()).thenReturn(null);

        CeVendorLicenseOpenClient vendorClient = activeVendorClient();
        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, vendorClient);

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("NO_VALID_LICENSE", result.getReason());
        assertNull(result.getLicenseState());
        verify(vendorClient, never()).currentLicense(any(), any(), any(), any());
    }

    @Test
    void deniesExpiredLicense() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        currentState.setValidTo(Date.from(Instant.parse("2026-06-04T23:59:59Z")));
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, activeVendorClient());

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("EXPIRED", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesClockRollback() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        currentState.setMaxObservedTime(Date.from(Instant.parse("2026-06-06T00:00:00Z")));
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, activeVendorClient());

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("CLOCK_ROLLBACK", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesInstallIdMismatch() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, activeVendorClient());

        CeLicenseGateResult result = service.evaluateCurrent("INSTALL-OTHER-999", EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("INSTALL_ID_MISMATCH", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesNonValidStoredState() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        currentState.setLicenseStatus("REVOKED");
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, activeVendorClient());

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("NO_VALID_LICENSE", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesWhenVendorBindingRejectsDirectlyWrittenState() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        when(stateService.queryCurrent()).thenReturn(currentState);
        CeVendorLicenseOpenClient vendorClient = mock(CeVendorLicenseOpenClient.class);
        when(vendorClient.currentLicense(any(), any(), any(), any()))
            .thenThrow(new ServiceException("license installId does not match"));
        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, vendorClient);

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("LICENSE_BINDING_FAILED", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesWhenVendorFeatureSnapshotDoesNotContainRequiredFeature() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        currentState.setFeatureCodes("capture,factor-sync,report-gate,report-template-download");
        when(stateService.queryCurrent()).thenReturn(currentState);
        CeVendorLicenseOpenClient vendorClient = mock(CeVendorLicenseOpenClient.class);
        CeVendorLicenseCurrentResponse response = activeVendorLicense();
        response.setFeatureCodes("[\"capture\",\"factor-sync\",\"report-gate\"]");
        when(vendorClient.currentLicense(any(), any(), any(), any())).thenReturn(response);
        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService, vendorClient);

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME,
            "report-template-download");

        assertEquals("DENY", result.getDecision());
        assertEquals("FEATURE_NOT_ENABLED", result.getReason());
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
        state.setFeatureCodes("capture,factor-sync,report-gate");
        state.setLicenseStatus("VALID");
        return state;
    }

    private CeVendorLicenseOpenClient activeVendorClient() {
        CeVendorLicenseOpenClient vendorClient = mock(CeVendorLicenseOpenClient.class);
        when(vendorClient.currentLicense(any(), any(), any(), any())).thenReturn(activeVendorLicense());
        return vendorClient;
    }

    private CeVendorLicenseCurrentResponse activeVendorLicense() {
        CeVendorLicenseCurrentResponse response = new CeVendorLicenseCurrentResponse();
        response.setLicenseId("LIC-TEST-VALID-001");
        response.setStatus("active");
        response.setFeatureCodes("capture,factor-sync,report-gate");
        return response;
    }
}
