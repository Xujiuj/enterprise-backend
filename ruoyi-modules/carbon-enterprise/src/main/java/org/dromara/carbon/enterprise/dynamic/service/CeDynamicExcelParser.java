package org.dromara.carbon.enterprise.dynamic.service;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.dynamic.domain.CeDynamicModels;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads arbitrary xlsx workbooks and derives safe module/field metadata.
 */
@Component
public class CeDynamicExcelParser {

    private static final int MAX_FILE_SIZE = 20 * 1024 * 1024;
    private static final int MAX_COLUMNS = 80;
    private static final int MAX_SAMPLE_ROWS = 5;
    private static final int MAX_INFERENCE_ROWS = 200;
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[^a-zA-Z0-9_]+");
    private static final Set<String> RESERVED_COLUMNS = Set.of("id", "create_by", "create_time", "update_by", "update_time");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("yyyy年M月d日")
    );

    public CeDynamicModels.WorkbookPreview preview(MultipartFile file) {
        List<CeDynamicModels.ParsedSheet> parsedSheets = parse(file);
        CeDynamicModels.WorkbookPreview preview = new CeDynamicModels.WorkbookPreview();
        preview.setFileName(file.getOriginalFilename());
        Set<String> moduleCodes = new HashSet<>();
        List<CeDynamicModels.SheetDefinition> sheets = new ArrayList<>();
        for (CeDynamicModels.ParsedSheet parsedSheet : parsedSheets) {
            CeDynamicModels.SheetDefinition sheet = toDefinition(parsedSheet);
            sheet.setModuleCode(uniqueIdentifier(sheet.getModuleCode(), moduleCodes));
            sheets.add(sheet);
        }
        preview.setSheets(sheets);
        return preview;
    }

    public List<CeDynamicModels.ParsedSheet> parse(MultipartFile file) {
        validateFile(file);
        List<RawRow> rawRows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            FastExcel.read(inputStream, new RawRowListener(rawRows))
                .autoCloseStream(false)
                .headRowNumber(0)
                .doReadAll();
        } catch (IOException e) {
            throw new ServiceException("读取 Excel 文件失败");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("解析 Excel 文件失败: " + e.getMessage());
        }
        return groupSheets(rawRows);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传非空的 Excel 文件");
        }
        String fileName = StringUtils.trimToEmpty(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xlsx")) {
            throw new ServiceException("仅支持 .xlsx 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceException("Excel 文件不能超过 20MB");
        }
    }

    private List<CeDynamicModels.ParsedSheet> groupSheets(List<RawRow> rawRows) {
        Map<Integer, List<RawRow>> grouped = new LinkedHashMap<>();
        for (RawRow row : rawRows) {
            grouped.computeIfAbsent(row.sheetNo(), ignored -> new ArrayList<>()).add(row);
        }
        List<CeDynamicModels.ParsedSheet> sheets = new ArrayList<>();
        for (Map.Entry<Integer, List<RawRow>> entry : grouped.entrySet()) {
            List<RawRow> rows = entry.getValue();
            if (rows.isEmpty()) {
                continue;
            }
            RawRow headerRow = rows.get(0);
            List<String> headers = orderedValues(headerRow.values());
            int lastNonBlank = lastNonBlankIndex(headers);
            if (lastNonBlank < 0) {
                continue;
            }
            if (lastNonBlank + 1 > MAX_COLUMNS) {
                throw new ServiceException("工作表 " + headerRow.sheetName() + " 超过最大列数 " + MAX_COLUMNS);
            }
            headers = new ArrayList<>(headers.subList(0, lastNonBlank + 1));
            validateHeaders(headers, headerRow.sheetName());
            int columnCount = headers.size();
            List<Map<Integer, String>> dataRows = rows.stream()
                .skip(1)
                .map(RawRow::values)
                .filter(values -> !isBlankRow(values, columnCount))
                .map(values -> (Map<Integer, String>) new LinkedHashMap<>(values))
                .toList();
            sheets.add(new CeDynamicModels.ParsedSheet(entry.getKey(), headerRow.sheetName(), headers, dataRows));
        }
        if (sheets.isEmpty()) {
            throw new ServiceException("Excel 中没有可生成页面的工作表");
        }
        return sheets;
    }

    private CeDynamicModels.SheetDefinition toDefinition(CeDynamicModels.ParsedSheet parsedSheet) {
        CeDynamicModels.SheetDefinition sheet = new CeDynamicModels.SheetDefinition();
        sheet.setSheetNo(parsedSheet.sheetNo());
        sheet.setSheetName(parsedSheet.sheetName());
        sheet.setModuleName(parsedSheet.sheetName());
        sheet.setModuleCode(normalizeIdentifier(parsedSheet.sheetName(), parsedSheet.sheetNo() + 1, "page"));
        sheet.setRowCount(parsedSheet.rows().size());

        Set<String> fieldCodes = new HashSet<>();
        List<CeDynamicModels.FieldDefinition> fields = new ArrayList<>();
        for (int index = 0; index < parsedSheet.headers().size(); index++) {
            String header = parsedSheet.headers().get(index).trim();
            String fieldCode = uniqueIdentifier(normalizeIdentifier(header, index + 1, "field"), fieldCodes);
            List<String> values = columnValues(parsedSheet.rows(), index);
            CeDynamicModels.FieldDefinition field = inferField(fieldCode, header, index, values);
            fields.add(field);
        }
        sheet.setFields(fields);
        sheet.setSampleRows(sampleRows(parsedSheet.rows(), fields));
        return sheet;
    }

    private CeDynamicModels.FieldDefinition inferField(String fieldCode, String fieldName, int index, List<String> values) {
        CeDynamicModels.FieldDefinition field = new CeDynamicModels.FieldDefinition();
        field.setFieldCode(fieldCode);
        field.setDbColumn(fieldCode);
        field.setFieldName(fieldName);
        field.setSortOrder(index + 1);
        field.setSearchable(index < 4);

        List<String> nonBlankValues = values.stream().filter(StringUtils::isNotBlank).limit(MAX_INFERENCE_ROWS).toList();
        int maxLength = nonBlankValues.stream().mapToInt(String::length).max().orElse(0);
        if (!nonBlankValues.isEmpty() && nonBlankValues.stream().allMatch(CeDynamicExcelParser::isBoolean)) {
            field.setValueType("boolean");
            field.setUiType("switch");
        } else if (!nonBlankValues.isEmpty() && nonBlankValues.stream().allMatch(CeDynamicExcelParser::isNumber)) {
            field.setValueType("number");
            field.setUiType("number");
        } else if (!nonBlankValues.isEmpty() && nonBlankValues.stream().allMatch(CeDynamicExcelParser::isDate)) {
            field.setValueType("date");
            field.setUiType("date");
        } else {
            field.setValueType("text");
            field.setUiType(maxLength > 255 ? "textarea" : "input");
            field.setMaxLength(normalizeMaxLength(maxLength));
        }
        return field;
    }

    private List<Map<String, Object>> sampleRows(List<Map<Integer, String>> rows,
                                                  List<CeDynamicModels.FieldDefinition> fields) {
        List<Map<String, Object>> samples = new ArrayList<>();
        for (Map<Integer, String> row : rows.stream().limit(MAX_SAMPLE_ROWS).toList()) {
            Map<String, Object> sample = CeDynamicModels.orderedRow();
            for (int index = 0; index < fields.size(); index++) {
                sample.put(fields.get(index).getFieldCode(), StringUtils.trimToEmpty(row.get(index)));
            }
            samples.add(sample);
        }
        return samples;
    }

    private static List<String> columnValues(List<Map<Integer, String>> rows, int columnIndex) {
        return rows.stream().map(row -> StringUtils.trimToEmpty(row.get(columnIndex))).toList();
    }

    private static List<String> orderedValues(Map<Integer, String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        int maxIndex = Collections.max(values.keySet());
        List<String> result = new ArrayList<>(maxIndex + 1);
        for (int i = 0; i <= maxIndex; i++) {
            result.add(StringUtils.trimToEmpty(values.get(i)));
        }
        return result;
    }

    private static void validateHeaders(List<String> headers, String sheetName) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = StringUtils.trimToEmpty(headers.get(i));
            if (StringUtils.isBlank(header)) {
                throw new ServiceException("工作表 " + sheetName + " 第 " + (i + 1) + " 列表头为空");
            }
            if (!names.add(header)) {
                throw new ServiceException("工作表 " + sheetName + " 存在重复表头: " + header);
            }
        }
    }

    private static int lastNonBlankIndex(List<String> values) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (StringUtils.isNotBlank(values.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isBlankRow(Map<Integer, String> values, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            if (StringUtils.isNotBlank(values.get(index))) {
                return false;
            }
        }
        return true;
    }

    public static String normalizeIdentifier(String raw, int fallbackIndex, String fallbackPrefix) {
        String normalized = Normalizer.normalize(StringUtils.trimToEmpty(raw), Normalizer.Form.NFKD)
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        normalized = IDENTIFIER_PATTERN.matcher(normalized).replaceAll("_")
            .replaceAll("_+", "_")
            .replaceAll("^_+|_+$", "")
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || Character.isDigit(normalized.charAt(0))) {
            normalized = fallbackPrefix + "_" + fallbackIndex;
        }
        if (RESERVED_COLUMNS.contains(normalized)) {
            normalized = fallbackPrefix + "_" + normalized;
        }
        return normalized.length() > 48 ? normalized.substring(0, 48).replaceAll("_+$", "") : normalized;
    }

    private static String uniqueIdentifier(String base, Set<String> used) {
        String candidate = base;
        int suffix = 2;
        while (!used.add(candidate)) {
            String suffixValue = "_" + suffix++;
            int limit = Math.min(base.length(), 48 - suffixValue.length());
            candidate = base.substring(0, limit) + suffixValue;
        }
        return candidate;
    }

    static boolean isBoolean(String value) {
        String normalized = StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT);
        return Set.of("true", "false", "yes", "no", "是", "否").contains(normalized);
    }

    static boolean isNumber(String value) {
        try {
            new BigDecimal(StringUtils.trimToEmpty(value).replace(",", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static boolean isDate(String value) {
        String normalized = StringUtils.trimToEmpty(value);
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                LocalDate.parse(normalized, formatter);
                return true;
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        return false;
    }

    private static int normalizeMaxLength(int observed) {
        if (observed <= 100) {
            return 100;
        }
        if (observed <= 255) {
            return 255;
        }
        if (observed <= 500) {
            return 500;
        }
        if (observed <= 1000) {
            return 1000;
        }
        return 2000;
    }

    private record RawRow(Integer sheetNo, String sheetName, int rowIndex, Map<Integer, String> values) {
    }

    @RequiredArgsConstructor
    private static final class RawRowListener extends AnalysisEventListener<Map<Integer, String>> {
        private final List<RawRow> rows;

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            Map<Integer, String> values = data == null ? Collections.emptyMap() : new LinkedHashMap<>(data);
            rows.add(new RawRow(
                context.readSheetHolder().getSheetNo(),
                context.readSheetHolder().getSheetName(),
                context.readRowHolder().getRowIndex(),
                values
            ));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
        }
    }
}
