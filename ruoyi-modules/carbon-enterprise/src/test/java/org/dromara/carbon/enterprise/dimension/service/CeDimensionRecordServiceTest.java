package org.dromara.carbon.enterprise.dimension.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    }

    @Test
    void companyStatusOverridesActiveFlag() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("company");
        bo.setRecordCode("COMP-001");
        bo.setRecordName("Demo Company");
        bo.setStatus("1");
        bo.setActiveFlag("Y");
        when(dimensionProjectionMapper.insertByDimensionCode(any())).thenReturn(1);

        service.insertByBo(bo);

        verify(dimensionProjectionMapper).insertByDimensionCode(argThat(record -> "N".equals(record.getActiveFlag())));
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
