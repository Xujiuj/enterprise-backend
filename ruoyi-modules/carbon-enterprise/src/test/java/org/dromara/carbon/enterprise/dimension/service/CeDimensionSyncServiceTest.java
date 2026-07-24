package org.dromara.carbon.enterprise.dimension.service;

import org.dromara.carbon.enterprise.dimension.domain.CeAdminDivision;
import org.dromara.carbon.enterprise.dimension.domain.CeBaseYear;
import org.dromara.carbon.enterprise.dimension.mapper.CeAdminDivisionMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeBaseYearMapper;
import org.dromara.carbon.enterprise.dimension.service.impl.CeDimensionSyncServiceImpl;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactorVersionMap;
import org.dromara.carbon.enterprise.factor.mapper.CeElectricityFactorMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeElectricityFactorScopeMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeElectricityFactorVersionMapMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeGreenhouseGasMapper;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.vendor.client.CeVendorDimensionOpenClient;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeDimensionSyncServiceTest {

    private final CeEmissionSourceCategoryMapper emissionSourceCategoryMapper = mock(CeEmissionSourceCategoryMapper.class);
    private final CeBaseYearMapper baseYearMapper = mock(CeBaseYearMapper.class);
    private final CeElectricityFactorVersionMapMapper electricityFactorVersionMapMapper = mock(CeElectricityFactorVersionMapMapper.class);
    private final CeDimensionSyncServiceImpl service = new CeDimensionSyncServiceImpl(
        mock(CeLicenseStateMapper.class),
        mock(CeVendorDimensionOpenClient.class),
        mock(CeAdminDivisionMapper.class),
        emissionSourceCategoryMapper,
        baseYearMapper,
        mock(CeElectricityFactorMapper.class),
        electricityFactorVersionMapMapper,
        mock(CeElectricityFactorScopeMapper.class),
        mock(CeGreenhouseGasMapper.class),
        mock(TransactionTemplate.class)
    );

    @Test
    void emissionSourceCategorySyncKeepsSourceASkAndBusinessKeySeparate() throws Exception {
        CeVendorDimensionRecord record = new CeVendorDimensionRecord();
        record.setRecordCode("1");
        record.setRecordName("1.1 固定源燃烧");
        record.setCategorySk("1");
        record.setBusinessKey("101");
        record.setGhgScope("范围1");
        record.setGhgScopeCategory("1.1 固定源燃烧");
        record.setVersionNo("2");
        record.setCurrentFlag("1");

        when(emissionSourceCategoryMapper.selectOne(any(), eq(false))).thenReturn(null);

        invoke("upsertEmissionSourceCategory", record);

        ArgumentCaptor<CeEmissionSourceCategory> captor = ArgumentCaptor.forClass(CeEmissionSourceCategory.class);
        verify(emissionSourceCategoryMapper).insert(captor.capture());
        CeEmissionSourceCategory entity = captor.getValue();
        assertEquals("1", entity.getCategorySk());
        assertEquals("101", entity.getBusinessKey());
        assertEquals("范围1", entity.getGhgScope());
        assertEquals("2", entity.getVersionNo());
        assertEquals("Y", entity.getIsCurrent());
    }

    @Test
    void baseYearSyncUsesBaseYearKeyInsteadOfFactoryColumns() throws Exception {
        CeVendorDimensionRecord record = new CeVendorDimensionRecord();
        record.setRecordCode("1");
        record.setRecordName("2023");
        record.setBaseYearKey("1");
        record.setBaseYear(2023);
        record.setIsCurrent(1);
        record.setDescription("国家 / ISSB / 双碳基准");

        when(baseYearMapper.selectOne(any(), eq(false))).thenReturn(null);

        invoke("upsertBaseYear", record);

        ArgumentCaptor<CeBaseYear> captor = ArgumentCaptor.forClass(CeBaseYear.class);
        verify(baseYearMapper).insert(captor.capture());
        CeBaseYear entity = captor.getValue();
        assertEquals("1", entity.getBaseYearKey());
        assertEquals(2023, entity.getBaseYear());
        assertEquals(1, entity.getIsCurrent());
        assertNull(entity.getFactoryCode());
        assertNull(entity.getFactoryName());
    }

    @Test
    void electricityVersionSyncKeepsDifferentYearsForTheSameFactorVersion() throws Exception {
        CeVendorDimensionRecord record = new CeVendorDimensionRecord();
        record.setFactorVersion("2023");
        record.setEffectiveYear(2025);
        record.setStatus("0");
        record.setRemark("source(A)");

        when(electricityFactorVersionMapMapper.selectOne(any(), eq(false))).thenReturn(null);

        invoke("upsertElectricityFactorVersionMap", record);

        ArgumentCaptor<CeElectricityFactorVersionMap> captor = ArgumentCaptor.forClass(CeElectricityFactorVersionMap.class);
        verify(electricityFactorVersionMapMapper).insert(captor.capture());
        CeElectricityFactorVersionMap inserted = captor.getValue();
        assertEquals("2023", inserted.getFactorVersion());
        assertEquals(2025, inserted.getEffectiveYear());
    }

    @Test
    void fullSyncDeletesRowsMissingFromVendorSnapshot() throws Exception {
        CeElectricityFactorVersionMap retained = new CeElectricityFactorVersionMap();
        retained.setId(7L);
        retained.setFactorVersion("2023");
        retained.setEffectiveYear(2025);
        CeElectricityFactorVersionMap deleted = new CeElectricityFactorVersionMap();
        deleted.setId(8L);
        deleted.setFactorVersion("2023");
        deleted.setEffectiveYear(2026);
        when(electricityFactorVersionMapMapper.selectList(any())).thenReturn(List.of(retained, deleted));

        Method method = CeDimensionSyncServiceImpl.class.getDeclaredMethod("deleteMissingDimensionRecords", String.class, Set.class);
        method.setAccessible(true);
        method.invoke(service, "ef-electricity-version", Set.of("2025\u001f2023"));

        verify(electricityFactorVersionMapMapper).deleteByIds(List.of(8L));
    }

    @Test
    void categorySyncPreservesOlderVersionsAndReconcilesReceivedVersion() throws Exception {
        CeEmissionSourceCategory olderVersion = category(1L, "101", "1");
        CeEmissionSourceCategory retained = category(2L, "101", "2");
        CeEmissionSourceCategory missing = category(3L, "102", "2");
        when(emissionSourceCategoryMapper.selectList(any())).thenReturn(List.of(olderVersion, retained, missing));

        Method method = CeDimensionSyncServiceImpl.class.getDeclaredMethod("deleteMissingDimensionRecords", String.class, Set.class);
        method.setAccessible(true);
        method.invoke(service, "emission-source-category", Set.of("101\u001f2"));

        verify(emissionSourceCategoryMapper).deleteByIds(List.of(3L));
    }

    @Test
    void categorySnapshotKeyIncludesVersionNumber() throws Exception {
        CeVendorDimensionRecord record = new CeVendorDimensionRecord();
        record.setCategorySk("101");
        record.setVersionNo("3");
        Method method = CeDimensionSyncServiceImpl.class.getDeclaredMethod(
            "dimensionRecordKey", String.class, CeVendorDimensionRecord.class);
        method.setAccessible(true);

        assertEquals("101\u001f3", method.invoke(service, "emission-source-category", record));
    }

    private CeEmissionSourceCategory category(Long id, String categorySk, String versionNo) {
        CeEmissionSourceCategory category = new CeEmissionSourceCategory();
        category.setId(id);
        category.setCategorySk(categorySk);
        category.setVersionNo(versionNo);
        return category;
    }

    private void invoke(String methodName, CeVendorDimensionRecord record) throws Exception {
        Method method = CeDimensionSyncServiceImpl.class.getDeclaredMethod(methodName, CeVendorDimensionRecord.class);
        method.setAccessible(true);
        try {
            method.invoke(service, record);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }
}
