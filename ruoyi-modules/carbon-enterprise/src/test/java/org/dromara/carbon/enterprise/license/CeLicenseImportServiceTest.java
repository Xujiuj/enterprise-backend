package org.dromara.carbon.enterprise.license;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResult;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.impl.CeLicenseImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Tag("dev")
class CeLicenseImportServiceTest {

    private static final String EXPECTED_INSTALL_ID = "INSTALL-ENTERPRISE-001";
    private static final Date VERIFICATION_TIME = Date.from(Instant.parse("2026-06-04T00:00:00Z"));
    private static final Date MAX_OBSERVED_TIME = Date.from(Instant.parse("2026-06-05T00:00:00Z"));

    private CeLicenseImportServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new CeLicenseImportServiceImpl(mock(CeLicenseStateMapper.class), objectMapper);
    }

    @Test
    void verifiesValidLicenseVector() throws Exception {
        CeLicenseImportResult result = verify("valid-license.json", null);

        assertTrue(result.isValid());
        assertEquals("VALID", result.getStatus());
        assertNotNull(result.getLicenseState());
        assertEquals("LIC-TEST-VALID-001", result.getLicenseState().getLicenseId());
        assertEquals(EXPECTED_INSTALL_ID, result.getLicenseState().getInstallId());
        assertEquals("VALID", result.getLicenseState().getLicenseStatus());
        assertEquals(VERIFICATION_TIME, result.getLicenseState().getMaxObservedTime());
    }

    @Test
    void rejectsTamperedLicenseVector() throws Exception {
        CeLicenseImportResult result = verify("tampered-license.json", null);

        assertEquals("SIGNATURE_INVALID", result.getStatus());
    }

    @Test
    void rejectsExpiredLicenseVector() throws Exception {
        CeLicenseImportResult result = verify("expired-license.json", null);

        assertEquals("EXPIRED", result.getStatus());
    }

    @Test
    void rejectsInstallMismatchLicenseVector() throws Exception {
        CeLicenseImportResult result = verify("install-mismatch-license.json", null);

        assertEquals("INSTALL_ID_MISMATCH", result.getStatus());
    }

    @Test
    void rejectsClockRollbackLicenseVector() throws Exception {
        CeLicenseImportResult result = verify("clock-rollback-license.json", MAX_OBSERVED_TIME);

        assertEquals("CLOCK_ROLLBACK", result.getStatus());
    }

    private CeLicenseImportResult verify(String vectorFile, Date maxObservedTime) throws Exception {
        return service.verifyLicense(readVector(vectorFile), readPublicKey(), EXPECTED_INSTALL_ID,
            VERIFICATION_TIME, maxObservedTime);
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
