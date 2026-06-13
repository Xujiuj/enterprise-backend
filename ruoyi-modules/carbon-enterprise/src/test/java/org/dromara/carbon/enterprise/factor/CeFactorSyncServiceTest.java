package org.dromara.carbon.enterprise.factor;

import org.dromara.carbon.enterprise.client.CeVendorFactorOpenClient;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.domain.CeFactorCacheVersion;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.sync.CeFactorSyncResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorFactorRecord;
import org.dromara.carbon.enterprise.domain.sync.CeVendorFactorSyncResponse;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheVersionMapper;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.impl.CeFactorSyncServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeFactorSyncServiceTest {

    private CeLicenseStateMapper licenseStateMapper;
    private CeFactorCacheVersionMapper factorCacheVersionMapper;
    private CeFactorCacheRecordMapper factorCacheRecordMapper;
    private CeVendorFactorOpenClient vendorFactorOpenClient;
    private CeFactorSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        licenseStateMapper = mock(CeLicenseStateMapper.class);
        factorCacheVersionMapper = mock(CeFactorCacheVersionMapper.class);
        factorCacheRecordMapper = mock(CeFactorCacheRecordMapper.class);
        vendorFactorOpenClient = mock(CeVendorFactorOpenClient.class);
        service = new CeFactorSyncServiceImpl(
            licenseStateMapper,
            factorCacheVersionMapper,
            factorCacheRecordMapper,
            vendorFactorOpenClient
        );
    }

    @Test
    void syncsVendorFactorsIntoLocalCache() {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of(validLicense()));
        CeFactorCacheVersion currentCache = new CeFactorCacheVersion();
        currentCache.setVersionCode("OLD");
        when(factorCacheVersionMapper.selectOne(any(), eq(false))).thenReturn(currentCache).thenReturn(null);
        doAnswer(invocation -> {
            CeFactorCacheVersion version = invocation.getArgument(0);
            version.setId(7001L);
            return 1;
        }).when(factorCacheVersionMapper).insert(any(CeFactorCacheVersion.class));
        when(vendorFactorOpenClient.syncFactors("LIC-001", "INSTALL-001", "OLD")).thenReturn(vendorResponse());
        when(factorCacheRecordMapper.selectOne(any(), eq(false))).thenReturn(null);

        CeFactorSyncResponse response = service.syncCurrentLicenseFactors();

        assertEquals("LIC-001", response.getLicenseId());
        assertEquals("88", response.getVendorVersionId());
        assertEquals("FV-2026", response.getVersionCode());
        assertEquals(1, response.getRecordCount());

        ArgumentCaptor<CeFactorCacheVersion> versionCaptor = ArgumentCaptor.forClass(CeFactorCacheVersion.class);
        verify(factorCacheVersionMapper).insert(versionCaptor.capture());
        assertEquals("88", versionCaptor.getValue().getVendorVersionId());
        assertEquals("LIC-001", versionCaptor.getValue().getLicenseId());

        ArgumentCaptor<CeFactorCacheRecord> recordCaptor = ArgumentCaptor.forClass(CeFactorCacheRecord.class);
        verify(factorCacheRecordMapper).insert(recordCaptor.capture());
        assertEquals(7001L, recordCaptor.getValue().getCacheVersionId());
        assertEquals("EF-ELEC-ZJ", recordCaptor.getValue().getFactorCode());
    }

    @Test
    void preservesExistingCacheWhenVendorClientFails() {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of(validLicense()));
        CeFactorCacheVersion currentCache = new CeFactorCacheVersion();
        currentCache.setVersionCode("OLD");
        when(factorCacheVersionMapper.selectOne(any(), eq(false))).thenReturn(currentCache);
        when(vendorFactorOpenClient.syncFactors("LIC-001", "INSTALL-001", "OLD"))
            .thenThrow(new ServiceException("vendor unavailable"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.syncCurrentLicenseFactors());

        assertEquals("vendor unavailable", exception.getMessage());
        verify(factorCacheVersionMapper, never()).insert(any(CeFactorCacheVersion.class));
        verify(factorCacheVersionMapper, never()).updateById(any(CeFactorCacheVersion.class));
        verify(factorCacheRecordMapper, never()).insert(any(CeFactorCacheRecord.class));
        verify(factorCacheRecordMapper, never()).updateById(any(CeFactorCacheRecord.class));
    }

    @Test
    void rejectsSyncWhenNoValidLicenseExists() {
        when(licenseStateMapper.selectList(any())).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.syncCurrentLicenseFactors());

        assertEquals("valid license state does not exist", exception.getMessage());
        verify(vendorFactorOpenClient, never()).syncFactors(any(), any(), any());
    }

    @Test
    void usesEnterpriseImportedValidLicenseStatus() throws Exception {
        Field field = CeFactorSyncServiceImpl.class.getDeclaredField("LICENSE_STATUS_VALID");
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

    private CeVendorFactorSyncResponse vendorResponse() {
        CeVendorFactorSyncResponse response = new CeVendorFactorSyncResponse();
        response.setLicenseId("LIC-001");
        response.setVendorVersionId("88");
        response.setVersionCode("FV-2026");
        response.setFrozenFlag(Boolean.FALSE);
        response.setChanged(true);
        response.setRecords(List.of(vendorRecord()));
        return response;
    }

    private CeVendorFactorRecord vendorRecord() {
        CeVendorFactorRecord record = new CeVendorFactorRecord();
        record.setFactorCode("EF-ELEC-ZJ");
        record.setFactorName("Zhejiang grid electricity");
        record.setFactorCategory("electricity");
        record.setFactorValue(new BigDecimal("0.5703000000"));
        record.setFactorUnit("kgCO2e/kWh");
        record.setSourceRef("official-source");
        return record;
    }
}
