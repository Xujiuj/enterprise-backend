package org.dromara.carbon.enterprise.dimension.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionSqlProvider;
import org.dromara.carbon.enterprise.dimension.service.impl.CeDimensionRecordServiceImpl;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.shared.service.ICeCompanyFactoryDeptSyncService;
import org.dromara.carbon.enterprise.vendor.client.CeVendorDimensionOpenClient;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeDimensionRecordServiceTest {

    private CeDimensionProjectionMapper dimensionProjectionMapper;
    private CeVendorDimensionOpenClient vendorDimensionOpenClient;
    private ICeCompanyFactoryDeptSyncService companyFactoryDeptSyncService;
    private CeDimensionRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        dimensionProjectionMapper = mock(CeDimensionProjectionMapper.class);
        CeLicenseStateMapper licenseStateMapper = mock(CeLicenseStateMapper.class);
        vendorDimensionOpenClient = mock(CeVendorDimensionOpenClient.class);
        companyFactoryDeptSyncService = mock(ICeCompanyFactoryDeptSyncService.class);
        service = new CeDimensionRecordServiceImpl(
            dimensionProjectionMapper,
            licenseStateMapper,
            vendorDimensionOpenClient,
            companyFactoryDeptSyncService
        );
    }

    @Test
    void concreteEnterpriseDimensionsAreReadFromLocalProjection() {
        CeDimensionRecordBo query = new CeDimensionRecordBo();
        query.setDimensionCode("company");
        query.setRecordName("Demo");
        Page<CeDimensionRecordVo> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(java.util.List.of(localCompany()));
        mapperPage.setTotal(1);
        when(dimensionProjectionMapper.selectPageByDimensionCode(any(), any())).thenReturn(mapperPage);

        TableDataInfo<CeDimensionRecordVo> page = service.queryPageList(query, new PageQuery(10, 1));

        assertEquals(1, page.getTotal());
        assertEquals("COMP-001", page.getRows().get(0).getRecordCode());
        verify(vendorDimensionOpenClient, never()).listDimensions(any(), any(), any(), any(), any());
    }

    @Test
    void reportTemplatesAreNotExposedAsDimensionRecords() {
        CeDimensionRecordBo query = new CeDimensionRecordBo();
        query.setDimensionCode("report-template-download");

        assertThrows(ServiceException.class, () -> service.queryPageList(query, new PageQuery(10, 1)));

        verify(vendorDimensionOpenClient, never()).listDimensions(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsLocalWritesForReadOnlyDimensions() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("greenhouse-gas");
        bo.setRecordCode("CO2");
        bo.setRecordName("Carbon dioxide");

        assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        verify(dimensionProjectionMapper, never()).insertByDimensionCode(any());
    }

    @Test
    void companyInsertGeneratesCompanySkWhenUserDoesNotProvideIt() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("company");
        bo.setRecordCode("COMP-001");
        bo.setParentCode("FAC-001");
        bo.setRecordName("Demo Company");
        bo.setFactoryName("Demo Factory");
        bo.setStatus("0");
        when(dimensionProjectionMapper.insertByDimensionCode(any())).thenReturn(1);

        service.insertByBo(bo);

        verify(dimensionProjectionMapper).insertByDimensionCode(argThat(record ->
            "SK_COMP-001_FAC-001".equals(record.getCompanySk()) && "Y".equals(record.getActiveFlag())
        ));
        verify(companyFactoryDeptSyncService).syncCompanyFactoriesToSysDept();
    }

    @Test
    void companyStatusOverridesActiveFlag() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("company");
        bo.setRecordCode("COMP-001");
        bo.setRecordName("Demo Company");
        bo.setParentCode("FAC-001");
        bo.setFactoryName("Demo Factory");
        bo.setStatus("1");
        bo.setActiveFlag("Y");
        when(dimensionProjectionMapper.insertByDimensionCode(any())).thenReturn(1);

        service.insertByBo(bo);

        verify(dimensionProjectionMapper).insertByDimensionCode(argThat(record -> "N".equals(record.getActiveFlag())));
    }

    @Test
    void companyInsertRejectsMissingFactoryCodeBeforeDatabaseConstraint() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("company");
        bo.setRecordCode("COMP-001");
        bo.setRecordName("Demo Company");
        bo.setFactoryName("Demo Factory");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("工厂编号不能为空", exception.getMessage());
        verify(dimensionProjectionMapper, never()).insertByDimensionCode(any());
    }

    @Test
    void intensityDenominatorRejectsMissingBusinessFieldsBeforeDatabaseConstraint() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("intensity-denominator");
        bo.setRecordCode("RULE-001");
        bo.setRecordName("制造工厂");
        bo.setDenominatorType("产量");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("分母度量名称不能为空", exception.getMessage());
        verify(dimensionProjectionMapper, never()).insertByDimensionCode(any());
    }

    @Test
    void intensityDenominatorNormalizesEnabledFlagBeforeWrite() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("intensity-denominator");
        bo.setRecordCode("RULE-001");
        bo.setRecordName("制造工厂");
        bo.setDenominatorType("产量");
        bo.setDenominatorMetricName("产品产量");
        bo.setIntensityUnitDisplay("tCO2e/吨");
        bo.setEnabledText("否");
        when(dimensionProjectionMapper.insertByDimensionCode(any())).thenReturn(1);

        service.insertByBo(bo);

        verify(dimensionProjectionMapper).insertByDimensionCode(argThat(record -> "0".equals(record.getEnabledText())));
    }

    @Test
    void companyDeleteDisablesSyncedFactoryDepartment() {
        CeDimensionRecordVo previous = localCompany();
        previous.setFactoryName("Demo Factory");
        when(dimensionProjectionMapper.selectByDimensionCodeAndId("company", 1L)).thenReturn(previous);
        when(dimensionProjectionMapper.deleteByDimensionCodeAndId("company", 1L)).thenReturn(1);

        boolean changed = service.deleteByIds("company", java.util.List.of(1L));

        assertTrue(changed);
        verify(companyFactoryDeptSyncService).disableCompanyFactoryDept(eq("COMP-001"), eq("Demo Factory"));
    }

    @Test
    void intensityTargetRejectsMissingTargetValueBeforeDatabaseConstraint() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("intensity-target");
        bo.setRecordCode("制造工厂");
        bo.setRecordName("2026");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("强度目标值不能为空", exception.getMessage());
        verify(dimensionProjectionMapper, never()).insertByDimensionCode(any());
    }

    @Test
    void companyWriteSqlBindsNullableDatesAsVarcharBeforeConvertingToSqlServerDate() {
        String insertSql = new CeDimensionProjectionSqlProvider().insertByDimensionCode();
        String updateSql = new CeDimensionProjectionSqlProvider().updateByDimensionCode();

        assertTrue(insertSql.contains("try_convert(date, nullif(#{record.effectiveDate,jdbcType=VARCHAR}, ''), 23)"));
        assertTrue(insertSql.contains("try_convert(date, nullif(#{record.expiryDate,jdbcType=VARCHAR}, ''), 23)"));
        assertTrue(updateSql.contains("try_convert(date, nullif(#{record.effectiveDate,jdbcType=VARCHAR}, ''), 23)"));
        assertTrue(updateSql.contains("try_convert(date, nullif(#{record.expiryDate,jdbcType=VARCHAR}, ''), 23)"));
    }

    @Test
    void intensityEnabledSqlUsesStableNumericFlagInsteadOfChineseText() {
        String insertSql = new CeDimensionProjectionSqlProvider().insertByDimensionCode();
        String updateSql = new CeDimensionProjectionSqlProvider().updateByDimensionCode();

        assertTrue(insertSql.contains("case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end"));
        assertTrue(updateSql.contains("case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end"));
    }

    private CeDimensionRecordVo localCompany() {
        CeDimensionRecordVo record = new CeDimensionRecordVo();
        record.setId(1L);
        record.setDimensionCode("company");
        record.setRecordCode("COMP-001");
        record.setRecordName("Demo Company");
        record.setStatus("0");
        return record;
    }
}
