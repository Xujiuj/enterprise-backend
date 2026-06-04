package org.dromara.carbon.enterprise.license;

import org.dromara.carbon.enterprise.domain.license.CeLicenseGateResult;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.dromara.carbon.enterprise.service.impl.CeLicenseGateServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
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

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService);

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("ALLOW", result.getDecision());
        assertEquals("VALID", result.getReason());
        assertSame(currentState, result.getLicenseState());
    }

    @Test
    void deniesWhenNoLicenseStateExists() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        when(stateService.queryCurrent()).thenReturn(null);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService);

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("NO_VALID_LICENSE", result.getReason());
        assertNull(result.getLicenseState());
    }

    @Test
    void deniesExpiredLicense() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        CeLicenseStateVo currentState = validState();
        currentState.setValidTo(Date.from(Instant.parse("2026-06-04T23:59:59Z")));
        when(stateService.queryCurrent()).thenReturn(currentState);

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService);

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

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService);

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

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService);

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

        CeLicenseGateServiceImpl service = new CeLicenseGateServiceImpl(stateService);

        CeLicenseGateResult result = service.evaluateCurrent(EXPECTED_INSTALL_ID, EVALUATION_TIME);

        assertEquals("DENY", result.getDecision());
        assertEquals("NO_VALID_LICENSE", result.getReason());
        assertSame(currentState, result.getLicenseState());
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
