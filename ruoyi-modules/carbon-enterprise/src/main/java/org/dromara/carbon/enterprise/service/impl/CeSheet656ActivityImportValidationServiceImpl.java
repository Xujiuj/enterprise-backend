package org.dromara.carbon.enterprise.service.impl;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationResult;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityImportValidationService;
import org.dromara.carbon.enterprise.service.ICeSheet656DerivedFieldResolver;
import org.dromara.carbon.enterprise.service.ICeSheet656ValidationService;
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
import java.util.Set;

/**
 * Thin validate-only import service for the frozen sheet_656 shape.
 */
@Service
public class CeSheet656ActivityImportValidationServiceImpl implements ICeSheet656ActivityImportValidationService {

    private static final String SEVERITY_ERROR = "ERROR";
    private static final String DERIVED_RESOLVER_UNAVAILABLE = "DERIVED_RESOLVER_UNAVAILABLE";
    private static final String XLSX_SUFFIX = ".xlsx";

    private final ICeSheet656ValidationService rowValidator;
    private final List<CeSheet656FieldDescriptor> expectedHeaderFields;
    private final boolean derivedFieldResolverConfigured;

    @Autowired
    public CeSheet656ActivityImportValidationServiceImpl(ObjectProvider<ICeSheet656DerivedFieldResolver> resolverProvider) {
        this(resolverProvider.getIfAvailable());
    }

    private CeSheet656ActivityImportValidationServiceImpl(ICeSheet656DerivedFieldResolver derivedFieldResolver) {
        this(
            derivedFieldResolver == null ? null : new CeSheet656ValidationServiceImpl(derivedFieldResolver),
            CeSheet656ValidationServiceImpl.frozenFieldDescriptors(),
            derivedFieldResolver != null
        );
    }

    public CeSheet656ActivityImportValidationServiceImpl(ICeSheet656ValidationService rowValidator) {
        this(rowValidator, rowValidator.listFrozenFields(), true);
    }

    private CeSheet656ActivityImportValidationServiceImpl(ICeSheet656ValidationService rowValidator,
                                                          List<CeSheet656FieldDescriptor> expectedHeaderFields,
                                                          boolean derivedFieldResolverConfigured) {
        this.rowValidator = rowValidator;
        this.expectedHeaderFields = List.copyOf(expectedHeaderFields);
        this.derivedFieldResolverConfigured = derivedFieldResolverConfigured;
    }

    @Override
    public CeSheet656ImportValidationRequest parseImportFile(MultipartFile file) {
        validateUploadFile(file);

        try (InputStream inputStream = file.getInputStream()) {
            List<ParsedSheetRow> sheetRows = readSheetRows(inputStream);
            return toImportValidationRequest(sheetRows);
        } catch (IOException e) {
            throw new ServiceException("读取 sheet_656 Excel 文件失败");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("解析 sheet_656 Excel 文件失败");
        }
    }

    @Override
    public CeSheet656ImportValidationResult validateImport(CeSheet656ImportValidationRequest request) {
        List<CeSheet656ValidationIssue> headerIssues = validateHeaderFields(request == null ? null : request.getHeaderFields());
        boolean headerValid = headerIssues.isEmpty();
        List<CeSheet656ValidationRequest> rows = request == null ? null : request.getRows();
        boolean resolverUnavailable = headerValid && !derivedFieldResolverConfigured && hasRows(rows);

        List<CeSheet656ValidationResult> rowResults;
        if (!headerValid) {
            rowResults = Collections.emptyList();
        } else if (resolverUnavailable) {
            rowResults = resolverUnavailableResults(rows);
        } else {
            rowResults = validateRows(rows);
        }

        CeSheet656ImportValidationResult result = new CeSheet656ImportValidationResult();
        result.setHeaderValid(headerValid);
        result.setValid(headerValid && !resolverUnavailable && rowResults.stream().allMatch(CeSheet656ValidationResult::isValid));
        result.setBlocking(!headerValid || resolverUnavailable || rowResults.stream().anyMatch(CeSheet656ValidationResult::isBlocking));
        result.setHeaderIssues(headerIssues);
        result.setRowResults(rowResults);
        return result;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传非空的 sheet_656 Excel 文件");
        }
        String fileName = normalize(file.getOriginalFilename());
        if (StringUtils.isBlank(fileName) || !fileName.toLowerCase().endsWith(XLSX_SUFFIX)) {
            throw new ServiceException("sheet_656 仅支持上传 .xlsx 文件");
        }
    }

    private List<ParsedSheetRow> readSheetRows(InputStream inputStream) {
        List<ParsedSheetRow> rows = new ArrayList<>();
        FastExcel.read(inputStream, new Sheet656RowListener(rows))
            .autoCloseStream(false)
            .sheet(0)
            .headRowNumber(0)
            .doRead();
        return rows;
    }

    private CeSheet656ImportValidationRequest toImportValidationRequest(List<ParsedSheetRow> sheetRows) {
        if (sheetRows == null || sheetRows.isEmpty()) {
            throw new ServiceException("sheet_656 Excel 至少需要一行表头");
        }

        Map<String, HeaderBinding> bindingsByCode = resolveHeaderBindings(sheetRows.get(0).values());
        List<CeSheet656ValidationRequest> rows = new ArrayList<>();
        for (int index = 1; index < sheetRows.size(); index++) {
            CeSheet656ValidationRequest row = toValidationRow(sheetRows.get(index), bindingsByCode);
            if (row != null) {
                rows.add(row);
            }
        }

        CeSheet656ImportValidationRequest request = new CeSheet656ImportValidationRequest();
        request.setHeaderFields(copyExpectedHeaderFields());
        request.setRows(rows);
        return request;
    }

    private Map<String, HeaderBinding> resolveHeaderBindings(Map<Integer, String> headerRow) {
        Map<String, CeSheet656FieldDescriptor> expectedByCode = new LinkedHashMap<>();
        Map<String, CeSheet656FieldDescriptor> expectedByName = new LinkedHashMap<>();
        for (CeSheet656FieldDescriptor descriptor : expectedHeaderFields) {
            expectedByCode.put(normalizeHeaderCode(descriptor.getSourceColumnCode()), descriptor);
            expectedByName.put(normalize(descriptor.getSourceColumnName()), descriptor);
        }

        Map<String, HeaderBinding> bindingsByCode = new LinkedHashMap<>();
        if (headerRow != null) {
            for (Map.Entry<Integer, String> entry : headerRow.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                String rawHeader = normalize(entry.getValue());
                if (StringUtils.isBlank(rawHeader)) {
                    continue;
                }

                CeSheet656FieldDescriptor descriptor = expectedByCode.get(normalizeHeaderCode(rawHeader));
                if (descriptor == null) {
                    descriptor = expectedByName.get(rawHeader);
                }
                if (descriptor == null) {
                    continue;
                }
                if (bindingsByCode.containsKey(descriptor.getSourceColumnCode())) {
                    throw new ServiceException("sheet_656 Excel 表头重复: {}", descriptor.getSourceColumnName());
                }
                bindingsByCode.put(descriptor.getSourceColumnCode(), new HeaderBinding(copyHeaderField(descriptor), entry.getKey()));
            }
        }

        List<String> missingHeaders = expectedHeaderFields.stream()
            .filter(descriptor -> !bindingsByCode.containsKey(descriptor.getSourceColumnCode()))
            .map(CeSheet656FieldDescriptor::getSourceColumnName)
            .toList();
        if (!missingHeaders.isEmpty()) {
            throw new ServiceException("sheet_656 Excel 缺少必要表头: {}", String.join(", ", missingHeaders));
        }
        return bindingsByCode;
    }

    private CeSheet656ValidationRequest toValidationRow(ParsedSheetRow sheetRow, Map<String, HeaderBinding> bindingsByCode) {
        List<CeSheet656FieldValue> fieldValues = new ArrayList<>(expectedHeaderFields.size());
        boolean blankRow = true;
        Map<Integer, String> rowValues = sheetRow == null ? Collections.emptyMap() : sheetRow.values();

        for (CeSheet656FieldDescriptor descriptor : expectedHeaderFields) {
            HeaderBinding binding = bindingsByCode.get(descriptor.getSourceColumnCode());
            String value = binding == null ? null : normalize(rowValues.get(binding.columnIndex()));
            if (StringUtils.isNotBlank(value)) {
                blankRow = false;
            }
            fieldValues.add(fieldValue(descriptor, value));
        }

        if (blankRow) {
            return null;
        }

        CeSheet656ValidationRequest request = new CeSheet656ValidationRequest();
        request.setRowNumber(sheetRow.rowIndex() + 1);
        request.setFieldValues(fieldValues);
        return request;
    }

    private List<CeSheet656FieldDescriptor> copyExpectedHeaderFields() {
        return expectedHeaderFields.stream()
            .map(this::copyHeaderField)
            .toList();
    }

    private CeSheet656FieldDescriptor copyHeaderField(CeSheet656FieldDescriptor descriptor) {
        CeSheet656FieldDescriptor copy = new CeSheet656FieldDescriptor();
        copy.setFieldOrder(descriptor.getFieldOrder());
        copy.setSourceColumnCode(descriptor.getSourceColumnCode());
        copy.setSourceColumnName(descriptor.getSourceColumnName());
        copy.setSourceRequired(descriptor.isSourceRequired());
        copy.setRowValueRequired(descriptor.isRowValueRequired());
        copy.setDerivedField(descriptor.isDerivedField());
        return copy;
    }

    private CeSheet656FieldValue fieldValue(CeSheet656FieldDescriptor descriptor, String value) {
        CeSheet656FieldValue fieldValue = new CeSheet656FieldValue();
        fieldValue.setSourceColumnCode(descriptor.getSourceColumnCode());
        fieldValue.setSourceColumnName(descriptor.getSourceColumnName());
        fieldValue.setValue(value);
        return fieldValue;
    }

    private String normalizeHeaderCode(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private List<CeSheet656ValidationIssue> validateHeaderFields(List<CeSheet656FieldDescriptor> actualHeaderFields) {
        if (actualHeaderFields == null) {
            return List.of(headerIssue("HEADER_REQUIRED", null, null,
                "headerFields must be provided for sheet_656 import validation"));
        }

        List<CeSheet656ValidationIssue> issues = new ArrayList<>();
        int maxSize = Math.max(expectedHeaderFields.size(), actualHeaderFields.size());
        for (int index = 0; index < maxSize; index++) {
            CeSheet656FieldDescriptor expected = index < expectedHeaderFields.size() ? expectedHeaderFields.get(index) : null;
            boolean actualPresent = index < actualHeaderFields.size();
            CeSheet656FieldDescriptor actual = actualPresent ? actualHeaderFields.get(index) : null;

            if (actualPresent && actual == null) {
                issues.add(headerIssue("INVALID_HEADER_COLUMN",
                    expected == null ? null : expected.getSourceColumnCode(),
                    expected == null ? null : expected.getSourceColumnName(),
                    "header column at position " + (index + 1) + " must not be null"));
                continue;
            }

            if (expected == null && actual != null) {
                issues.add(headerIssue("UNEXPECTED_HEADER_COLUMN", actual.getSourceColumnCode(), actual.getSourceColumnName(),
                    "unexpected header column at position " + (index + 1)));
                continue;
            }
            if (expected != null && !actualPresent) {
                issues.add(headerIssue("MISSING_HEADER_COLUMN", expected.getSourceColumnCode(), expected.getSourceColumnName(),
                    "missing required header column at position " + (index + 1)));
                continue;
            }
            if (!sameHeader(expected, actual)) {
                issues.add(headerIssue("HEADER_COLUMN_MISMATCH", normalize(actual.getSourceColumnCode()),
                    normalize(actual.getSourceColumnName()),
                    "header column at position " + (index + 1) + " must be "
                        + expected.getSourceColumnCode() + "/" + expected.getSourceColumnName()));
            }
        }
        return issues;
    }

    private boolean hasRows(List<CeSheet656ValidationRequest> rows) {
        return rows != null && !rows.isEmpty();
    }

    private List<CeSheet656ValidationResult> validateRows(List<CeSheet656ValidationRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CeSheet656ValidationResult> results = new ArrayList<>(rows.size());
        Set<Integer> seenRowNumbers = new HashSet<>();
        for (CeSheet656ValidationRequest row : rows) {
            Integer rowNumber = row == null ? null : row.getRowNumber();
            if (rowNumber == null) {
                results.add(invalidRowNumberResult(rowNumber, "ROW_NUMBER_MISSING",
                    "rowNumber must be provided for each import row"));
                continue;
            }
            if (!seenRowNumbers.add(rowNumber)) {
                results.add(invalidRowNumberResult(rowNumber, "DUPLICATE_ROW_NUMBER",
                    "rowNumber must be unique within a sheet_656 import request"));
                continue;
            }
            results.add(rowValidator.validate(row));
        }
        return results;
    }

    private CeSheet656ValidationResult invalidRowNumberResult(Integer rowNumber, String code, String message) {
        CeSheet656ValidationIssue issue = headerIssue(code, "rowNumber", "rowNumber", message);
        issue.setRowNumber(rowNumber);

        CeSheet656ValidationResult result = new CeSheet656ValidationResult();
        result.setRowNumber(rowNumber);
        result.setValid(false);
        result.setBlocking(true);
        result.setDraftSavable(false);
        result.setResolvedDerivedFieldValues(Collections.emptyList());
        result.setIssues(List.of(issue));
        return result;
    }

    private List<CeSheet656ValidationResult> resolverUnavailableResults(List<CeSheet656ValidationRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CeSheet656ValidationResult> results = new ArrayList<>(rows.size());
        for (CeSheet656ValidationRequest row : rows) {
            CeSheet656ValidationResult result = new CeSheet656ValidationResult();
            result.setRowNumber(row == null ? null : row.getRowNumber());
            result.setValid(false);
            result.setBlocking(true);
            result.setDraftSavable(false);
            result.setResolvedDerivedFieldValues(Collections.emptyList());
            result.setIssues(List.of(rowIssue(
                DERIVED_RESOLVER_UNAVAILABLE,
                row == null ? null : row.getRowNumber(),
                "f001",
                "PK_鎺掓斁婧愯瘑鍒紪鍙?",
                "enterprise-local derived field resolver is not configured"
            )));
            results.add(result);
        }
        return results;
    }

    private boolean sameHeader(CeSheet656FieldDescriptor expected, CeSheet656FieldDescriptor actual) {
        return StringUtils.equals(normalize(expected.getSourceColumnCode()), normalize(actual.getSourceColumnCode()))
            && StringUtils.equals(normalize(expected.getSourceColumnName()), normalize(actual.getSourceColumnName()));
    }

    private CeSheet656FieldDescriptor expectedHeaderField(String sourceColumnCode) {
        return expectedHeaderFields.stream()
            .filter(field -> StringUtils.equals(sourceColumnCode, field.getSourceColumnCode()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("missing frozen header field: " + sourceColumnCode));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private CeSheet656ValidationIssue headerIssue(String code, String sourceColumnCode, String sourceColumnName, String message) {
        CeSheet656ValidationIssue issue = new CeSheet656ValidationIssue();
        issue.setSeverity(SEVERITY_ERROR);
        issue.setCode(code);
        issue.setSourceColumnCode(sourceColumnCode);
        issue.setSourceColumnName(sourceColumnName);
        issue.setMessage(message);
        return issue;
    }

    private CeSheet656ValidationIssue rowIssue(String code, Integer rowNumber, String sourceColumnCode,
                                               String sourceColumnName, String message) {
        if (StringUtils.equals("f001", sourceColumnCode)) {
            sourceColumnName = expectedHeaderField(sourceColumnCode).getSourceColumnName();
        }
        CeSheet656ValidationIssue issue = headerIssue(code, sourceColumnCode, sourceColumnName, message);
        issue.setRowNumber(rowNumber);
        return issue;
    }

    private record HeaderBinding(CeSheet656FieldDescriptor descriptor, Integer columnIndex) {
    }

    private record ParsedSheetRow(int rowIndex, Map<Integer, String> values) {
    }

    private static final class Sheet656RowListener extends AnalysisEventListener<Map<Integer, String>> {

        private final List<ParsedSheetRow> rows;

        private Sheet656RowListener(List<ParsedSheetRow> rows) {
            this.rows = rows;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            Map<Integer, String> values = data == null ? Collections.emptyMap() : new LinkedHashMap<>(data);
            rows.add(new ParsedSheetRow(context.readRowHolder().getRowIndex(), values));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
        }
    }
}
