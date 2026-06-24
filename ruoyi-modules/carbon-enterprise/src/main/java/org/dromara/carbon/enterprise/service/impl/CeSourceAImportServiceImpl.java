package org.dromara.carbon.enterprise.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dromara.carbon.enterprise.domain.sourcea.CeSourceAImportResult;
import org.dromara.carbon.enterprise.service.ICeSourceAImportService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Source(A) multi-workbook importer.
 */
@RequiredArgsConstructor
@Service
public class CeSourceAImportServiceImpl implements ICeSourceAImportService {

    private static final String MARK = "source(A)";
    private static final Pattern LEADING_INTEGER = Pattern.compile("^\\s*([+-]?\\d+)");
    private static final Pattern LOCAL_DATE = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
    private static final Set<String> GRID_FACTOR_KEYS = Set.of("电网因子", "电力因子表");
    private static final String[] DELETE_ORDER = {
        "ce_activity_data",
        "ce_green_power_certificate",
        "ce_intensity_metric",
        "ce_intensity_denominator_fact",
        "ce_intensity_target",
        "ce_intensity_tolerance",
        "ce_intensity_denominator_rule",
        "ce_emission_source",
        "ce_ef_factor",
        "ce_base_year",
        "ce_emission_source_category",
        "ce_company_factory",
        "ce_admin_division",
        "ce_electricity_factor",
        "ce_electricity_factor_version_map",
        "ce_fuel_factor_calc",
        "ce_electricity_factor_scope",
        "ce_greenhouse_gas"
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CeSourceAImportResult importFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ServiceException("Source(A) upload requires at least one xlsx file");
        }
        SourceAData data = new SourceAData();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "uploaded.xlsx");
            if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                throw new ServiceException("Only .xlsx files are supported: " + filename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                readWorkbook(filename, inputStream, data);
            } catch (IOException e) {
                throw new ServiceException("Failed to read Source(A) workbook: " + filename);
            }
        }
        return importData(data, "upload");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CeSourceAImportResult importDirectory(String sourceDirectory) {
        if (StringUtils.isBlank(sourceDirectory)) {
            throw new ServiceException("Source(A) directory path cannot be blank");
        }
        Path root = Path.of(sourceDirectory).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new ServiceException("Source(A) directory does not exist: " + root);
        }

        SourceAData data = new SourceAData();
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> workbooks = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
            for (Path workbook : workbooks) {
                try (InputStream inputStream = Files.newInputStream(workbook)) {
                    readWorkbook(root.relativize(workbook).toString(), inputStream, data);
                }
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to scan Source(A) directory: " + root);
        }
        return importData(data, "directory");
    }

    @Override
    public CeSourceAImportResult validateCurrentData() {
        CeSourceAImportResult result = new CeSourceAImportResult();
        result.setImported(false);
        result.setSourceMode("database");
        tableCount(result, "ce_admin_division");
        tableCount(result, "ce_company_factory");
        tableCount(result, "ce_emission_source_category");
        tableCount(result, "ce_ef_factor");
        tableCount(result, "ce_emission_source");
        tableCount(result, "ce_activity_data");
        tableCount(result, "ce_green_power_certificate");
        tableCount(result, "ce_intensity_denominator_fact");
        validateDatabaseRelationships(result);
        return result;
    }

    private CeSourceAImportResult importData(SourceAData data, String sourceMode) {
        CeSourceAImportResult result = data.result;
        result.setSourceMode(sourceMode);
        validateSourceRelationships(data, result);
        if (!result.getErrors().isEmpty()) {
            result.setImported(false);
            return result;
        }

        clearSourceAData();
        writeAll(data, result);
        validateDatabaseRelationships(result);
        result.setImported(result.getErrors().isEmpty());
        return result;
    }

    private void readWorkbook(String fileName, InputStream inputStream, SourceAData data) {
        data.result.setWorkbookCount(data.result.getWorkbookCount() + 1);
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<Map<String, Object>> rows = readSheet(sheet);
                if (rows.isEmpty()) {
                    continue;
                }
                data.result.setSheetCount(data.result.getSheetCount() + 1);
                data.result.setSourceRowCount(data.result.getSourceRowCount() + rows.size());
                routeRows(fileName, sheet.getSheetName(), rows, data);
            }
        } catch (Exception e) {
            throw new ServiceException("Failed to parse workbook " + fileName + ": " + e.getMessage());
        }
    }

    private List<Map<String, Object>> readSheet(Sheet sheet) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            return List.of();
        }
        List<String> headers = new ArrayList<>();
        short lastCell = headerRow.getLastCellNum();
        for (int i = 0; i < lastCell; i++) {
            headers.add(text(cellValue(headerRow.getCell(i))));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int rowNum = sheet.getFirstRowNum() + 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                continue;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            boolean nonEmpty = false;
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                if (StringUtils.isBlank(header)) {
                    continue;
                }
                Object value = normalize(cellValue(row.getCell(i)));
                if (value != null) {
                    nonEmpty = true;
                }
                values.put(header, value);
            }
            if (nonEmpty) {
                rows.add(values);
            }
        }
        return rows;
    }

    private Object cellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                ? cell.getLocalDateTimeCellValue().toLocalDate()
                : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null;
        };
    }

    private Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return value;
    }

    private void routeRows(String fileName, String sheetName, List<Map<String, Object>> rows, SourceAData data) {
        if (sheetName.equals("101行政区划")) {
            data.adminRows.addAll(rows);
        } else if (sheetName.equals("102公司表")) {
            data.companyRows.addAll(rows);
        } else if (sheetName.equals("103排放源分类")) {
            data.categoryRows.addAll(rows);
        } else if (sheetName.startsWith("104排放源识别")) {
            data.emissionRows.addAll(rows);
        } else if (sheetName.equals("106基准年维度表")) {
            data.baseYearRows.addAll(rows);
        } else if (sheetName.equals("201EF排放因子维度表")) {
            data.efFactorRows.addAll(rows);
        } else if (sheetName.equals("202EF电力因子维度表")) {
            data.electricityFactorRows.addAll(rows);
        } else if (sheetName.equals("203EF电力因子版本对应")) {
            data.electricityVersionRows.addAll(rows);
        } else if (sheetName.equals("204EF燃料因子计算")) {
            data.fuelFactorRows.addAll(rows);
        } else if (sheetName.equals("205EF电力因子口径维度")) {
            data.electricityScopeRows.addAll(rows);
        } else if (sheetName.equals("206温室气体维度")) {
            data.greenhouseGasRows.addAll(rows);
        } else if (sheetName.equals("501碳排放强度分母维度表")) {
            data.denominatorRuleRows.addAll(rows);
        } else if (sheetName.equals("502强度目标表")) {
            data.intensityTargetRows.addAll(rows);
        } else if (sheetName.equals("504碳排放强度容忍率参数表")) {
            data.intensityToleranceRows.addAll(rows);
        } else if (sheetName.startsWith("503分母事实表")) {
            data.denominatorFactRows.addAll(rows);
        } else if (sheetName.equals("105绿电绿证活动数据")) {
            data.greenPowerRows.addAll(rows);
        } else if (isActivitySheet(fileName, rows)) {
            data.activityRows.addAll(rows);
        }
    }

    private boolean isActivitySheet(String fileName, List<Map<String, Object>> rows) {
        if (!fileName.contains("活动数据表") || rows.isEmpty()) {
            return false;
        }
        Set<String> headers = rows.get(0).keySet();
        return headers.contains("PK_排放源识别编号")
            && headers.contains("年度")
            && headers.contains("月份")
            && headers.contains("活动数据");
    }

    private void validateSourceRelationships(SourceAData data, CeSourceAImportResult result) {
        Set<String> adminCodes = keySet(data.adminRows, row -> text(row.get("行政区划代码")));
        Set<String> factoryCodes = keySet(data.companyRows, row -> text(row.get("BK_工厂编号")));
        Set<String> categoryKeys = keySet(data.categoryRows, row -> text(row.get("SK_排放源分类")));
        Set<String> factorKeys = keySet(data.efFactorRows, row -> text(row.get("SK_排放因子")));
        Set<String> sourceCodes = keySet(data.emissionRows, row -> text(row.get("PK_排放源识别编号")));
        Map<String, Map<String, Object>> sourceByCode = sourceByCode(data);

        requireNoDuplicate(result, "admin.primary", data.adminRows, row -> text(row.get("行政区划代码")));
        requireNoDuplicate(result, "company.factory", data.companyRows, row -> text(row.get("BK_工厂编号")));
        requireNoDuplicate(result, "category.primary", data.categoryRows, row -> text(row.get("SK_排放源分类")));
        requireNoDuplicate(result, "factor.primary", data.efFactorRows, row -> text(row.get("SK_排放因子")));
        requireNoDuplicate(result, "emission-source.primary", data.emissionRows, row -> text(row.get("PK_排放源识别编号")));
        requireNoDuplicate(result, "activity.composite", data.activityRows,
            row -> text(row.get("PK_排放源识别编号")) + "|" + text(row.get("年度")) + "|" + text(row.get("月份")));
        requireNoDuplicate(result, "denominator-fact.composite", data.denominatorFactRows,
            row -> text(row.get("工厂编号")) + "|" + text(row.get("年份")) + "|" + text(row.get("月份")) + "|" + text(row.get("分母度量名称")));

        countOrphans(result, "company.province", data.companyRows, row -> text(row.get("省份编码")), adminCodes);
        countOrphans(result, "emission-source.company", data.emissionRows, row -> text(row.get("FK_公司编号")), factoryCodes);
        countOrphans(result, "emission-source.category", data.emissionRows, row -> text(row.get("FK_排放源分类")), categoryKeys);
        countOrphans(result, "activity.source", data.activityRows, row -> text(row.get("PK_排放源识别编号")), sourceCodes);
        countOrphans(result, "activity.company", data.activityRows, row -> text(row.get("FK_公司编号")), factoryCodes);
        countOrphans(result, "activity.category", data.activityRows, row -> text(row.get("FK_排放源分类")), categoryKeys);
        countOrphans(result, "green-power.factory", data.greenPowerRows, row -> text(row.get("FK_工厂编号")), factoryCodes);
        countOrphans(result, "green-power.category", data.greenPowerRows, row -> text(row.get("FK_排放源分类")), categoryKeys);
        countOrphans(result, "denominator-fact.factory", data.denominatorFactRows, row -> text(row.get("工厂编号")), factoryCodes);

        Set<String> nonEfEmissionFactors = nonEfFactors(data.emissionRows, row -> text(row.get("FK_排放因子")), factorKeys);
        Set<String> nonEfActivityFactors = nonEfFactors(data.activityRows, row -> activityFactorKey(row, sourceByCode, factorKeys), factorKeys);
        Set<String> nonEfGreenFactors = nonEfFactors(data.greenPowerRows, row -> text(row.get("FK_排放因子")), factorKeys);
        if (!nonEfEmissionFactors.isEmpty()) {
            result.warn("Emission source factor_key contains non-201EF dynamic factor references: " + nonEfEmissionFactors);
        }
        if (!nonEfActivityFactors.isEmpty()) {
            result.warn("Activity factor_key contains non-201EF dynamic factor references: " + nonEfActivityFactors);
        }
        if (!nonEfGreenFactors.isEmpty()) {
            result.warn("Green power factor_key contains non-201EF dynamic factor references: " + nonEfGreenFactors);
        }
        int invalidActivityFactorRows = invalidActivityFactorRows(data.activityRows, sourceByCode, factorKeys);
        if (invalidActivityFactorRows > 0) {
            result.issue("warn", "activity.factor-invalid-reference",
                "Activity factor_key is not a known 201EF or dynamic electricity key; emission-source master factor_key is used", invalidActivityFactorRows);
        }

        long negativeActivityRows = data.activityRows.stream()
            .map(row -> decimal(row.get("活动数据")))
            .filter(Objects::nonNull)
            .filter(value -> value.signum() < 0)
            .count();
        if (negativeActivityRows > 0) {
            result.issue("warn", "activity.negative-value",
                "Negative activity values are present and preserved for explicit negative-emission sources", (int) negativeActivityRows);
        }
    }

    private void clearSourceAData() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            jdbcTemplate.update("""
                DELETE FROM ce_extension_field_value
                WHERE owner_table_code = 'ce_activity_data'
                  AND owner_record_id IN (SELECT id FROM ce_activity_data WHERE remark = ?)
                """, MARK);
            for (String table : DELETE_ORDER) {
                jdbcTemplate.update("DELETE FROM " + table + " WHERE remark = ?", MARK);
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void writeAll(SourceAData data, CeSourceAImportResult result) {
        insertRows(result, "ce_admin_division", List.of("division_code", "division_name", "remark"),
            mapRows(data.adminRows, row -> values(row.get("行政区划代码"), row.get("行政区划"), MARK)));

        insertRows(result, "ce_company_factory", List.of(
                "company_sk", "company_code", "factory_code", "company_name", "factory_name", "province_code", "province_name",
                "factory_type", "industry_section_code", "industry_section_name", "industry_division_code", "industry_division_name",
                "industry_group_code", "industry_group_name", "industry_class_code", "industry_class_name", "effective_date", "expiry_date",
                "is_active", "remark"),
            mapRows(data.companyRows, row -> values(row.get("SK_公司"), row.get("公司编号"), row.get("BK_工厂编号"), row.get("公司"),
                row.get("工厂"), row.get("省份编码"), row.get("所在省份"), row.get("工厂类型"), row.get("行业门类代码"),
                row.get("行业门类名称"), row.get("行业大类代码"), row.get("行业大类名称"), row.get("行业中类代码"), row.get("行业中类名称"),
                row.get("行业小类代码"), row.get("行业小类名称"), sqlDate(row.get("生效日期")), sqlDate(row.get("失效日期")),
                activeFlag(row.get("是否有效")), MARK)));

        insertRows(result, "ce_emission_source_category", List.of(
                "category_sk", "business_key", "ghg_scope", "ghg_scope_category_sort", "ghg_scope_category", "ghg_scope_en",
                "ghg_scope_category_en", "iso_category", "iso_category_en", "iso_category_description", "iso_category_description_en",
                "iso_custom_subcategory", "gb_scope_category", "gb_subcategory", "effective_date", "expiry_date", "is_current",
                "version_no", "unified_standard_category", "remark"),
            mapRows(data.categoryRows, row -> values(row.get("SK_排放源分类"), row.get("BK_业务键"), row.get("GHG Protocol范围"),
                leadingInteger(row.get("GHG Protocol范围子类别排序")), row.get("GHG Protocol范围子类别"), row.get("Scope (GHG Protocol)"),
                row.get("Scope Category (GHG Protocol)"), row.get("ISO 14064-1类别"), row.get("ISO 14064-1 Category"),
                row.get("ISO 14064-1类别描述"), row.get("ISO 14064-1 Category Description (EN)"), row.get("ISO 14064-1子类别（自定义）"),
                row.get("GB/T 32150-2025范围分类"), row.get("GB/T 32150-2025子类别"), sqlDate(row.get("生效日期")),
                sqlDate(row.get("失效日期")), activeFlag(row.get("是否当前")), row.get("版本号"), row.get("统一标准分类"), MARK)));

        insertRows(result, "ce_base_year", List.of("factory_code", "factory_name", "base_year", "enabled_flag", "remark"),
            data.companyRows.stream()
                .flatMap(company -> data.baseYearRows.stream()
                    .map(baseYear -> values(company.get("BK_工厂编号"), company.get("工厂"), integer(baseYear.get("基准年")),
                        enabledFlag(baseYear.get("是否当前基准")), MARK)))
                .filter(row -> row.get(0) != null && row.get(2) != null)
                .toList());

        insertRows(result, "ce_ef_factor", List.of(
                "factor_sk", "emission_source_name", "emission_source_name_en", "fuel_material_category", "source_unit",
                "co2", "ch4", "n2o", "hfcs", "pfcs", "sf6", "nf3", "applicable_scope", "factor_source",
                "gwp_ch4", "gwp_n2o", "gwp_hfcs", "gwp_pfcs", "gwp_sf6", "gwp_nf3", "factor_gwp", "factor_unit", "remark"),
            mapRows(data.efFactorRows, row -> values(row.get("SK_排放因子"), row.get("排放源"), row.get("排放源_EN"),
                row.get("燃料/物质类别"), row.get("排放源单位"), decimal(row.get("CO2")), decimal(row.get("CH4")), decimal(row.get("N2O")),
                decimal(row.get("HFCs")), decimal(row.get("PFCs")), decimal(row.get("SF6")), decimal(row.get("NF3")), row.get("适用范围"),
                row.get("因子来源"), decimal(row.get("GWP_CH4")), decimal(row.get("GWP_N2O")), decimal(row.get("GWP_HFCs")),
                decimal(row.get("GWP_PFCs")), decimal(row.get("GWP_SF6")), decimal(row.get("GWP_NF3")), decimal(row.get("因子GWP")),
                row.get("因子单位"), MARK)));

        insertElectricityDimensions(data, result);
        insertEmissionRows(data, result);
        insertActivityRows(data, result);
        insertGreenPowerRows(data, result);
        insertIntensityRows(data, result);
    }

    private void insertElectricityDimensions(SourceAData data, CeSourceAImportResult result) {
        insertRows(result, "ce_electricity_factor", List.of(
                "version_province_code", "factor_version", "division_code", "division_name", "region_name", "province_factor",
                "region_factor", "national_factor", "non_fossil_excluded_factor", "national_fossil_power_factor", "remark"),
            mapRows(data.electricityFactorRows, row -> values(row.get("PK_因子版本省份代码"), row.get("因子版本"),
                row.get("行政区划代码"), row.get("行政区划"), row.get("区域划分"), decimal(row.get("省级因子（kgCO2/kWh)")),
                decimal(row.get("区域因子（kgCO2/kWh)")), decimal(row.get("全国因子（kgCO2/kWh）")),
                decimal(row.get("不包括市场化交易的非化石能源电量因子（kgCO2/kWh）")),
                decimal(row.get("全国化石能源电力二氧化碳排放因子（kgCO2/kWh）")), MARK)));
        insertRows(result, "ce_electricity_factor_version_map", List.of("effective_year", "factor_version", "remark"),
            mapRows(data.electricityVersionRows, row -> values(integer(row.get("年份")), row.get("对应因子版本"), MARK)));
        insertRows(result, "ce_fuel_factor_calc", List.of("calc_key", "fuel_material_category", "lower_heat_value", "lower_heat_value_unit",
                "carbon_content", "carbon_content_unit", "oxidation_rate", "co2_factor", "factor_unit", "factor_source", "effective_date",
                "expiry_date", "enabled_flag", "remark"),
            mapRows(data.fuelFactorRows, row -> values(fuelFactorCalcKey(row), row.get("三类"), decimal(row.get("低位发热量 (TJ/10⁸ Nm³)")),
                "TJ/10^8 Nm3", decimal(row.get("因子 (tCO₂/TJ)")), "tCO2/TJ", null,
                decimal(row.get("因子（转换）")), row.get("因子单位"), row.get("因子来源"), null, null, 1, MARK))
                .stream().filter(row -> row.get(0) != null).toList());
        insertRows(result, "ce_electricity_factor_scope", List.of("scope_key", "scope_name", "remark"),
            mapRows(data.electricityScopeRows, row -> values(row.get("因子口径Key"), row.get("因子口径"), MARK)));
        insertRows(result, "ce_greenhouse_gas", List.of("gas_code", "gas_name", "gas_name_en", "remark"),
            mapRows(data.greenhouseGasRows, row -> values(row.get("GasKey"), row.get("气体"), null, MARK)));
    }

    private void insertEmissionRows(SourceAData data, CeSourceAImportResult result) {
        insertRows(result, "ce_emission_source", List.of("company_code", "company_name", "factory_name", "source_category_key",
                "scope_name", "scope_subcategory", "source_identification_code", "source_identification_name", "emission_source_name",
                "responsible_dept", "data_source", "factor_key", "enabled_flag", "remark"),
            mapRows(data.emissionRows, row -> values(row.get("FK_公司编号"), row.get("公司名称"), row.get("工厂"), row.get("FK_排放源分类"),
                row.get("范围"), row.get("范围子类别"), row.get("PK_排放源识别编号"), row.get("排放源识别"), row.get("排放源"),
                row.get("负责部门"), row.get("数据来源"), row.get("FK_排放因子"), 1, MARK)));
    }

    private void insertActivityRows(SourceAData data, CeSourceAImportResult result) {
        Map<String, Map<String, Object>> sourceByCode = sourceByCode(data);
        insertRows(result, "ce_activity_data", List.of("source_sheet_code", "source_identification_code", "company_code", "company_name",
                "factory_name", "source_category_key", "scope_name", "scope_subcategory", "source_identification_name", "emission_source_name",
                "activity_unit", "activity_year", "activity_month", "activity_date", "activity_value", "responsible_dept", "data_source",
                "source_remark", "factor_key", "data_status", "remark"),
            mapRows(data.activityRows, row -> {
                String sourceCode = text(row.get("PK_排放源识别编号"));
                Map<String, Object> source = sourceByCode.getOrDefault(sourceCode, Map.of());
                Integer year = integer(row.get("年度"));
                Integer month = integer(row.get("月份"));
                return values("source-a-activity", sourceCode, first(row.get("FK_公司编号"), source.get("FK_公司编号")),
                    first(row.get("公司名称"), source.get("公司名称")), first(row.get("工厂"), source.get("工厂")),
                    first(row.get("FK_排放源分类"), source.get("FK_排放源分类")), first(row.get("范围"), source.get("范围")),
                    first(row.get("范围子类别"), source.get("范围子类别")), first(row.get("排放源识别"), source.get("排放源识别")),
                    first(row.get("排放源"), source.get("排放源")), row.get("单位"), year, month, sqlDate(first(row.get("日期"), firstDay(year, month))),
                    decimal(row.get("活动数据")), first(row.get("负责部门"), source.get("负责部门")), first(row.get("数据来源"), source.get("数据来源")),
                    row.get("备注"), activityFactorKey(row, sourceByCode, keySet(data.efFactorRows, factor -> text(factor.get("SK_排放因子")))), "submitted", MARK);
            }));
    }

    private void insertGreenPowerRows(SourceAData data, CeSourceAImportResult result) {
        insertRows(result, "ce_green_power_certificate", List.of("factory_code", "factory_name", "activity_year", "activity_month",
                "source_category_key", "scope_name", "scope_subcategory", "electricity_type", "electricity_type_desc", "quantity_kwh",
                "certificate_code", "issuing_org", "purchase_date", "expiry_date", "power_grid_region", "offset_power_source", "data_source",
                "source_remark", "emission_source_name", "factor_key", "proof_status", "remark"),
            mapRows(data.greenPowerRows, row -> values(row.get("FK_工厂编号"), row.get("工厂名称"), integer(row.get("年度")), integer(row.get("月份")),
                row.get("FK_排放源分类"), row.get("范围"), row.get("范围子类别"), row.get("电力类型"), row.get("电力类型说明"),
                decimal(row.get("数量_kWh")), row.get("证书编号"), row.get("证书签发机构"), sqlDate(row.get("购买日期")), sqlDate(row.get("到期日期")),
                row.get("对应电网区域"), row.get("抵消电力来源"), row.get("数据来源"), row.get("备注"), row.get("排放源"), row.get("FK_排放因子"),
                "verified", MARK)));
    }

    private void insertIntensityRows(SourceAData data, CeSourceAImportResult result) {
        insertRows(result, "ce_intensity_denominator_rule", List.of("denominator_rule_key", "factory_type", "denominator_type",
                "denominator_metric_name", "intensity_unit_display", "enabled_flag", "remark"),
            mapRows(data.denominatorRuleRows, row -> values(row.get("分母维度Key"), row.get("工厂类型"), row.get("分母类型"),
                row.get("分母度量名称"), row.get("强度单位展示"), enabledFlag(row.get("是否启用")), MARK))
                .stream().filter(row -> row.get(0) != null && row.get(1) != null && row.get(2) != null).toList());
        insertRows(result, "ce_intensity_target", List.of("factory_type", "target_year", "target_value", "unit_name", "remark"),
            mapRows(data.intensityTargetRows, row -> values(row.get("工厂类型"), integer(row.get("年份")), decimal(row.get("强度目标值")), row.get("单位"), MARK)));
        insertRows(result, "ce_intensity_tolerance", List.of("tolerance_key", "industry_section", "tolerance_rate", "enabled_flag", "remark"),
            mapRows(data.intensityToleranceRows, row -> values(row.get("容忍率Key"), row.get("行业门类"), decimal(row.get("容忍率")),
                enabledFlag(row.get("是否启用")), MARK)));
        insertRows(result, "ce_intensity_denominator_fact", List.of("source_sheet_code", "factory_code", "factory_name", "factory_type",
                "fact_year", "fact_month", "denominator_type", "denominator_metric_name", "denominator_value", "unit_name", "data_source", "remark"),
            mapRows(data.denominatorFactRows, row -> values("503", row.get("工厂编号"), row.get("工厂名称"), row.get("工厂类型"),
                integer(row.get("年份")), integer(row.get("月份")), row.get("分母类型"), row.get("分母度量名称"), decimal(row.get("分母值")),
                row.get("单位"), row.get("数据来源"), MARK)));
    }

    private void insertRows(CeSourceAImportResult result, String table, List<String> columns, List<List<Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        String placeholders = columns.stream().map(column -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        List<Object[]> batchArgs = rows.stream().map(row -> row.toArray(new Object[0])).toList();
        jdbcTemplate.batchUpdate(sql, batchArgs);
        result.addTableRows(table, rows.size());
    }

    private void validateDatabaseRelationships(CeSourceAImportResult result) {
        checkSqlCount(result, "db.company.province",
            "SELECT COUNT(*) FROM ce_company_factory cf LEFT JOIN ce_admin_division ad ON ad.division_code = cf.province_code WHERE cf.remark = ? AND cf.province_code IS NOT NULL AND ad.id IS NULL");
        checkSqlCount(result, "db.emission-source.company",
            "SELECT COUNT(*) FROM ce_emission_source es LEFT JOIN ce_company_factory cf ON cf.factory_code = es.company_code WHERE es.remark = ? AND cf.id IS NULL");
        checkSqlCount(result, "db.emission-source.category",
            "SELECT COUNT(*) FROM ce_emission_source es LEFT JOIN ce_emission_source_category cat ON cat.category_sk = es.source_category_key WHERE es.remark = ? AND cat.id IS NULL");
        checkSqlCount(result, "db.activity.source",
            "SELECT COUNT(*) FROM ce_activity_data ad LEFT JOIN ce_emission_source es ON es.source_identification_code = ad.source_identification_code WHERE ad.remark = ? AND es.id IS NULL");
        checkSqlCount(result, "db.activity.company",
            "SELECT COUNT(*) FROM ce_activity_data ad LEFT JOIN ce_company_factory cf ON cf.factory_code = ad.company_code WHERE ad.remark = ? AND cf.id IS NULL");
        checkSqlCount(result, "db.green-power.factory",
            "SELECT COUNT(*) FROM ce_green_power_certificate gp LEFT JOIN ce_company_factory cf ON cf.factory_code = gp.factory_code WHERE gp.remark = ? AND cf.id IS NULL");
        checkSqlCount(result, "db.denominator-fact.factory",
            "SELECT COUNT(*) FROM ce_intensity_denominator_fact df LEFT JOIN ce_company_factory cf ON cf.factory_code = df.factory_code WHERE df.remark = ? AND cf.id IS NULL");
        int dynamicFactorRows = count("""
            SELECT COUNT(*) FROM (
                SELECT factor_key FROM ce_emission_source WHERE remark = ?
                UNION ALL SELECT factor_key FROM ce_activity_data WHERE remark = ?
                UNION ALL SELECT factor_key FROM ce_green_power_certificate WHERE remark = ?
            ) x LEFT JOIN ce_ef_factor f ON f.factor_sk = x.factor_key
            WHERE x.factor_key IS NOT NULL AND x.factor_key <> '' AND f.id IS NULL
            """, MARK, MARK, MARK);
        if (dynamicFactorRows > 0) {
            result.issue("warn", "db.factor.dynamic-reference",
                "factor_key has dynamic non-201EF references retained for electricity factor resolution", dynamicFactorRows);
        }
    }

    private void checkSqlCount(CeSourceAImportResult result, String code, String sql) {
        int count = count(sql, MARK);
        if (count > 0) {
            result.issue("error", code, "Relationship validation failed: " + code, count);
        }
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private void tableCount(CeSourceAImportResult result, String table) {
        result.addTableRows(table, count("SELECT COUNT(*) FROM " + table + " WHERE remark = ?", MARK));
    }

    private Set<String> keySet(List<Map<String, Object>> rows, Function<Map<String, Object>, String> keyResolver) {
        return rows.stream().map(keyResolver).filter(StringUtils::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void requireNoDuplicate(CeSourceAImportResult result, String code, List<Map<String, Object>> rows,
                                    Function<Map<String, Object>, String> keyResolver) {
        Map<String, Long> counts = rows.stream()
            .map(keyResolver)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        int duplicateRows = counts.values().stream().filter(count -> count > 1).mapToInt(count -> (int) (count - 1)).sum();
        if (duplicateRows > 0) {
            result.issue("error", code, "Duplicate Source(A) key: " + code, duplicateRows);
        }
    }

    private void countOrphans(CeSourceAImportResult result, String code, List<Map<String, Object>> rows,
                              Function<Map<String, Object>, String> keyResolver, Set<String> parentKeys) {
        int count = (int) rows.stream()
            .map(keyResolver)
            .filter(StringUtils::isNotBlank)
            .filter(key -> !parentKeys.contains(key))
            .count();
        if (count > 0) {
            result.issue("error", code, "Source(A) orphan relationship: " + code, count);
        }
    }

    private Set<String> nonEfFactors(List<Map<String, Object>> rows, Function<Map<String, Object>, String> keyResolver, Set<String> efKeys) {
        return rows.stream()
            .map(keyResolver)
            .filter(StringUtils::isNotBlank)
            .filter(key -> !efKeys.contains(key))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Map<String, Object>> sourceByCode(SourceAData data) {
        return data.emissionRows.stream()
            .filter(row -> StringUtils.isNotBlank(text(row.get("PK_排放源识别编号"))))
            .collect(Collectors.toMap(row -> text(row.get("PK_排放源识别编号")), Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private String activityFactorKey(Map<String, Object> row, Map<String, Map<String, Object>> sourceByCode, Set<String> factorKeys) {
        String activityFactor = text(row.get("FK_排放因子"));
        if (isKnownFactorKey(activityFactor, factorKeys)) {
            return activityFactor;
        }
        Map<String, Object> source = sourceByCode.get(text(row.get("PK_排放源识别编号")));
        return text(first(source == null ? null : source.get("FK_排放因子"), row.get("FK_排放因子")));
    }

    private int invalidActivityFactorRows(List<Map<String, Object>> rows, Map<String, Map<String, Object>> sourceByCode, Set<String> factorKeys) {
        return (int) rows.stream().filter(row -> {
            String activityFactor = text(row.get("FK_排放因子"));
            Map<String, Object> source = sourceByCode.get(text(row.get("PK_排放源识别编号")));
            String sourceFactor = source == null ? null : text(source.get("FK_排放因子"));
            return StringUtils.isNotBlank(activityFactor)
                && StringUtils.isNotBlank(sourceFactor)
                && !isKnownFactorKey(activityFactor, factorKeys)
                && isKnownFactorKey(sourceFactor, factorKeys);
        }).count();
    }

    private boolean isKnownFactorKey(String factorKey, Set<String> factorKeys) {
        return StringUtils.isNotBlank(factorKey) && (factorKeys.contains(factorKey) || GRID_FACTOR_KEYS.contains(factorKey));
    }

    private List<List<Object>> mapRows(List<Map<String, Object>> rows, Function<Map<String, Object>, List<Object>> mapper) {
        return rows.stream().map(mapper).filter(row -> row.stream().anyMatch(Objects::nonNull)).toList();
    }

    private List<Object> values(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private Object first(Object first, Object second) {
        return first == null ? second : first;
    }

    private String fuelFactorCalcKey(Map<String, Object> row) {
        String level1 = text(row.get("一类"));
        String level2 = text(row.get("二类"));
        String level3 = text(row.get("三类"));
        String level4 = text(row.get("四类"));
        if (StringUtils.isBlank(level1) || StringUtils.isBlank(level2) || StringUtils.isBlank(level3) || StringUtils.isBlank(level4)) {
            return null;
        }
        return String.join("|", level1, level2, level3, level4);
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal && decimal.scale() <= 0) {
            return decimal.toPlainString();
        }
        return String.valueOf(value).trim();
    }

    private Integer integer(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = text(value);
        return StringUtils.isBlank(text) ? null : new BigDecimal(text).intValue();
    }

    private Integer leadingInteger(Object value) {
        if (value == null || value instanceof Number) {
            return integer(value);
        }
        String text = text(value);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        Matcher matcher = LEADING_INTEGER.matcher(text);
        return matcher.find() ? new BigDecimal(matcher.group(1)).intValue() : null;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = text(value);
        return StringUtils.isBlank(text) ? null : new BigDecimal(text);
    }

    private Date sqlDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return Date.valueOf(localDate);
        }
        if (value instanceof java.util.Date date) {
            return new Date(date.getTime());
        }
        String text = text(value);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        if (text.matches("\\d{4}-\\d{1,2}")) {
            return Date.valueOf(YearMonth.parse(text).atDay(1));
        }
        Matcher matcher = LOCAL_DATE.matcher(text);
        if (matcher.matches()) {
            return Date.valueOf(LocalDate.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))));
        }
        return Date.valueOf(LocalDate.parse(text));
    }

    private LocalDate firstDay(Integer year, Integer month) {
        if (year == null || month == null) {
            return null;
        }
        return LocalDate.of(year, month, 1);
    }

    private String activeFlag(Object value) {
        String text = text(value);
        if (StringUtils.isBlank(text)) {
            return "N";
        }
        return ("是".equals(text) || "Y".equalsIgnoreCase(text) || "1".equals(text) || "true".equalsIgnoreCase(text)) ? "Y" : "N";
    }

    private Integer enabledFlag(Object value) {
        return "Y".equals(activeFlag(value)) ? 1 : 0;
    }

    private static final class SourceAData {
        private final CeSourceAImportResult result = new CeSourceAImportResult();
        private final List<Map<String, Object>> adminRows = new ArrayList<>();
        private final List<Map<String, Object>> companyRows = new ArrayList<>();
        private final List<Map<String, Object>> categoryRows = new ArrayList<>();
        private final List<Map<String, Object>> emissionRows = new ArrayList<>();
        private final List<Map<String, Object>> baseYearRows = new ArrayList<>();
        private final List<Map<String, Object>> efFactorRows = new ArrayList<>();
        private final List<Map<String, Object>> electricityFactorRows = new ArrayList<>();
        private final List<Map<String, Object>> electricityVersionRows = new ArrayList<>();
        private final List<Map<String, Object>> fuelFactorRows = new ArrayList<>();
        private final List<Map<String, Object>> electricityScopeRows = new ArrayList<>();
        private final List<Map<String, Object>> greenhouseGasRows = new ArrayList<>();
        private final List<Map<String, Object>> denominatorRuleRows = new ArrayList<>();
        private final List<Map<String, Object>> intensityTargetRows = new ArrayList<>();
        private final List<Map<String, Object>> intensityToleranceRows = new ArrayList<>();
        private final List<Map<String, Object>> denominatorFactRows = new ArrayList<>();
        private final List<Map<String, Object>> greenPowerRows = new ArrayList<>();
        private final List<Map<String, Object>> activityRows = new ArrayList<>();
    }
}
