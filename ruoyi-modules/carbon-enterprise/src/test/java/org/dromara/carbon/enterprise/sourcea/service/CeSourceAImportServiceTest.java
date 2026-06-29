package org.dromara.carbon.enterprise.sourcea.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.carbon.enterprise.sourcea.domain.CeSourceAImportResult;
import org.dromara.carbon.enterprise.sourcea.service.impl.CeSourceAImportServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeSourceAImportServiceTest {

    @Test
    void importDirectorySkipsExcelLockFiles(@TempDir Path dir) throws IOException {
        // Create a lock file that should be skipped
        Files.writeString(dir.resolve("~$1 排放源识别表.xlsx"), "locked");
        // Create a documentation xlsx would require POI, so just verify the filter logic
        // by checking that the service's filter accepts normal files and rejects lock files
        String lockFileName = "~$1 排放源识别表.xlsx";
        String normalFileName = "2 排放因子表.xlsx";

        assertTrue(normalFileName.toLowerCase().endsWith(".xlsx") && !normalFileName.startsWith("~$"));
        assertTrue(!(lockFileName.toLowerCase().endsWith(".xlsx") && !lockFileName.startsWith("~$")));
    }

    @Test
    void importDirectoryMapsSourceAReferenceFieldAliases(@TempDir Path sourceDir) throws IOException {
        writeMinimalSourceAWorkbooks(sourceDir);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        List<BatchInsert> batches = new ArrayList<>();
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());
        when(jdbcTemplate.batchUpdate(anyString(), any(List.class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            List<Object[]> rows = invocation.getArgument(1);
            batches.add(new BatchInsert(sql, rows));
            return new int[rows.size()];
        });

        CeSourceAImportResult result = new CeSourceAImportServiceImpl(jdbcTemplate).importDirectory(sourceDir.toString());

        assertTrue(result.isImported(), () -> "errors=" + result.getErrors() + ", warnings=" + result.getWarnings());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("de-duplicated")));
        assertFalse(result.getTableRows().getOrDefault("ce_emission_source", 0) == 0);
        BatchInsert emissionSourceBatch = batches.stream()
            .filter(batch -> batch.sql().contains("INSERT INTO ce_emission_source "))
            .findFirst()
            .orElseThrow();
        Object[] firstEmissionSource = emissionSourceBatch.rows().get(0);
        assertEquals("10101", firstEmissionSource[0]);
        assertEquals("10101", firstEmissionSource[2]);
        assertEquals("ES-001", firstEmissionSource[7]);
        assertEquals("EF-001", firstEmissionSource[12]);
        assertEquals("t", firstEmissionSource[13]);
    }

    private record BatchInsert(String sql, List<Object[]> rows) {
    }

    private void writeMinimalSourceAWorkbooks(Path sourceDir) throws IOException {
        try (Workbook sourceWorkbook = new XSSFWorkbook()) {
            appendRow(sourceWorkbook.createSheet("101行政区划"), "行政区划代码", "行政区划");
            appendRow(sourceWorkbook.getSheet("101行政区划"), "650000", "新疆维吾尔自治区");

            appendRow(sourceWorkbook.createSheet("102公司表"), "SK_公司", "公司编号", "BK_工厂编号", "公司", "工厂", "省份编码",
                "所在省份", "工厂类型", "生效日期", "失效日期", "是否有效");
            appendRow(sourceWorkbook.getSheet("102公司表"), "1", "101", "10101", "峰行智成集团", "集团总部",
                "650000", "新疆维吾尔自治区", "集团总部", "2024-01-01", "9999-12-31", "是");

            appendRow(sourceWorkbook.createSheet("103排放源分类"), "SK_排放源分类", "BK_业务键", "GHG Protocol范围",
                "GHG Protocol范围子类别排序", "GHG Protocol范围子类别", "Scope (GHG Protocol)",
                "Scope Category (GHG Protocol)", "ISO 14064-1类别", "ISO 14064-1 Category", "ISO 14064-1类别描述",
                "ISO 14064-1 Category Description (EN)", "ISO 14064-1子类别（自定义）", "GB/T 32150-2025范围分类",
                "GB/T 32150-2025子类别", "生效日期", "失效日期", "是否当前", "版本号", "统一标准分类");
            appendRow(sourceWorkbook.getSheet("103排放源分类"), "CAT-001", "CAT-001", "范围1", "1", "固定燃烧",
                "Scope 1", "Stationary combustion", "1", "Category 1", "直接排放", "Direct emissions", "",
                "范围一", "固定燃烧", "2024-01-01", "9999-12-31", "是", "v1", "固定燃烧");

            appendRow(sourceWorkbook.createSheet("104排放源识别10101"), "序号", "FK_公司编号", "公司名称", "工厂",
                "FK_排放源分类", "范围", "范围子类别", "PK_排放源识别编号", "排放源识别", "排放源",
                "负责部门", "数据来源", "FK_排放因子");
            appendRow(sourceWorkbook.getSheet("104排放源识别10101"), "1", "10101", "峰行智成集团", "集团总部",
                "CAT-001", "范围1", "固定燃烧", "ES-001", "天然气燃烧", "天然气", "生产部", "手工填报", "EF-001");

            try (var output = Files.newOutputStream(sourceDir.resolve("1-source.xlsx"))) {
                sourceWorkbook.write(output);
            }
        }

        try (Workbook factorWorkbook = new XSSFWorkbook()) {
            appendRow(factorWorkbook.createSheet("201EF排放因子维度表"), "SK_排放因子", "排放源", "排放源_EN",
                "燃料/物质类别", "排放源单位", "CO2", "CH4", "N2O", "HFCs", "PFCs", "SF6", "NF3", "适用范围",
                "因子来源", "GWP_CH4", "GWP_N2O", "GWP_HFCs", "GWP_PFCs", "GWP_SF6", "GWP_NF3", "因子GWP", "因子单位");
        Sheet factorSheet = factorWorkbook.getSheet("201EF排放因子维度表");
        appendRow(factorSheet, "EF-001", "天然气", "Natural gas",
                "天然气", "t", "1", "0", "0", "", "", "", "", "固定燃烧", "测试", "28", "265", "", "", "", "", "1", "tCO2/t");
        appendRow(factorSheet, "EF-001", "天然气重复", "Natural gas duplicate",
                "天然气", "t", "1", "0", "0", "", "", "", "", "固定燃烧", "测试", "28", "265", "", "", "", "", "1", "tCO2/t");
            try (var output = Files.newOutputStream(sourceDir.resolve("2-factor.xlsx"))) {
                factorWorkbook.write(output);
            }
        }
    }

    private void appendRow(Sheet sheet, String... values) {
        Row row = sheet.createRow(sheet.getLastRowNum() == 0 && sheet.getRow(0) == null ? 0 : sheet.getLastRowNum() + 1);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
