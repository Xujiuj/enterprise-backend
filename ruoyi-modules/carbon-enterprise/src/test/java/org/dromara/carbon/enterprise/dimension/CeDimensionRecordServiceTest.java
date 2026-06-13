package org.dromara.carbon.enterprise.dimension;

import org.dromara.carbon.enterprise.client.CeVendorDimensionOpenClient;
import org.dromara.carbon.enterprise.domain.CeDimensionRecord;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionListResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionRecord;
import org.dromara.carbon.enterprise.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.mapper.CeDimensionRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.impl.CeDimensionRecordServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
class CeDimensionRecordServiceTest {

    private CeDimensionRecordMapper dimensionRecordMapper;
    private CeLicenseStateMapper licenseStateMapper;
    private CeVendorDimensionOpenClient vendorDimensionOpenClient;
    private CeDimensionRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        dimensionRecordMapper = mock(CeDimensionRecordMapper.class);
        licenseStateMapper = mock(CeLicenseStateMapper.class);
        vendorDimensionOpenClient = mock(CeVendorDimensionOpenClient.class);
        service = new CeDimensionRecordServiceImpl(
            dimensionRecordMapper,
            licenseStateMapper,
            vendorDimensionOpenClient
        );
    }

    @Test
    void vendorOwnedDimensionsAreReadFromVendorOpenApi() {
        CeDimensionRecordBo query = new CeDimensionRecordBo();
        query.setDimensionCode("admin-division");
        query.setRecordName("浙江");
        when(licenseStateMapper.selectList(any())).thenReturn(List.of(validLicense()));
        when(vendorDimensionOpenClient.listDimensions(eq("LIC-001"), eq("INSTALL-001"), eq(query), eq(1), eq(10)))
            .thenReturn(vendorResponse());

        TableDataInfo<CeDimensionRecordVo> page = service.queryPageList(query, new PageQuery(10, 1));

        assertEquals(1, page.getTotal());
        assertEquals("330000", page.getRows().get(0).getRecordCode());
        assertEquals("vendor", page.getRows().get(0).getSourceType());
        verify(dimensionRecordMapper, never()).selectVoPage(any(), any());
    }

    @Test
    void rejectsLocalWritesForVendorOwnedDimensions() {
        CeDimensionRecordBo bo = new CeDimensionRecordBo();
        bo.setDimensionCode("greenhouse-gas");
        bo.setRecordCode("CO2");
        bo.setRecordName("Carbon dioxide");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("Vendor-owned dimension data must be maintained in vendor backend", exception.getMessage());
        verify(dimensionRecordMapper, never()).insert(any(CeDimensionRecord.class));
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

    private CeVendorDimensionListResponse vendorResponse() {
        CeVendorDimensionRecord record = new CeVendorDimensionRecord();
        record.setId(100L);
        record.setDimensionCode("admin-division");
        record.setRecordCode("330000");
        record.setRecordName("浙江省");
        record.setSourceType("vendor");
        record.setStatus("0");

        CeVendorDimensionListResponse response = new CeVendorDimensionListResponse();
        response.setLicenseId("LIC-001");
        response.setDimensionCode("admin-division");
        response.setTotal(1);
        response.setRecords(List.of(record));
        return response;
    }
}
