package org.dromara.carbon.enterprise.report;

import org.dromara.carbon.enterprise.client.CeVendorReportTemplateOpenClient;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.domain.sync.CeReportTemplateSyncResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateDownloadResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateListResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateRecord;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.service.impl.CeReportTemplateSyncServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeReportTemplateSyncServiceTest {

    private CeLicenseStateMapper licenseStateMapper;
    private CeReportTemplateFileMapper reportTemplateFileMapper;
    private CeVendorReportTemplateOpenClient vendorReportTemplateOpenClient;
    private CeReportTemplateSyncServiceImpl service;

    @TempDir
    private Path templateRoot;

    @BeforeEach
    void setUp() throws Exception {
        licenseStateMapper = mock(CeLicenseStateMapper.class);
        reportTemplateFileMapper = mock(CeReportTemplateFileMapper.class);
        vendorReportTemplateOpenClient = mock(CeVendorReportTemplateOpenClient.class);
        service = new CeReportTemplateSyncServiceImpl(
            licenseStateMapper,
            reportTemplateFileMapper,
            vendorReportTemplateOpenClient
        );
        setReportTemplateRoot();
    }

    @Test
    void syncsAuthorizedVendorTemplatesIntoLocalCatalog() throws Exception {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of(validLicense()));
        when(vendorReportTemplateOpenClient.listTemplates("LIC-001", "INSTALL-001"))
            .thenReturn(listResponse());
        when(vendorReportTemplateOpenClient.downloadTemplate(301L, "LIC-001", "INSTALL-001"))
            .thenReturn(downloadResponse());
        when(vendorReportTemplateOpenClient.downloadTemplateFile("TOKEN-001"))
            .thenReturn("template-bytes".getBytes());
        when(reportTemplateFileMapper.selectOne(any(), eq(false))).thenReturn(null);

        CeReportTemplateSyncResponse response = service.syncCurrentLicenseReportTemplates();

        assertEquals("LIC-001", response.getLicenseId());
        assertEquals(1, response.getTemplateCount());
        ArgumentCaptor<CeReportTemplateFile> captor = ArgumentCaptor.forClass(CeReportTemplateFile.class);
        verify(reportTemplateFileMapper).insert(captor.capture());
        assertEquals("carbon-standard", captor.getValue().getTemplateCode());
        assertEquals("vendor", captor.getValue().getTemplateType());
        assertEquals("vendor/carbon-standard-301-carbon-standard.pbix", captor.getValue().getFilePath());
        assertEquals("template-bytes", Files.readString(templateRoot.resolve(captor.getValue().getFilePath())));
    }

    @Test
    void updatesExistingLocalTemplateCatalogRecord() {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of(validLicense()));
        when(vendorReportTemplateOpenClient.listTemplates("LIC-001", "INSTALL-001"))
            .thenReturn(listResponse());
        when(vendorReportTemplateOpenClient.downloadTemplate(301L, "LIC-001", "INSTALL-001"))
            .thenReturn(downloadResponse());
        when(vendorReportTemplateOpenClient.downloadTemplateFile("TOKEN-001"))
            .thenReturn("updated-template-bytes".getBytes());
        CeReportTemplateFile existing = new CeReportTemplateFile();
        existing.setId(88L);
        existing.setTemplateCode("carbon-standard");
        when(reportTemplateFileMapper.selectOne(any(), eq(false))).thenReturn(existing);

        service.syncCurrentLicenseReportTemplates();

        ArgumentCaptor<CeReportTemplateFile> captor = ArgumentCaptor.forClass(CeReportTemplateFile.class);
        verify(reportTemplateFileMapper).updateById(captor.capture());
        assertEquals(88L, captor.getValue().getId());
        assertEquals("vendor/carbon-standard-301-carbon-standard.pbix", captor.getValue().getFilePath());
    }

    @Test
    void doesNotWriteWhenVendorDownloadResponseIsIncomplete() {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of(validLicense()));
        when(vendorReportTemplateOpenClient.listTemplates("LIC-001", "INSTALL-001"))
            .thenReturn(listResponse());
        CeVendorReportTemplateDownloadResponse incomplete = downloadResponse();
        incomplete.setDownloadToken(" ");
        when(vendorReportTemplateOpenClient.downloadTemplate(301L, "LIC-001", "INSTALL-001"))
            .thenReturn(incomplete);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.syncCurrentLicenseReportTemplates());

        assertEquals("vendor report template download token is missing", exception.getMessage());
        verify(reportTemplateFileMapper, never()).insert(any(CeReportTemplateFile.class));
        verify(reportTemplateFileMapper, never()).updateById(any(CeReportTemplateFile.class));
    }

    @Test
    void rejectsSyncWhenNoValidLicenseExists() {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.syncCurrentLicenseReportTemplates());

        assertEquals("valid license state does not exist", exception.getMessage());
        verify(vendorReportTemplateOpenClient, never()).listTemplates(any(), any());
    }

    @Test
    void usesEnterpriseImportedValidLicenseStatus() throws Exception {
        Field field = CeReportTemplateSyncServiceImpl.class.getDeclaredField("LICENSE_STATUS_VALID");
        field.setAccessible(true);
        assertEquals("VALID", field.get(null));
    }

    private CeLicenseState validLicense() {
        CeLicenseState license = new CeLicenseState();
        license.setLicenseId("LIC-001");
        license.setInstallId("INSTALL-001");
        license.setLicenseStatus("VALID");
        license.setValidFrom(Date.from(Instant.now().minusSeconds(3600)));
        license.setValidTo(Date.from(Instant.now().plusSeconds(3600)));
        return license;
    }

    private void setReportTemplateRoot() throws Exception {
        Field field = CeReportTemplateSyncServiceImpl.class.getDeclaredField("reportTemplateRoot");
        field.setAccessible(true);
        field.set(service, templateRoot.toString());
    }

    private CeVendorReportTemplateListResponse listResponse() {
        CeVendorReportTemplateListResponse response = new CeVendorReportTemplateListResponse();
        response.setLicenseId("LIC-001");
        response.setTemplates(List.of(templateRecord()));
        return response;
    }

    private CeVendorReportTemplateRecord templateRecord() {
        CeVendorReportTemplateRecord record = new CeVendorReportTemplateRecord();
        record.setTemplateId(301L);
        record.setTemplateCode("carbon-standard");
        record.setTemplateName("Carbon standard");
        record.setTemplateVersion("2026.1");
        record.setFileName("carbon-standard.pbix");
        return record;
    }

    private CeVendorReportTemplateDownloadResponse downloadResponse() {
        CeVendorReportTemplateDownloadResponse response = new CeVendorReportTemplateDownloadResponse();
        response.setLicenseId("LIC-001");
        response.setTemplateId(301L);
        response.setTemplateCode("carbon-standard");
        response.setTemplateName("Carbon standard");
        response.setTemplateVersion("2026.1");
        response.setFileName("carbon-standard.pbix");
        response.setDownloadToken("TOKEN-001");
        return response;
    }
}
