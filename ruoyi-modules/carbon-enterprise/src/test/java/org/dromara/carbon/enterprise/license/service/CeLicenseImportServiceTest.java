package org.dromara.carbon.enterprise.license.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.dimension.domain.CeDimensionSyncResponse;
import org.dromara.carbon.enterprise.factor.domain.CeFactorSyncResponse;
import org.dromara.carbon.enterprise.license.domain.CeLicenseImportResult;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.shared.service.ICeDimensionSyncService;
import org.dromara.carbon.enterprise.shared.service.ICeFactorSyncService;
import org.dromara.carbon.enterprise.license.service.impl.CeLicenseImportServiceImpl;
import org.dromara.carbon.enterprise.vendor.client.CeVendorLicenseOpenClient;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorLicenseCurrentResponse;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeLicenseImportServiceTest {

    private static final String EXPECTED_INSTALL_ID = "INSTALL-ENTERPRISE-001";
    private static final Date VERIFICATION_TIME = Date.from(Instant.parse("2026-06-04T00:00:00Z"));
    private static final Date MAX_OBSERVED_TIME = Date.from(Instant.parse("2026-06-05T00:00:00Z"));

    private CeLicenseImportServiceImpl service;
    private ObjectMapper objectMapper;
    private CeLicenseStateMapper licenseStateMapper;
    private ICeDimensionSyncService dimensionSyncService;
    private ICeFactorSyncService factorSyncService;
    private CeVendorLicenseOpenClient vendorLicenseOpenClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        licenseStateMapper = mock(CeLicenseStateMapper.class);
        dimensionSyncService = mock(ICeDimensionSyncService.class);
        factorSyncService = mock(ICeFactorSyncService.class);
        vendorLicenseOpenClient = mock(CeVendorLicenseOpenClient.class);
        service = new CeLicenseImportServiceImpl(licenseStateMapper, objectMapper,
            dimensionSyncService, factorSyncService, vendorLicenseOpenClient);
    }

    @Test
    void verifiesValidLicenseVector() throws Exception {
        CeLicenseImportResult result = verifyVector("valid-license.json", null);

        assertTrue(result.isValid());
        assertEquals("VALID", result.getStatus());
        assertNotNull(result.getLicenseState());
        assertEquals("LIC-TEST-VALID-001", result.getLicenseState().getLicenseId());
        assertEquals(EXPECTED_INSTALL_ID, result.getLicenseState().getInstallId());
        assertEquals("VALID", result.getLicenseState().getLicenseStatus());
        assertEquals(VERIFICATION_TIME, result.getLicenseState().getMaxObservedTime());
        assertEquals("capture,factor-sync,report-gate", result.getLicenseState().getFeatureCodes());
        assertEquals(64, result.getLicenseState().getPayloadDigest().length());
        assertTrue(result.getLicenseState().getCurrentSummary().contains("enterprise"));
        assertTrue(result.getLicenseState().getCurrentSummary().contains("capture,factor-sync,report-gate"));
    }

    @Test
    void rejectsTamperedLicenseVector() throws Exception {
        CeLicenseImportResult result = verifyVector("tampered-license.json", null);

        assertEquals("SIGNATURE_INVALID", result.getStatus());
    }

    @Test
    void rejectsExpiredLicenseVector() throws Exception {
        CeLicenseImportResult result = verifyVector("expired-license.json", null);

        assertEquals("EXPIRED", result.getStatus());
    }

    @Test
    void rejectsInstallMismatchLicenseVector() throws Exception {
        CeLicenseImportResult result = verifyVector("install-mismatch-license.json", null);

        assertEquals("INSTALL_ID_MISMATCH", result.getStatus());
    }

    @Test
    void rejectsClockRollbackLicenseVector() throws Exception {
        CeLicenseImportResult result = verifyVector("clock-rollback-license.json", MAX_OBSERVED_TIME);

        assertEquals("CLOCK_ROLLBACK", result.getStatus());
    }

    @Test
    void importLicenseConfirmsVendorBindingBeforePersisting() throws Exception {
        givenSuccessfulImportDependencies();

        CeLicenseImportResult result = service.importLicense(readVector("valid-license.json"), readPublicKey(),
            EXPECTED_INSTALL_ID, VERIFICATION_TIME);

        assertTrue(result.isValid());
        InOrder inOrder = inOrder(vendorLicenseOpenClient, licenseStateMapper);
        inOrder.verify(vendorLicenseOpenClient).currentLicense(eq("LIC-TEST-VALID-001"), eq(EXPECTED_INSTALL_ID),
            eq("test-key-2026-01"), any());
        inOrder.verify(licenseStateMapper).insert(any(CeLicenseState.class));
    }

    @Test
    void importLicenseDoesNotPersistWhenVendorBindingFails() throws Exception {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of());
        when(vendorLicenseOpenClient.currentLicense(any(), any(), any(), any()))
            .thenThrow(new ServiceException("license installId does not match"));

        CeLicenseImportResult result = service.importLicense(readVector("valid-license.json"), readPublicKey(),
            EXPECTED_INSTALL_ID, VERIFICATION_TIME);

        assertEquals("LICENSE_BINDING_FAILED", result.getStatus());
        verify(licenseStateMapper, never()).insert(any(CeLicenseState.class));
        verify(licenseStateMapper, never()).updateById(any(CeLicenseState.class));
    }

    @Test
    void importLicenseUpdatesExistingMatchingStateInsteadOfInsertingDuplicate() throws Exception {
        givenSuccessfulImportDependencies();
        CeLicenseState existing = new CeLicenseState();
        existing.setId(99L);
        when(licenseStateMapper.selectOne(any(), eq(false))).thenReturn(existing);

        CeLicenseImportResult result = service.importLicense(readVector("valid-license.json"), readPublicKey(),
            EXPECTED_INSTALL_ID, VERIFICATION_TIME);

        assertTrue(result.isValid());
        verify(licenseStateMapper, never()).insert(any(CeLicenseState.class));
        verify(licenseStateMapper).updateById(any(CeLicenseState.class));
    }

    private CeLicenseImportResult verifyVector(String vectorFile, Date maxObservedTime) throws Exception {
        return service.verifyLicense(readVector(vectorFile), readPublicKey(), EXPECTED_INSTALL_ID,
            VERIFICATION_TIME, maxObservedTime);
    }

    private void givenSuccessfulImportDependencies() {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of());
        when(licenseStateMapper.selectOne(any(), eq(false))).thenReturn(null);
        when(vendorLicenseOpenClient.currentLicense(any(), any(), any(), any())).thenReturn(activeVendorLicense());
        when(dimensionSyncService.syncAllVendorDimensions()).thenReturn(List.of(successDimensionSync()));
        when(factorSyncService.syncCurrentLicenseFactors()).thenReturn(successFactorSync());
    }

    private CeVendorLicenseCurrentResponse activeVendorLicense() {
        CeVendorLicenseCurrentResponse response = new CeVendorLicenseCurrentResponse();
        response.setLicenseId("LIC-TEST-VALID-001");
        response.setStatus("active");
        return response;
    }

    private CeDimensionSyncResponse successDimensionSync() {
        CeDimensionSyncResponse response = new CeDimensionSyncResponse();
        response.setSuccess(true);
        response.setRecordCount(1);
        return response;
    }

    private CeFactorSyncResponse successFactorSync() {
        CeFactorSyncResponse response = new CeFactorSyncResponse();
        response.setVersionCode("V1");
        response.setChanged(false);
        response.setRecordCount(1);
        return response;
    }

    private String readVector(String vectorFile) throws Exception {
        ClassPathResource resource = new ClassPathResource("license-vectors/" + vectorFile);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String readPublicKey() throws Exception {
        ClassPathResource resource = new ClassPathResource("license-vectors/manifest.json");
        JsonNode publicKeyPem = objectMapper.readTree(resource.getInputStream()).get("publicKeyPem");
        List<String> lines = objectMapper.convertValue(publicKeyPem,
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        return lines.stream().collect(Collectors.joining("\n"));
    }
}
