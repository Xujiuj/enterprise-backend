package org.dromara.carbon.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.config.CeLicenseGateWebMvcConfigurer;
import org.dromara.carbon.enterprise.domain.license.CeLicenseGateResult;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResult;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldVo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldValueVo;
import org.dromara.carbon.enterprise.interceptor.CeLicenseGateInterceptor;
import org.dromara.carbon.enterprise.service.ICeActivityDataService;
import org.dromara.carbon.enterprise.service.ICeDimensionRecordService;
import org.dromara.carbon.enterprise.service.ICeEmissionSourceService;
import org.dromara.carbon.enterprise.service.ICeFactorConfirmationService;
import org.dromara.carbon.enterprise.service.CeLicenseInstallIdProvider;
import org.dromara.carbon.enterprise.service.CeLicensePublicKeyProvider;
import org.dromara.carbon.enterprise.service.ICeGreenPowerCertificateService;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldService;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldValueService;
import org.dromara.carbon.enterprise.service.ICeIntensityMetricService;
import org.dromara.carbon.enterprise.service.ICeLicenseGateService;
import org.dromara.carbon.enterprise.service.ICeLicenseImportService;
import org.dromara.carbon.enterprise.service.ICeReportTemplateFileService;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = CeLicenseGateWebMvcConfigurerTest.TestConfig.class)
class CeLicenseGateWebMvcConfigurerTest {

    private static final String EXPECTED_INSTALL_ID = "INSTALL-ENTERPRISE-001";
    private static final List<String> NEWLY_PROTECTED_ROUTE_LISTS = Arrays.asList(
        "/enterprise/activity-data/list",
        "/enterprise/emission-source/list",
        "/enterprise/extension-field-value/list",
        "/enterprise/green-power-certificate/list",
        "/enterprise/factor-confirmation/list",
        "/enterprise/intensity-metric/list",
        "/enterprise/report-template-file/list",
        "/enterprise/dimension-record/list",
        "/enterprise/data-validation/dashboard"
    );

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ICeLicenseGateService licenseGateService;

    @Autowired
    private CeLicenseInstallIdProvider installIdProvider;

    @Autowired
    private ICeExtensionFieldService extensionFieldService;

    @Autowired
    private ICeExtensionFieldValueService extensionFieldValueService;

    @Autowired
    private ICeLicenseImportService licenseImportService;

    @Autowired
    private CeLicensePublicKeyProvider publicKeyProvider;

    @Autowired
    private ICeActivityDataService activityDataService;

    @Autowired
    private ICeEmissionSourceService emissionSourceService;

    @Autowired
    private ICeGreenPowerCertificateService greenPowerCertificateService;

    @Autowired
    private ICeFactorConfirmationService factorConfirmationService;

    @Autowired
    private ICeIntensityMetricService intensityMetricService;

    @Autowired
    private ICeReportTemplateFileService reportTemplateFileService;

    @Autowired
    private ICeDimensionRecordService dimensionRecordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(
            licenseGateService,
            installIdProvider,
            extensionFieldService,
            extensionFieldValueService,
            licenseImportService,
            publicKeyProvider,
            activityDataService,
            emissionSourceService,
            greenPowerCertificateService,
            factorConfirmationService,
            intensityMetricService,
            reportTemplateFileService,
            dimensionRecordService
        );
        when(installIdProvider.getExpectedInstallId()).thenReturn(EXPECTED_INSTALL_ID);
    }

    @Test
    void allowsProtectedRouteThroughRegisteredConfigWhenGateIsOpen() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("ALLOW", "VALID", null));
        when(extensionFieldService.queryPageList(any(), any()))
            .thenReturn(new TableDataInfo<>(Collections.<CeExtensionFieldVo>emptyList(), 0));

        mockMvc.perform(get("/enterprise/extension-field/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.rows").isArray())
            .andExpect(jsonPath("$.total", is(0)));

        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class));
        verify(extensionFieldService).queryPageList(any(), any());
    }

    @Test
    void allowsExtensionFieldValueRouteThroughRegisteredConfigWhenGateIsOpen() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("ALLOW", "VALID", null));
        when(extensionFieldValueService.queryPageList(any(), any()))
            .thenReturn(new TableDataInfo<>(Collections.<CeExtensionFieldValueVo>emptyList(), 0));

        mockMvc.perform(get("/enterprise/extension-field-value/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.rows").isArray())
            .andExpect(jsonPath("$.total", is(0)));

        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class));
        verify(extensionFieldValueService).queryPageList(any(), any());
    }

    @Test
    void deniesProtectedRouteWithRealHttp403AndNoLicenseStateLeak() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("DENY", "EXPIRED", null));

        mockMvc.perform(get("/enterprise/extension-field/list"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().encoding("UTF-8"))
            .andExpect(jsonPath("$.code", is(403)))
            .andExpect(jsonPath("$.msg", is("enterprise license gate denied access")))
            .andExpect(jsonPath("$.data.errorCode", is("ENTERPRISE_LICENSE_GATE_DENIED")))
            .andExpect(jsonPath("$.data.gate.decision", is("DENY")))
            .andExpect(jsonPath("$.data.gate.reason", is("EXPIRED")))
            .andExpect(jsonPath("$.data.gate.message", is("license has expired")))
            .andExpect(jsonPath("$.data.gate.licenseState").doesNotExist());
    }

    @Test
    void deniesNewlyProtectedCrudRoutesWhenLicenseHasExpired() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("DENY", "EXPIRED", null));
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), any()))
            .thenReturn(new CeLicenseGateResult("DENY", "EXPIRED", null));

        for (String route : NEWLY_PROTECTED_ROUTE_LISTS) {
            mockMvc.perform(get(route))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code", is(403)))
                .andExpect(jsonPath("$.data.errorCode", is("ENTERPRISE_LICENSE_GATE_DENIED")))
                .andExpect(jsonPath("$.data.gate.reason", is("EXPIRED")));
        }

        verifyNoInteractions(
            activityDataService,
            emissionSourceService,
            extensionFieldValueService,
            greenPowerCertificateService,
            factorConfirmationService,
            intensityMetricService,
            reportTemplateFileService,
            dimensionRecordService
        );
    }

    @Test
    void passesReportTemplateRoutesThroughFeatureGate() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-template-download")))
            .thenReturn(new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", null));

        mockMvc.perform(get("/enterprise/report-template-file/list"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.data.gate.reason", is("FEATURE_NOT_ENABLED")))
            .andExpect(jsonPath("$.data.gate.message", is("license does not include the required feature")));

        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-template-download"));
        verifyNoInteractions(reportTemplateFileService);
    }

    @Test
    void passesReportGateRoutesThroughFeatureGate() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-gate")))
            .thenReturn(new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", null));

        mockMvc.perform(get("/enterprise/data-validation/dashboard"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.data.gate.reason", is("FEATURE_NOT_ENABLED")));

        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-gate"));
        verifyNoInteractions(activityDataService);
    }

    @Test
    void keepsExemptRouteOpenThroughRegisteredConfig() throws Exception {
        when(publicKeyProvider.getPublicKeyPem()).thenReturn("configured-public-key");
        when(licenseImportService.importLicense(any(), any(), any(), any()))
            .thenReturn(CeLicenseImportResult.invalid("EXPIRED", "license import failed"));

        mockMvc.perform(post("/enterprise/license-import/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"licenseContent":"{\\"schemaVersion\\":\\"license.v1\\"}","expectedInstallId":"INSTALL-ENTERPRISE-001"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.valid", is(false)))
            .andExpect(jsonPath("$.data.status", is("EXPIRED")))
            .andExpect(jsonPath("$.data.message", is("license has expired")));

        verifyNoInteractions(licenseGateService);
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ICeLicenseGateService licenseGateService() {
            return mock(ICeLicenseGateService.class);
        }

        @Bean
        CeLicenseInstallIdProvider installIdProvider() {
            return mock(CeLicenseInstallIdProvider.class);
        }

        @Bean
        ICeExtensionFieldService extensionFieldService() {
            return mock(ICeExtensionFieldService.class);
        }

        @Bean
        ICeExtensionFieldValueService extensionFieldValueService() {
            return mock(ICeExtensionFieldValueService.class);
        }

        @Bean
        ICeActivityDataService activityDataService() {
            return mock(ICeActivityDataService.class);
        }

        @Bean
        ICeEmissionSourceService emissionSourceService() {
            return mock(ICeEmissionSourceService.class);
        }

        @Bean
        ICeGreenPowerCertificateService greenPowerCertificateService() {
            return mock(ICeGreenPowerCertificateService.class);
        }

        @Bean
        ICeFactorConfirmationService factorConfirmationService() {
            return mock(ICeFactorConfirmationService.class);
        }

        @Bean
        ICeIntensityMetricService intensityMetricService() {
            return mock(ICeIntensityMetricService.class);
        }

        @Bean
        ICeReportTemplateFileService reportTemplateFileService() {
            return mock(ICeReportTemplateFileService.class);
        }

        @Bean
        ICeDimensionRecordService dimensionRecordService() {
            return mock(ICeDimensionRecordService.class);
        }

        @Bean
        ICeLicenseImportService licenseImportService() {
            return mock(ICeLicenseImportService.class);
        }

        @Bean
        CeLicensePublicKeyProvider publicKeyProvider() {
            return mock(CeLicensePublicKeyProvider.class);
        }

        @Bean
        CeLicenseGateInterceptor ceLicenseGateInterceptor(ICeLicenseGateService licenseGateService,
                                                          CeLicenseInstallIdProvider installIdProvider,
                                                          ObjectMapper objectMapper) {
            return new CeLicenseGateInterceptor(licenseGateService, installIdProvider, objectMapper);
        }

        @Bean
        CeLicenseGateWebMvcConfigurer ceLicenseGateWebMvcConfigurer(CeLicenseGateInterceptor interceptor) {
            return new CeLicenseGateWebMvcConfigurer(interceptor);
        }

        @Bean
        CeExtensionFieldController ceExtensionFieldController(ICeExtensionFieldService extensionFieldService) {
            return new CeExtensionFieldController(extensionFieldService);
        }

        @Bean
        CeExtensionFieldValueController ceExtensionFieldValueController(ICeExtensionFieldValueService extensionFieldValueService) {
            return new CeExtensionFieldValueController(extensionFieldValueService);
        }

        @Bean
        CeActivityDataController ceActivityDataController(ICeActivityDataService activityDataService) {
            return new CeActivityDataController(activityDataService);
        }

        @Bean
        CeDataValidationController ceDataValidationController(ICeActivityDataService activityDataService) {
            return new CeDataValidationController(activityDataService);
        }

        @Bean
        CeEmissionSourceController ceEmissionSourceController(ICeEmissionSourceService emissionSourceService) {
            return new CeEmissionSourceController(emissionSourceService);
        }

        @Bean
        CeGreenPowerCertificateController ceGreenPowerCertificateController(
            ICeGreenPowerCertificateService greenPowerCertificateService
        ) {
            return new CeGreenPowerCertificateController(greenPowerCertificateService);
        }

        @Bean
        CeFactorConfirmationController ceFactorConfirmationController(
            ICeFactorConfirmationService factorConfirmationService
        ) {
            return new CeFactorConfirmationController(factorConfirmationService);
        }

        @Bean
        CeIntensityMetricController ceIntensityMetricController(ICeIntensityMetricService intensityMetricService) {
            return new CeIntensityMetricController(intensityMetricService);
        }

        @Bean
        CeReportTemplateFileController ceReportTemplateFileController(
            ICeReportTemplateFileService reportTemplateFileService
        ) {
            return new CeReportTemplateFileController(reportTemplateFileService);
        }

        @Bean
        CeDimensionRecordController ceDimensionRecordController(ICeDimensionRecordService dimensionRecordService) {
            return new CeDimensionRecordController(dimensionRecordService);
        }

        @Bean
        CeLicenseImportController ceLicenseImportController(ICeLicenseImportService licenseImportService,
                                                            CeLicensePublicKeyProvider publicKeyProvider) {
            return new CeLicenseImportController(licenseImportService, publicKeyProvider);
        }

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }
}
