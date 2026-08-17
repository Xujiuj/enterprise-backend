package org.dromara.carbon.enterprise.dimension.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionSqlProvider;
import org.dromara.carbon.enterprise.dimension.service.impl.CeDimensionRecordServiceImpl;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.vendor.client.CeVendorDimensionOpenClient;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private CeDimensionRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        dimensionProjectionMapper = mock(CeDimensionProjectionMapper.class);
        CeLicenseStateMapper licenseStateMapper = mock(CeLicenseStateMapper.class);
        vendorDimensionOpenClient = mock(CeVendorDimensionOpenClient.class);
        service = new CeDimensionRecordServiceImpl(
            dimensionProjectionMapper,
            licenseStateMapper,
            vendorDimensionOpenClient
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
    void companyInsertIsRejectedBecauseDepartmentManagementOwnsOrganizationData() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("company");
        bo.setRecordCode("COMP-001");
        bo.setRecordName("Demo Company");
        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("公司表由部门管理维护，仅支持查看", exception.getMessage());
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
    void efFactorInsertGeneratesRecordCodeWhenTemplateOmitsSequence() {
        CeDimensionRecordVo existing = new CeDimensionRecordVo();
        existing.setRecordCode("67");
        CeDimensionRecordVo legacyPrefixed = new CeDimensionRecordVo();
        legacyPrefixed.setRecordCode("EF201-9999");
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("ef-factor");
        bo.setRecordName("天然气");
        bo.setSourceUnit("m3");
        when(dimensionProjectionMapper.selectByDimensionCode("ef-factor")).thenReturn(java.util.List.of(existing, legacyPrefixed));
        when(dimensionProjectionMapper.insertByDimensionCode(any())).thenReturn(1);

        service.insertByBo(bo);

        verify(dimensionProjectionMapper).insertByDimensionCode(argThat(record ->
            "68".equals(record.getRecordCode()) && "天然气".equals(record.getRecordName())
        ));
    }

    @Test
    void companyDeleteIsRejectedBecauseDepartmentManagementOwnsOrganizationData() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.deleteByIds("company", java.util.List.of(1L)));

        assertEquals("公司表由部门管理维护，仅支持查看", exception.getMessage());
        verify(dimensionProjectionMapper, never()).deleteByDimensionCodeAndId(any(), any());
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
    void companyProjectionPreservesCompanyTableFieldsAndReadsOrganizationFieldsFromDepartmentTree() {
        String selectSql = new CeDimensionProjectionSqlProvider()
            .selectByDimensionCode(java.util.Map.of("dimensionCode", "company"));

        assertTrue(selectSql.contains("from ce_company_factory company_factory"));
        assertTrue(selectSql.contains("left join sys_dept factory"));
        assertTrue(selectSql.contains("left join sys_dept company"));
        assertTrue(selectSql.contains("coalesce(company.dept_category, company_factory.company_code) as record_code"));
        assertTrue(selectSql.contains("company_factory.province_code as province_code"));
        assertTrue(selectSql.contains("company_factory.industry_section_code as industry_section_code"));
        assertTrue(selectSql.contains("company_factory.remark"));
    }

    @Test
    void intensityEnabledSqlUsesStableNumericFlagInsteadOfChineseText() {
        String insertSql = new CeDimensionProjectionSqlProvider().insertByDimensionCode();
        String updateSql = new CeDimensionProjectionSqlProvider().updateByDimensionCode();

        assertTrue(insertSql.contains("case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end"));
        assertTrue(updateSql.contains("case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end"));
    }

    @Test
    void electricityVersionSqlUsesYearAsCodeAndFactorVersionAsName() {
        CeDimensionProjectionSqlProvider provider = new CeDimensionProjectionSqlProvider();
        String selectSql = provider.selectByDimensionCode(java.util.Map.of("dimensionCode", "ef-electricity-version"));
        String insertSql = provider.insertByDimensionCode();
        String updateSql = provider.updateByDimensionCode();

        assertTrue(selectSql.contains("cast(effective_year as char) as record_code"));
        assertTrue(selectSql.contains("factor_version as record_name"));
        assertTrue(insertSql.contains("#{record.recordName}, #{record.recordCode}, #{record.remark}"));
        assertTrue(updateSql.contains("factor_version = #{record.recordName}"));
        assertTrue(updateSql.contains("effective_year = #{record.recordCode}"));
    }

    @Test
    void emissionSourceCategoryUsesLatestVersionWhileHistoryKeepsAllVersions() {
        CeDimensionProjectionSqlProvider provider = new CeDimensionProjectionSqlProvider();

        String currentSql = provider.selectByDimensionCode(java.util.Map.of("dimensionCode", "emission-source-category"));
        String historySql = provider.selectByDimensionCode(java.util.Map.of("dimensionCode", "emission-source-category-history"));

        assertTrue(currentSql.contains("select top 1"));
        assertTrue(currentSql.contains("version_no"));
        assertTrue(historySql.contains("from ce_emission_source_category"));
        assertFalse(historySql.contains("select top 1"));
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
