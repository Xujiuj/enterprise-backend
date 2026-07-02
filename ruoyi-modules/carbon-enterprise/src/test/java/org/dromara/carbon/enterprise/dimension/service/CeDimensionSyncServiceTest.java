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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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
        mock(CeGreenhouseGasMapper.class)
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

        when(emissionSourceCategoryMapper.selectOne(any(), eq(false))).thenReturn(null);

        invoke("upsertEmissionSourceCategory", record);

        ArgumentCaptor<CeEmissionSourceCategory> captor = ArgumentCaptor.forClass(CeEmissionSourceCategory.class);
        verify(emissionSourceCategoryMapper).insert(captor.capture());
        CeEmissionSourceCategory entity = captor.getValue();
        assertEquals("1", entity.getCategorySk());
        assertEquals("101", entity.getBusinessKey());
        assertEquals("范围1", entity.getGhgScope());
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
    void electricityVersionSyncRemovesDuplicateFactorVersionRows() throws Exception {
        CeVendorDimensionRecord record = new CeVendorDimensionRecord();
        record.setRecordCode("2023");
        record.setEffectiveYear(2025);
        record.setStatus("0");
        record.setRemark("source(A)");

        CeElectricityFactorVersionMap stale = new CeElectricityFactorVersionMap();
        stale.setId(8L);
        stale.setFactorVersion("2023");
        stale.setEffectiveYear(2026);
        CeElectricityFactorVersionMap current = new CeElectricityFactorVersionMap();
        current.setId(7L);
        current.setFactorVersion("2023");
        current.setEffectiveYear(2025);
        when(electricityFactorVersionMapMapper.selectList(any())).thenReturn(List.of(stale, current));

        invoke("upsertElectricityFactorVersionMap", record);

        verify(electricityFactorVersionMapMapper).deleteById(8L);
        ArgumentCaptor<CeElectricityFactorVersionMap> captor = ArgumentCaptor.forClass(CeElectricityFactorVersionMap.class);
        verify(electricityFactorVersionMapMapper).updateById(captor.capture());
        CeElectricityFactorVersionMap updated = captor.getValue();
        assertEquals(7L, updated.getId());
        assertEquals(2025, updated.getEffectiveYear());
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
