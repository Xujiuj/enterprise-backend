package org.dromara.carbon.enterprise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.config.CeLicenseGateWebMvcConfigurer;
import org.dromara.carbon.enterprise.domain.license.CeLicenseGateResult;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResult;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.domain.vo.CeExtensionFieldVo;
import org.dromara.carbon.enterprise.interceptor.CeLicenseGateInterceptor;
import org.dromara.carbon.enterprise.service.ICeActivityDataService;
import org.dromara.carbon.enterprise.service.ICeDimensionRecordService;
import org.dromara.carbon.enterprise.service.ICeEmissionSourceService;
import org.dromara.carbon.enterprise.service.ICeFactorConfirmationService;
import org.dromara.carbon.enterprise.service.ICeFactorSyncService;
import org.dromara.carbon.enterprise.service.CeLicenseInstallIdProvider;
import org.dromara.carbon.enterprise.service.CeLicensePublicKeyProvider;
import org.dromara.carbon.enterprise.service.ICeGreenPowerCertificateService;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldService;
import org.dromara.carbon.enterprise.service.ICeExtensionFieldValueService;
import org.dromara.carbon.enterprise.service.ICeIntensityMetricService;
import org.dromara.carbon.enterprise.service.ICeLicenseGateService;
import org.dromara.carbon.enterprise.service.ICeLicenseImportService;
import org.dromara.carbon.enterprise.service.ICeReportTemplateFileService;
import org.dromara.carbon.enterprise.service.ICeReportTemplateSyncService;
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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = CeLicenseGateWebMvcConfigurerTest.TestConfig.class)
class CeLicenseGateWebMvcConfigurerTest {

    private static final String EXPECTED_INSTALL_ID = "INSTALL-ENTERPRISE-001";

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

    @Autowired
    private ICeFactorSyncService factorSyncService;

    @Autowired
    private ICeReportTemplateSyncService reportTemplateSyncService;

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
            dimensionRecordService,
            factorSyncService,
            reportTemplateSyncService
        );
        when(installIdProvider.getExpectedInstallId()).thenReturn(EXPECTED_INSTALL_ID);
    }

    @Test
    void keepsEnterpriseLocalCrudOpenWhenLicenseIsDenied() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("DENY", "EXPIRED", null));
        when(extensionFieldService.queryPageList(any(), any()))
            .thenReturn(new TableDataInfo<>(Collections.<CeExtensionFieldVo>emptyList(), 0));

        mockMvc.perform(get("/enterprise/extension-field/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.rows").isArray())
            .andExpect(jsonPath("$.total", is(0)));

        verifyNoInteractions(licenseGateService);
        verify(extensionFieldService).queryPageList(any(), any());
    }

    @Test
    void keepsEnterpriseLocalWriteCrudOpenWhenLicenseIsDenied() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("DENY", "EXPIRED", null));
        when(extensionFieldService.insertByBo(any())).thenReturn(true);
        when(extensionFieldService.updateByBo(any())).thenReturn(true);
        when(extensionFieldService.deleteByIds(any())).thenReturn(true);

        mockMvc.perform(post("/enterprise/extension-field")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"templateVersionId":1,"moduleCode":"activity","sheetId":1,"fieldCode":"fuel","fieldName":"Fuel"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)));

        mockMvc.perform(put("/enterprise/extension-field")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"id":1,"templateVersionId":1,"moduleCode":"activity","sheetId":1,"fieldCode":"fuel","fieldName":"Fuel"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)));

        mockMvc.perform(delete("/enterprise/extension-field/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)));

        verifyNoInteractions(licenseGateService);
        verify(extensionFieldService).insertByBo(any());
        verify(extensionFieldService).updateByBo(any());
        verify(extensionFieldService).deleteByIds(any());
    }

    @Test
    void keepsEnterpriseLocalValidationRoutesOpenWhenLicenseIsDenied() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class)))
            .thenReturn(new CeLicenseGateResult("DENY", "EXPIRED", null));
        when(activityDataService.queryValidationDashboard(any()))
            .thenReturn(new CeActivityDataValidationDashboardVo());

        for (String route : List.of(
            "/enterprise/data-validation/dashboard",
            "/enterprise/data-validation/summary",
            "/enterprise/data-validation/submissions",
            "/enterprise/data-validation/issues"
        )) {
            mockMvc.perform(get(route))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.expectedItems", is(0)));
        }

        verifyNoInteractions(licenseGateService);
        verify(activityDataService, times(4)).queryValidationDashboard(any());
    }

    @Test
    void keepsEnterpriseLocalReportTemplateListOpenWhenLicenseIsDenied() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), any()))
            .thenReturn(new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", null));
        when(reportTemplateFileService.queryPageList(any(), any()))
            .thenReturn(new TableDataInfo<>(Collections.emptyList(), 0));

        mockMvc.perform(get("/enterprise/report-template-file/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.rows").isArray())
            .andExpect(jsonPath("$.total", is(0)));

        verifyNoInteractions(licenseGateService);
        verify(reportTemplateFileService).queryPageList(any(), any());
    }

    @Test
    void deniesVendorReportTemplateDownloadWhenFeatureIsNotEnabled() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-template-download")))
            .thenReturn(new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", null));

        mockMvc.perform(get("/enterprise/report-template-file/download/7"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().encoding("UTF-8"))
            .andExpect(jsonPath("$.code", is(403)))
            .andExpect(jsonPath("$.msg", is("enterprise license gate denied access")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.data.gate.reason", is("FEATURE_NOT_ENABLED")))
            .andExpect(jsonPath("$.data.gate.message", is("license does not include the required feature")));

        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-template-download"));
        verifyNoInteractions(reportTemplateFileService);
    }

    @Test
    void deniesVendorFactorSyncWhenFeatureIsNotEnabled() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("factor-sync")))
            .thenReturn(new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", null));

        mockMvc.perform(post("/enterprise/factor-sync/run"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.data.gate.reason", is("FEATURE_NOT_ENABLED")));

        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("factor-sync"));
        verifyNoInteractions(factorSyncService);
    }

    @Test
    void deniesVendorReportTemplateSyncWhenFeatureIsNotEnabled() throws Exception {
        when(licenseGateService.evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-template-download")))
            .thenReturn(new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", null));

        mockMvc.perform(post("/enterprise/report-template-sync/run"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.data.gate.reason", is("FEATURE_NOT_ENABLED")));

        verify(licenseGateService).evaluateCurrent(eq(EXPECTED_INSTALL_ID), any(Date.class), eq("report-template-download"));
        verifyNoInteractions(reportTemplateSyncService);
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
        ICeFactorSyncService factorSyncService() {
            return mock(ICeFactorSyncService.class);
        }

        @Bean
        ICeReportTemplateSyncService reportTemplateSyncService() {
            return mock(ICeReportTemplateSyncService.class);
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
        CeFactorSyncController ceFactorSyncController(ICeFactorSyncService factorSyncService) {
            return new CeFactorSyncController(factorSyncService);
        }

        @Bean
        CeReportTemplateSyncController ceReportTemplateSyncController(ICeReportTemplateSyncService reportTemplateSyncService) {
            return new CeReportTemplateSyncController(reportTemplateSyncService);
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
