package org.dromara.carbon.enterprise.extension.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.dromara.carbon.enterprise.extension.domain.CeExtensionField;
import org.dromara.carbon.enterprise.extension.domain.bo.CeExtensionFieldBo;
import org.dromara.carbon.enterprise.extension.mapper.CeExtensionFieldMapper;
import org.dromara.carbon.enterprise.extension.service.impl.CeExtensionFieldServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeExtensionFieldServiceTest {

    private CeExtensionFieldMapper extensionFieldMapper;
    private CeExtensionFieldServiceImpl service;

    @BeforeEach
    void setUp() {
        extensionFieldMapper = mock(CeExtensionFieldMapper.class);
        service = new CeExtensionFieldServiceImpl(extensionFieldMapper) {
            @Override
            protected CeExtensionField toEntity(CeExtensionFieldBo bo) {
                CeExtensionField field = new CeExtensionField();
                field.setId(bo.getId());
                field.setTemplateVersionId(bo.getTemplateVersionId());
                field.setModuleCode(bo.getModuleCode());
                field.setSheetId(bo.getSheetId());
                field.setFieldCode(bo.getFieldCode());
                field.setFieldName(bo.getFieldName());
                field.setValueType(bo.getValueType());
                field.setEnabledFlag(bo.getEnabledFlag());
                return field;
            }
        };
    }

    @Test
    void insertsNormalizedExtensionFieldForEnterpriseOwnedModule() {
        when(extensionFieldMapper.insert(any(CeExtensionField.class))).thenReturn(1);

        service.insertByBo(validBo("activity_data", "Invoice_No", "text"));

        ArgumentCaptor<CeExtensionField> captor = ArgumentCaptor.forClass(CeExtensionField.class);
        verify(extensionFieldMapper).insert(captor.capture());
        assertEquals("invoice_no", captor.getValue().getFieldCode());
        assertTrue(captor.getValue().getEnabledFlag());
    }

    @Test
    void rejectsFieldCodeThatOverridesOriginalField() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("activity_data", "activityValue", "number")));

        assertEquals("extension field code cannot override original field: activityvalue", exception.getMessage());
        verify(extensionFieldMapper, never()).insert(any(CeExtensionField.class));
    }

    @Test
    void rejectsDuplicateFieldCodeWithinSameModule() {
        CeExtensionField existing = new CeExtensionField();
        existing.setId(9L);
        when(extensionFieldMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("green_electricity", "invoice_no", "text")));

        assertEquals("extension field code already exists in module: invoice_no", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedStorageType() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("green_electricity", "invoice_no", "json")));

        assertEquals("Unsupported enterprise extension value type: json", exception.getMessage());
        verify(extensionFieldMapper, never()).insert(any(CeExtensionField.class));
    }

    @Test
    void rejectsUnsupportedModule() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.insertByBo(validBo("vendor_table", "invoice_no", "text")));

        assertEquals("Unsupported enterprise extension module code: vendor_table", exception.getMessage());
        verify(extensionFieldMapper, never()).insert(any(CeExtensionField.class));
    }

    private CeExtensionFieldBo validBo(String moduleCode, String fieldCode, String valueType) {
        CeExtensionFieldBo bo = new CeExtensionFieldBo();
        bo.setTemplateVersionId(1L);
        bo.setModuleCode(moduleCode);
        bo.setSheetId(1L);
        bo.setFieldCode(fieldCode);
        bo.setFieldName("Custom field");
        bo.setValueType(valueType);
        return bo;
    }
}
