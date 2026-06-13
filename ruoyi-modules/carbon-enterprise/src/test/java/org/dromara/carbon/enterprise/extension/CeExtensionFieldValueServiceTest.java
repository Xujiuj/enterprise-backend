package org.dromara.carbon.enterprise.extension;

import org.dromara.carbon.enterprise.domain.CeExtensionField;
import org.dromara.carbon.enterprise.domain.CeExtensionFieldValue;
import org.dromara.carbon.enterprise.domain.bo.CeExtensionFieldValueBo;
import org.dromara.carbon.enterprise.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.mapper.CeExtensionFieldMapper;
import org.dromara.carbon.enterprise.mapper.CeExtensionFieldValueMapper;
import org.dromara.carbon.enterprise.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.mapper.CeIntensityMetricMapper;
import org.dromara.carbon.enterprise.service.impl.CeExtensionFieldValueServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeExtensionFieldValueServiceTest {

    private CeExtensionFieldValueMapper extensionFieldValueMapper;
    private CeExtensionFieldMapper extensionFieldMapper;
    private CeActivityDataMapper activityDataMapper;
    private CeGreenPowerCertificateMapper greenPowerCertificateMapper;
    private CeIntensityMetricMapper intensityMetricMapper;
    private CeExtensionFieldValueServiceImpl service;

    @BeforeEach
    void setUp() {
        extensionFieldValueMapper = mock(CeExtensionFieldValueMapper.class);
        extensionFieldMapper = mock(CeExtensionFieldMapper.class);
        activityDataMapper = mock(CeActivityDataMapper.class);
        greenPowerCertificateMapper = mock(CeGreenPowerCertificateMapper.class);
        intensityMetricMapper = mock(CeIntensityMetricMapper.class);
        service = new CeExtensionFieldValueServiceImpl(
            extensionFieldValueMapper,
            extensionFieldMapper,
            activityDataMapper,
            greenPowerCertificateMapper,
            intensityMetricMapper
        ) {
            @Override
            protected CeExtensionFieldValue toEntity(CeExtensionFieldValueBo bo) {
                CeExtensionFieldValue value = new CeExtensionFieldValue();
                value.setId(bo.getId());
                value.setOwnerTableCode(bo.getOwnerTableCode());
                value.setOwnerRecordId(bo.getOwnerRecordId());
                value.setExtensionFieldId(bo.getExtensionFieldId());
                value.setTextValue(bo.getTextValue());
                return value;
            }
        };
    }

    @Test
    void savesValueWhenExtensionFieldMatchesOwnerTable() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("activity_data", true));
        when(activityDataMapper.selectById(9001L)).thenReturn(new org.dromara.carbon.enterprise.domain.CeActivityData());

        service.insertByBo(validBo("ce_activity_data"));

        ArgumentCaptor<CeExtensionFieldValue> captor = ArgumentCaptor.forClass(CeExtensionFieldValue.class);
        verify(extensionFieldValueMapper).insert(captor.capture());
        assertEquals("ce_activity_data", captor.getValue().getOwnerTableCode());
        assertEquals(501L, captor.getValue().getExtensionFieldId());
    }

    @Test
    void rejectsValueWhenOwnerTableDoesNotMatchModule() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("activity_data", true));
        when(greenPowerCertificateMapper.selectById(anyLong()))
            .thenReturn(new org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate());

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("ce_green_power_certificate")));

        assertEquals("extension field owner table does not match module code", exception.getMessage());
        verify(extensionFieldValueMapper, never()).insert(any(CeExtensionFieldValue.class));
    }

    @Test
    void rejectsValueWhenExtensionFieldModuleIsUnsupported() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("emission_source", true));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("ce_activity_data")));

        assertEquals("Unsupported enterprise extension module code: emission_source", exception.getMessage());
        verify(extensionFieldValueMapper, never()).insert(any(CeExtensionFieldValue.class));
    }

    @Test
    void rejectsValueWhenExtensionFieldIsDisabled() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("activity_data", false));
        when(activityDataMapper.selectById(9001L)).thenReturn(new org.dromara.carbon.enterprise.domain.CeActivityData());

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("ce_activity_data")));

        assertEquals("extension field is disabled", exception.getMessage());
        verify(extensionFieldValueMapper, never()).insert(any(CeExtensionFieldValue.class));
    }

    @Test
    void rejectsValueWhenExtensionFieldIsMissing() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("ce_activity_data")));

        assertEquals("extension field does not exist", exception.getMessage());
        verify(extensionFieldValueMapper, never()).insert(any(CeExtensionFieldValue.class));
    }

    @Test
    void rejectsValueWhenOwnerRecordIsMissing() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("activity_data", true));
        when(activityDataMapper.selectById(9001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("ce_activity_data")));

        assertEquals("extension field owner record does not exist", exception.getMessage());
        verify(extensionFieldValueMapper, never()).insert(any(CeExtensionFieldValue.class));
    }

    @Test
    void savesBatchValuesThroughOneServiceCall() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("activity_data", true));
        when(activityDataMapper.selectById(9001L)).thenReturn(new org.dromara.carbon.enterprise.domain.CeActivityData());
        when(extensionFieldValueMapper.insert(any(CeExtensionFieldValue.class))).thenReturn(1);

        service.saveBatch(List.of(validBo("ce_activity_data")));

        verify(extensionFieldValueMapper).insert(any(CeExtensionFieldValue.class));
    }

    @Test
    void saveBatchUpsertsByOwnerAndFieldInsteadOfPayloadId() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("activity_data", true));
        when(activityDataMapper.selectById(9001L)).thenReturn(new org.dromara.carbon.enterprise.domain.CeActivityData());
        CeExtensionFieldValue existing = new CeExtensionFieldValue();
        existing.setId(888L);
        when(extensionFieldValueMapper.selectOne(any())).thenReturn(existing);
        when(extensionFieldValueMapper.updateById(any(CeExtensionFieldValue.class))).thenReturn(1);

        CeExtensionFieldValueBo bo = validBo("ce_activity_data");
        bo.setId(777L);
        service.saveBatch(List.of(bo));

        ArgumentCaptor<CeExtensionFieldValue> captor = ArgumentCaptor.forClass(CeExtensionFieldValue.class);
        verify(extensionFieldValueMapper, never()).selectById(anyLong());
        verify(extensionFieldValueMapper).updateById(captor.capture());
        assertEquals(888L, captor.getValue().getId());
    }

    @Test
    void saveBatchRejectsZeroRowInsert() {
        when(extensionFieldMapper.selectById(501L)).thenReturn(field("activity_data", true));
        when(activityDataMapper.selectById(9001L)).thenReturn(new org.dromara.carbon.enterprise.domain.CeActivityData());
        when(extensionFieldValueMapper.insert(any(CeExtensionFieldValue.class))).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.saveBatch(List.of(validBo("ce_activity_data"))));

        assertEquals("extension field value batch insert failed", exception.getMessage());
    }

    private CeExtensionFieldValueBo validBo(String ownerTableCode) {
        CeExtensionFieldValueBo bo = new CeExtensionFieldValueBo();
        bo.setOwnerTableCode(ownerTableCode);
        bo.setOwnerRecordId(9001L);
        bo.setExtensionFieldId(501L);
        bo.setTextValue("custom-value");
        return bo;
    }

    private CeExtensionField field(String moduleCode, boolean enabled) {
        CeExtensionField field = new CeExtensionField();
        field.setId(501L);
        field.setModuleCode(moduleCode);
        field.setEnabledFlag(enabled);
        return field;
    }
}
