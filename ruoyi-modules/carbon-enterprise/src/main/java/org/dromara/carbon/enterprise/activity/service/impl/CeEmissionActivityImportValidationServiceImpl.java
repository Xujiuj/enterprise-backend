package org.dromara.carbon.enterprise.activity.service.impl;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationResult;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityDerivedFieldResolver;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityImportValidationService;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityValidationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parses and validates Excel rows for the semantic emission_activity entry contract.
 */
@Service
public class CeEmissionActivityImportValidationServiceImpl implements ICeEmissionActivityImportValidationService {

    private static final String SEVERITY_ERROR = "ERROR";
    private static final String DERIVED_RESOLVER_UNAVAILABLE = "DERIVED_RESOLVER_UNAVAILABLE";
    private static final String XLSX_SUFFIX = ".xlsx";

    private final ICeEmissionActivityValidationService rowValidator;
    private final List<CeEmissionActivityFieldDescriptor> allFields;
    private final List<CeEmissionActivityFieldDescriptor> entryFields;
    private final boolean derivedFieldResolverConfigured;

    @Autowired
    public CeEmissionActivityImportValidationServiceImpl(ObjectProvider<ICeEmissionActivityDerivedFieldResolver> resolverProvider) {
        this(resolverProvider.getIfAvailable());
    }

    private CeEmissionActivityImportValidationServiceImpl(ICeEmissionActivityDerivedFieldResolver derivedFieldResolver) {
        this(
            derivedFieldResolver == null ? null : new CeEmissionActivityValidationServiceImpl(derivedFieldResolver),
            CeEmissionActivityValidationServiceImpl.allFieldDescriptors(),
            CeEmissionActivityValidationServiceImpl.entryFieldDescriptors(),
            derivedFieldResolver != null
        );
    }

    public CeEmissionActivityImportValidationServiceImpl(ICeEmissionActivityValidationService rowValidator) {
        this(rowValidator, CeEmissionActivityValidationServiceImpl.allFieldDescriptors(), rowValidator.listEntryFields(), true);
    }

    private CeEmissionActivityImportValidationServiceImpl(ICeEmissionActivityValidationService rowValidator,
                                                          List<CeEmissionActivityFieldDescriptor> allFields,
                                                          List<CeEmissionActivityFieldDescriptor> entryFields,
                                                          boolean derivedFieldResolverConfigured) {
        this.rowValidator = rowValidator;
        this.allFields = List.copyOf(allFields);
        this.entryFields = List.copyOf(entryFields);
        this.derivedFieldResolverConfigured = derivedFieldResolverConfigured;
    }

    @Override
    public CeEmissionActivityImportValidationRequest parseImportFile(MultipartFile file) {
        validateUploadFile(file);

        try (InputStream inputStream = file.getInputStream()) {
            List<ParsedSheetRow> sheetRows = readSheetRows(inputStream);
            return toImportValidationRequest(sheetRows);
        } catch (IOException e) {
            throw new ServiceException("读取 emission_activity Excel 文件失败");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("解析 emission_activity Excel 文件失败");
        }
    }

    @Override
    public CeEmissionActivityImportValidationResult validateImport(CeEmissionActivityImportValidationRequest request) {
        List<CeEmissionActivityValidationIssue> headerIssues = validateHeaderFields(request == null ? null : request.getHeaderFields());
        boolean headerValid = headerIssues.isEmpty();
        List<CeEmissionActivityValidationRequest> rows = request == null ? null : request.getRows();
        boolean resolverUnavailable = headerValid && !derivedFieldResolverConfigured && hasRows(rows);

        List<CeEmissionActivityValidationResult> rowResults;
        if (!headerValid) {
            rowResults = Collections.emptyList();
        } else if (resolverUnavailable) {
            rowResults = resolverUnavailableResults(rows);
        } else {
            rowResults = validateRows(rows);
        }

        CeEmissionActivityImportValidationResult result = new CeEmissionActivityImportValidationResult();
        result.setHeaderValid(headerValid);
        result.setValid(headerValid && !resolverUnavailable && rowResults.stream().allMatch(CeEmissionActivityValidationResult::isValid));
        result.setBlocking(!headerValid || resolverUnavailable || rowResults.stream().anyMatch(CeEmissionActivityValidationResult::isBlocking));
        result.setHeaderIssues(headerIssues);
        result.setRowResults(rowResults);
        return result;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传非空的 emission_activity Excel 文件");
        }
        String fileName = normalize(file.getOriginalFilename());
        if (StringUtils.isBlank(fileName) || !fileName.toLowerCase().endsWith(XLSX_SUFFIX)) {
            throw new ServiceException("emission_activity 仅支持上传 .xlsx 文件");
        }
    }

    private List<ParsedSheetRow> readSheetRows(InputStream inputStream) {
        List<ParsedSheetRow> rows = new ArrayList<>();
        FastExcel.read(inputStream, new EmissionActivityRowListener(rows))
            .autoCloseStream(false)
            .headRowNumber(0)
            .doReadAll();
        return rows;
    }

    private CeEmissionActivityImportValidationRequest toImportValidationRequest(List<ParsedSheetRow> sheetRows) {
        if (sheetRows == null || sheetRows.isEmpty()) {
            throw new ServiceException("emission_activity Excel 至少需要一行表头");
        }

        List<CeEmissionActivityValidationRequest> rows = new ArrayList<>();
        Map<String, HeaderBinding> bindingsByCode = Collections.emptyMap();
        Integer currentSheetNo = null;
        int importRowNumber = 2;
        for (ParsedSheetRow sheetRow : sheetRows) {
            if (sheetRow.rowIndex() == 0 || !Objects.equals(currentSheetNo, sheetRow.sheetNo())) {
                bindingsByCode = resolveHeaderBindings(sheetRow.values());
                currentSheetNo = sheetRow.sheetNo();
                continue;
            }
            CeEmissionActivityValidationRequest row = toValidationRow(sheetRow, bindingsByCode, importRowNumber);
            if (row != null) {
                rows.add(row);
                importRowNumber++;
            }
        }

        CeEmissionActivityImportValidationRequest request = new CeEmissionActivityImportValidationRequest();
        request.setHeaderFields(copyExpectedHeaderFields());
        request.setRows(rows);
        return request;
    }

    private Map<String, HeaderBinding> resolveHeaderBindings(Map<Integer, String> headerRow) {
        Map<String, CeEmissionActivityFieldDescriptor> expectedByCode = new LinkedHashMap<>();
        Map<String, CeEmissionActivityFieldDescriptor> expectedByName = new LinkedHashMap<>();
        for (CeEmissionActivityFieldDescriptor descriptor : entryFields) {
            expectedByCode.put(normalizeHeaderCode(descriptor.getFieldCode()), descriptor);
            expectedByName.put(normalize(descriptor.getFieldName()), descriptor);
        }

        Map<String, HeaderBinding> bindingsByCode = new LinkedHashMap<>();
        if (headerRow != null) {
            for (Map.Entry<Integer, String> entry : headerRow.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                String rawHeader = normalize(entry.getValue());
                if (StringUtils.isBlank(rawHeader)) {
                    continue;
                }

                CeEmissionActivityFieldDescriptor descriptor = expectedByCode.get(normalizeHeaderCode(rawHeader));
                if (descriptor == null) {
                    descriptor = expectedByName.get(rawHeader);
                }
                if (descriptor == null) {
                    continue;
                }
                if (bindingsByCode.containsKey(descriptor.getFieldCode())) {
                    throw new ServiceException("emission_activity Excel 表头重复: {}", descriptor.getFieldName());
                }
                bindingsByCode.put(descriptor.getFieldCode(), new HeaderBinding(copyHeaderField(descriptor), entry.getKey()));
            }
        }

        List<String> missingHeaders = entryFields.stream()
            .filter(descriptor -> !bindingsByCode.containsKey(descriptor.getFieldCode()))
            .map(CeEmissionActivityFieldDescriptor::getFieldName)
            .toList();
        if (!missingHeaders.isEmpty()) {
            throw new ServiceException("emission_activity Excel 缺少必要表头: {}", String.join(", ", missingHeaders));
        }
        return bindingsByCode;
    }

    private CeEmissionActivityValidationRequest toValidationRow(ParsedSheetRow sheetRow, Map<String, HeaderBinding> bindingsByCode,
                                                               int importRowNumber) {
        List<CeEmissionActivityFieldValue> fieldValues = new ArrayList<>(allFields.size());
        boolean blankRow = true;
        Map<Integer, String> rowValues = sheetRow == null ? Collections.emptyMap() : sheetRow.values();
        Map<String, String> valuesByCode = normalizeEntryValues(rowValues, bindingsByCode);

        for (CeEmissionActivityFieldDescriptor descriptor : allFields) {
            String value = normalize(valuesByCode.get(descriptor.getFieldCode()));
            if (value == null) {
                value = "";
            }
            if (StringUtils.isNotBlank(value)) {
                blankRow = false;
            }
            fieldValues.add(fieldValue(descriptor, value));
        }

        if (blankRow) {
            return null;
        }

        CeEmissionActivityValidationRequest request = new CeEmissionActivityValidationRequest();
        request.setRowNumber(importRowNumber);
        request.setFieldValues(fieldValues);
        return request;
    }

    private Map<String, String> normalizeEntryValues(Map<Integer, String> rowValues, Map<String, HeaderBinding> bindingsByCode) {
        Map<String, String> valuesByCode = new LinkedHashMap<>();
        for (HeaderBinding binding : bindingsByCode.values()) {
            String fieldCode = binding.descriptor().getFieldCode();
            String value = normalize(rowValues.get(binding.columnIndex()));
            if ("activityPeriod".equals(fieldCode)) {
                String[] period = splitActivityPeriod(value);
                valuesByCode.put("activityYear", period[0]);
                valuesByCode.put("activityMonth", period[1]);
                continue;
            }
            valuesByCode.put(fieldCode, value);
        }
        return valuesByCode;
    }

    private String[] splitActivityPeriod(String value) {
        String text = normalize(value);
        if (StringUtils.isBlank(text)) {
            return new String[] { "", "" };
        }
        String normalized = text
            .replace('\u5e74', '-')
            .replace('\u6708', ' ')
            .replace('/', '-')
            .replace('.', '-')
            .trim();
        String[] parts = normalized.split("-", 2);
        if (parts.length < 2) {
            return new String[] { normalized, "" };
        }
        String month = parts[1].trim().replaceAll("[^0-9].*$", "");
        if (month.length() == 2 && month.startsWith("0")) {
            month = month.substring(1);
        }
        return new String[] { parts[0].trim(), month };
    }

    private List<CeEmissionActivityFieldDescriptor> copyExpectedHeaderFields() {
        return entryFields.stream()
            .map(this::copyHeaderField)
            .toList();
    }

    private CeEmissionActivityFieldDescriptor copyHeaderField(CeEmissionActivityFieldDescriptor descriptor) {
        CeEmissionActivityFieldDescriptor copy = new CeEmissionActivityFieldDescriptor();
        copy.setFieldOrder(descriptor.getFieldOrder());
        copy.setFieldCode(descriptor.getFieldCode());
        copy.setFieldName(descriptor.getFieldName());
        copy.setSourceRequired(descriptor.isSourceRequired());
        copy.setRowValueRequired(descriptor.isRowValueRequired());
        copy.setDerivedField(descriptor.isDerivedField());
        return copy;
    }

    private CeEmissionActivityFieldValue fieldValue(CeEmissionActivityFieldDescriptor descriptor, String value) {
        CeEmissionActivityFieldValue fieldValue = new CeEmissionActivityFieldValue();
        fieldValue.setFieldCode(descriptor.getFieldCode());
        fieldValue.setFieldName(descriptor.getFieldName());
        fieldValue.setValue(value);
        return fieldValue;
    }

    private String normalizeHeaderCode(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private List<CeEmissionActivityValidationIssue> validateHeaderFields(List<CeEmissionActivityFieldDescriptor> actualHeaderFields) {
        if (actualHeaderFields == null) {
            return List.of(headerIssue("HEADER_REQUIRED", null, null,
                "headerFields must be provided for emission_activity import validation"));
        }

        List<CeEmissionActivityValidationIssue> issues = new ArrayList<>();
        int maxSize = Math.max(entryFields.size(), actualHeaderFields.size());
        for (int index = 0; index < maxSize; index++) {
            CeEmissionActivityFieldDescriptor expected = index < entryFields.size() ? entryFields.get(index) : null;
            boolean actualPresent = index < actualHeaderFields.size();
            CeEmissionActivityFieldDescriptor actual = actualPresent ? actualHeaderFields.get(index) : null;

            if (actualPresent && actual == null) {
                issues.add(headerIssue("INVALID_HEADER_COLUMN",
                    expected == null ? null : expected.getFieldCode(),
                    expected == null ? null : expected.getFieldName(),
                    "header column at position " + (index + 1) + " must not be null"));
                continue;
            }

            if (expected == null && actual != null) {
                issues.add(headerIssue("UNEXPECTED_HEADER_COLUMN", actual.getFieldCode(), actual.getFieldName(),
                    "unexpected header column at position " + (index + 1)));
                continue;
            }
            if (expected != null && !actualPresent) {
                issues.add(headerIssue("MISSING_HEADER_COLUMN", expected.getFieldCode(), expected.getFieldName(),
                    "missing required header column at position " + (index + 1)));
                continue;
            }
            if (!sameHeader(expected, actual)) {
                issues.add(headerIssue("HEADER_COLUMN_MISMATCH", normalize(actual.getFieldCode()),
                    normalize(actual.getFieldName()),
                    "header column at position " + (index + 1) + " must be "
                        + expected.getFieldCode() + "/" + expected.getFieldName()));
            }
        }
        return issues;
    }

    private boolean hasRows(List<CeEmissionActivityValidationRequest> rows) {
        return rows != null && !rows.isEmpty();
    }

    private List<CeEmissionActivityValidationResult> validateRows(List<CeEmissionActivityValidationRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CeEmissionActivityValidationResult> results = new ArrayList<>(rows.size());
        Set<Integer> seenRowNumbers = new HashSet<>();
        for (CeEmissionActivityValidationRequest row : rows) {
            Integer rowNumber = row == null ? null : row.getRowNumber();
            if (rowNumber == null) {
                results.add(invalidRowNumberResult(rowNumber, "ROW_NUMBER_MISSING",
                    "rowNumber must be provided for each import row"));
                continue;
            }
            if (!seenRowNumbers.add(rowNumber)) {
                results.add(invalidRowNumberResult(rowNumber, "DUPLICATE_ROW_NUMBER",
                    "rowNumber must be unique within a emission_activity import request"));
                continue;
            }
            results.add(rowValidator.validate(row));
        }
        return results;
    }

    private CeEmissionActivityValidationResult invalidRowNumberResult(Integer rowNumber, String code, String message) {
        CeEmissionActivityValidationIssue issue = headerIssue(code, "rowNumber", "rowNumber", message);
        issue.setRowNumber(rowNumber);

        CeEmissionActivityValidationResult result = new CeEmissionActivityValidationResult();
        result.setRowNumber(rowNumber);
        result.setValid(false);
        result.setBlocking(true);
        result.setDraftSavable(false);
        result.setResolvedDerivedFieldValues(Collections.emptyList());
        result.setIssues(List.of(issue));
        return result;
    }

    private List<CeEmissionActivityValidationResult> resolverUnavailableResults(List<CeEmissionActivityValidationRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CeEmissionActivityValidationResult> results = new ArrayList<>(rows.size());
        for (CeEmissionActivityValidationRequest row : rows) {
            CeEmissionActivityValidationResult result = new CeEmissionActivityValidationResult();
            result.setRowNumber(row == null ? null : row.getRowNumber());
            result.setValid(false);
            result.setBlocking(true);
            result.setDraftSavable(false);
            result.setResolvedDerivedFieldValues(Collections.emptyList());
            result.setIssues(List.of(rowIssue(
                DERIVED_RESOLVER_UNAVAILABLE,
                row == null ? null : row.getRowNumber(),
                "sourceIdentificationCode",
                "排放源识别编号",
                "enterprise-local derived field resolver is not configured"
            )));
            results.add(result);
        }
        return results;
    }

    private boolean sameHeader(CeEmissionActivityFieldDescriptor expected, CeEmissionActivityFieldDescriptor actual) {
        return StringUtils.equals(normalize(expected.getFieldCode()), normalize(actual.getFieldCode()))
            && StringUtils.equals(normalize(expected.getFieldName()), normalize(actual.getFieldName()));
    }

    private CeEmissionActivityFieldDescriptor expectedHeaderField(String fieldCode) {
        return allFields.stream()
            .filter(field -> StringUtils.equals(fieldCode, field.getFieldCode()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("missing frozen header field: " + fieldCode));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private CeEmissionActivityValidationIssue headerIssue(String code, String fieldCode, String fieldName, String message) {
        CeEmissionActivityValidationIssue issue = new CeEmissionActivityValidationIssue();
        issue.setSeverity(SEVERITY_ERROR);
        issue.setCode(code);
        issue.setFieldCode(fieldCode);
        issue.setFieldName(fieldName);
        issue.setMessage(message);
        return issue;
    }

    private CeEmissionActivityValidationIssue rowIssue(String code, Integer rowNumber, String fieldCode,
                                                       String fieldName, String message) {
        if (StringUtils.equals("sourceIdentificationCode", fieldCode)) {
            fieldName = expectedHeaderField(fieldCode).getFieldName();
        }
        CeEmissionActivityValidationIssue issue = headerIssue(code, fieldCode, fieldName, message);
        issue.setRowNumber(rowNumber);
        return issue;
    }

    private record HeaderBinding(CeEmissionActivityFieldDescriptor descriptor, Integer columnIndex) {
    }

    private record ParsedSheetRow(Integer sheetNo, int rowIndex, Map<Integer, String> values) {
    }

    private static final class EmissionActivityRowListener extends AnalysisEventListener<Map<Integer, String>> {

        private final List<ParsedSheetRow> rows;

        private EmissionActivityRowListener(List<ParsedSheetRow> rows) {
            this.rows = rows;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            Map<Integer, String> values = data == null ? Collections.emptyMap() : new LinkedHashMap<>(data);
            rows.add(new ParsedSheetRow(context.readSheetHolder().getSheetNo(), context.readRowHolder().getRowIndex(), values));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
        }
    }
}
