package org.dromara.carbon.enterprise.dimension.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;

import java.util.Map;

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

    @Test
    void emissionSourceCategoryProjectionKeepsLegacySchemaCompatibleFields() {
        CeDimensionRecordBo query = new CeDimensionRecordBo();
        query.setDimensionCode("emission-source-category");

        String sql = new CeDimensionProjectionSqlProvider().selectPageByDimensionCode(
            Map.of("query", query)
        );

        assertTrue(sql.contains("from ce_emission_source_category"));
        assertTrue(sql.contains("parent_code as parent_code"));
        assertTrue(sql.contains("category_name_en as category_name_en"));
        assertTrue(sql.contains("coalesce(sort_order, ghg_scope_category_sort, id) as sort_order"));
        assertTrue(sql.contains("coalesce(status, '0') as status"));
        assertTrue(sql.contains("from ce_emission_source_category latest"));
    }
}
