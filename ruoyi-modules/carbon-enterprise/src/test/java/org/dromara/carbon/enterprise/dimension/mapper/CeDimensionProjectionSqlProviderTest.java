package org.dromara.carbon.enterprise.dimension.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class CeDimensionProjectionSqlProviderTest {

    @Test
    void companyAddsInsertRowsInsteadOfUpdatingExistingFactoryRecords() {
        String sql = new CeDimensionProjectionSqlProvider().insertByDimensionCode();

        assertTrue(sql.contains("insert into ce_company_factory"));
        assertTrue(sql.contains("company_sk"));
        assertFalse(sql.contains("update ce_company_factory"));
    }
}
