package org.dromara.carbon.enterprise.factor;

import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.domain.CeFactorCacheVersion;
import org.dromara.carbon.enterprise.domain.bo.CeFactorCacheRecordBo;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheVersionMapper;
import org.dromara.carbon.enterprise.service.impl.CeFactorCacheRecordServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeFactorCacheRecordServiceTest {

    private CeFactorCacheRecordMapper factorCacheRecordMapper;
    private CeFactorCacheVersionMapper factorCacheVersionMapper;
    private CeFactorCacheRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        factorCacheRecordMapper = mock(CeFactorCacheRecordMapper.class);
        factorCacheVersionMapper = mock(CeFactorCacheVersionMapper.class);
        service = new CeFactorCacheRecordServiceImpl(factorCacheRecordMapper, factorCacheVersionMapper) {
            @Override
            protected CeFactorCacheRecord toEntity(CeFactorCacheRecordBo bo) {
                CeFactorCacheRecord record = new CeFactorCacheRecord();
                record.setId(bo.getId());
                record.setCacheVersionId(bo.getCacheVersionId());
                record.setFactorCode(bo.getFactorCode());
                record.setFactorName(bo.getFactorName());
                record.setFactorCategory(bo.getFactorCategory());
                record.setFactorValue(bo.getFactorValue());
                record.setFactorUnit(bo.getFactorUnit());
                record.setSourceRef(bo.getSourceRef());
                record.setEnabledFlag(bo.getEnabledFlag());
                return record;
            }
        };
    }

    @Test
    void insertsRecordWhenCacheVersionExists() {
        when(factorCacheVersionMapper.selectById(77L)).thenReturn(new CeFactorCacheVersion());

        service.insertByBo(validBo());

        ArgumentCaptor<CeFactorCacheRecord> captor = ArgumentCaptor.forClass(CeFactorCacheRecord.class);
        verify(factorCacheRecordMapper).insert(captor.capture());
        assertEquals(77L, captor.getValue().getCacheVersionId());
        assertEquals("EF-ELEC-ZJ", captor.getValue().getFactorCode());
    }

    @Test
    void rejectsRecordWhenCacheVersionIsMissing() {
        when(factorCacheVersionMapper.selectById(77L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(validBo()));

        assertEquals("factor cache version does not exist", exception.getMessage());
        verify(factorCacheRecordMapper, never()).insert(any(CeFactorCacheRecord.class));
    }

    private CeFactorCacheRecordBo validBo() {
        CeFactorCacheRecordBo bo = new CeFactorCacheRecordBo();
        bo.setCacheVersionId(77L);
        bo.setFactorCode("EF-ELEC-ZJ");
        bo.setFactorName("Zhejiang grid electricity");
        bo.setFactorCategory("electricity");
        bo.setFactorValue(new BigDecimal("0.5703000000"));
        bo.setFactorUnit("kgCO2e/kWh");
        bo.setEnabledFlag(Boolean.TRUE);
        return bo;
    }
}
