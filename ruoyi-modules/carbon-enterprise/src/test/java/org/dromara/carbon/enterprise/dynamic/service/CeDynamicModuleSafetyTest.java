package org.dromara.carbon.enterprise.dynamic.service;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class CeDynamicModuleSafetyTest {

    @Test
    void normalizesUnsafeExcelHeadersToBoundedIdentifiers() {
        assertEquals("supplier_name", CeDynamicExcelParser.normalizeIdentifier("Supplier Name", 1, "field"));
        assertEquals("field_2", CeDynamicExcelParser.normalizeIdentifier("供应商名称", 2, "field"));
        assertEquals("field_id", CeDynamicExcelParser.normalizeIdentifier("id", 1, "field"));
        assertTrue(CeDynamicExcelParser.normalizeIdentifier("a".repeat(100), 1, "field").length() <= 48);
    }

    @Test
    void infersSupportedPrimitiveTypes() {
        assertTrue(CeDynamicExcelParser.isNumber("1,234.50"));
        assertFalse(CeDynamicExcelParser.isNumber("12kg"));
        assertTrue(CeDynamicExcelParser.isDate("2026-07-13"));
        assertTrue(CeDynamicExcelParser.isBoolean("是"));
    }

    @Test
    void rejectsSqlIdentifiersOutsideMetadataAllowList() {
        assertEquals("[valid_column]", CeDynamicModuleService.quote("valid_column"));
        assertEquals("[ce_dyn_supplier_data]", CeDynamicModuleService.quote("ce_dyn_supplier_data"));
        assertThrows(ServiceException.class, () -> CeDynamicModuleService.quote("name]; DROP TABLE sys_user;--"));
    }
}
