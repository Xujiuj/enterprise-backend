package org.dromara.carbon.enterprise.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationResult;
import org.dromara.carbon.enterprise.service.ICeSheet656DerivedFieldResolver;
import org.dromara.carbon.enterprise.service.ICeSheet656ValidationService;
import org.dromara.common.core.utils.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Thin row-level validator for the frozen sheet_656 shape. EB-4 owns import header validation.
 */
@RequiredArgsConstructor
public class CeSheet656ValidationServiceImpl implements ICeSheet656ValidationService {

    private static final String SEVERITY_ERROR = "ERROR";
    private static final String SEVERITY_WARNING = "WARNING";
    private static final List<FieldDescriptor> FIELD_ORDER = List.of(
        new FieldDescriptor(1, "f001", "PK_排放源识别编号", false, true, false),
        new FieldDescriptor(2, "f002", "FK_公司编号", false, false, true),
        new FieldDescriptor(3, "f003", "公司名称", false, false, true),
        new FieldDescriptor(4, "f004", "工厂", false, false, true),
        new FieldDescriptor(5, "f005", "FK_排放源分类", false, false, true),
        new FieldDescriptor(6, "f006", "范围", false, false, true),
        new FieldDescriptor(7, "f007", "范围子类别", false, false, true),
        new FieldDescriptor(8, "f008", "排放源识别", false, false, true),
        new FieldDescriptor(9, "f009", "排放源", false, false, true),
        new FieldDescriptor(10, "f010", "单位", false, false, true),
        new FieldDescriptor(11, "f011", "年度", false, true, false),
        new FieldDescriptor(12, "f012", "月份", false, true, false),
        new FieldDescriptor(13, "f013", "日期", false, true, false),
        new FieldDescriptor(14, "f014", "活动数据", false, true, false),
        new FieldDescriptor(15, "f015", "负责部门", false, true, false),
        new FieldDescriptor(16, "f016", "数据来源", false, true, false),
        new FieldDescriptor(17, "f017", "备注", false, false, false),
        new FieldDescriptor(18, "f018", "FK_排放因子", false, false, true)
    );
    private static final Map<String, FieldDescriptor> FIELD_BY_CODE = buildFieldIndex();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private final ICeSheet656DerivedFieldResolver derivedFieldResolver;

    @Override
    public List<CeSheet656FieldDescriptor> listFrozenFields() {
        return FIELD_ORDER.stream()
            .map(this::toFieldDescriptor)
            .toList();
    }

    @Override
    public CeSheet656ValidationResult validate(CeSheet656ValidationRequest request) {
        Integer rowNumber = request == null ? null : request.getRowNumber();
        List<CeSheet656ValidationIssue> issues = new ArrayList<>();
        Map<String, String> clientValues = toClientValueMap(request);

        for (FieldDescriptor field : FIELD_ORDER) {
            String value = clientValues.get(field.sourceColumnCode());
            if (field.rowValueRequired() && StringUtils.isBlank(value)) {
                issues.add(issue(SEVERITY_ERROR, "REQUIRED_FIELD_MISSING", rowNumber, field,
                    "required field is missing"));
            }
        }

        validateYear(clientValues.get("f011"), rowNumber, issues);
        validateMonth(clientValues.get("f012"), rowNumber, issues);
        validateDate(clientValues.get("f013"), rowNumber, issues);
        validateActivityValue(clientValues.get("f014"), rowNumber, issues);

        List<CeSheet656FieldValue> resolvedDerivedFieldValues = new ArrayList<>();
        String emissionSourceCode = clientValues.get("f001");
        if (StringUtils.isNotBlank(emissionSourceCode)) {
            Optional<CeSheet656ResolvedRow> resolvedRow = derivedFieldResolver.resolve(emissionSourceCode);
            if (resolvedRow.isEmpty()) {
                issues.add(issue(SEVERITY_ERROR, "MASTER_DATA_NOT_FOUND", rowNumber, descriptor("f001"),
                    "no enterprise-local master data match exists for the emission source"));
            } else {
                validateDerivedFields(clientValues, resolvedRow.get(), rowNumber, issues, resolvedDerivedFieldValues);
            }
        }

        boolean blocking = issues.stream().anyMatch(issue -> SEVERITY_ERROR.equals(issue.getSeverity()));
        CeSheet656ValidationResult result = new CeSheet656ValidationResult();
        result.setRowNumber(rowNumber);
        result.setValid(!blocking);
        result.setBlocking(blocking);
        result.setDraftSavable(!blocking);
        result.setIssues(issues);
        result.setResolvedDerivedFieldValues(resolvedDerivedFieldValues);
        return result;
    }

    private void validateDerivedFields(Map<String, String> clientValues, CeSheet656ResolvedRow resolvedRow, Integer rowNumber,
                                       List<CeSheet656ValidationIssue> issues, List<CeSheet656FieldValue> resolvedFields) {
        Map<String, String> serverValues = new LinkedHashMap<>();
        serverValues.put("f002", resolvedRow.getCompanyCode());
        serverValues.put("f003", resolvedRow.getCompanyName());
        serverValues.put("f004", resolvedRow.getFactoryName());
        serverValues.put("f005", resolvedRow.getEmissionSourceCategoryCode());
        serverValues.put("f006", resolvedRow.getScope());
        serverValues.put("f007", resolvedRow.getScopeSubcategory());
        serverValues.put("f008", resolvedRow.getEmissionSourceIdentity());
        serverValues.put("f009", resolvedRow.getEmissionSourceName());
        serverValues.put("f010", resolvedRow.getUnit());
        serverValues.put("f018", resolvedRow.getEmissionFactorCode());

        for (Map.Entry<String, String> entry : serverValues.entrySet()) {
            FieldDescriptor descriptor = descriptor(entry.getKey());
            String serverValue = normalize(entry.getValue());
            String clientValue = normalize(clientValues.get(entry.getKey()));

            if (StringUtils.isBlank(serverValue)) {
                issues.add(issue(SEVERITY_ERROR, "MASTER_DATA_INCOMPLETE", rowNumber, descriptor,
                    "enterprise-local master data is missing a derived field value"));
                continue;
            }

            resolvedFields.add(fieldValue(descriptor, serverValue));

            if (StringUtils.isBlank(clientValue)) {
                issues.add(issue(SEVERITY_WARNING, "DERIVED_FIELD_SERVER_FILLED", rowNumber, descriptor,
                    "client value is ignored and will be filled from enterprise-local master data"));
                continue;
            }

            if (!serverValue.equals(clientValue)) {
                issues.add(issue(SEVERITY_ERROR, "DERIVED_FIELD_MISMATCH", rowNumber, descriptor,
                    "client value does not match enterprise-local derived value"));
            }
        }
    }

    private void validateYear(String value, Integer rowNumber, List<CeSheet656ValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            int year = Integer.parseInt(value);
            if (year < 1900 || year > 2200) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_VALUE_DOMAIN", rowNumber, descriptor("f011"),
                    "year must be between 1900 and 2200"));
            }
        } catch (NumberFormatException e) {
            issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("f011"),
                "year must be an integer"));
        }
    }

    private void validateMonth(String value, Integer rowNumber, List<CeSheet656ValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            int month = Integer.parseInt(value);
            if (month < 1 || month > 12) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_VALUE_DOMAIN", rowNumber, descriptor("f012"),
                    "month must be between 1 and 12"));
            }
        } catch (NumberFormatException e) {
            issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("f012"),
                "month must be an integer"));
        }
    }

    private void validateDate(String value, Integer rowNumber, List<CeSheet656ValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException firstFailure) {
            try {
                YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyy-MM", Locale.ROOT));
            } catch (DateTimeParseException ignored) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("f013"),
                    "date must be yyyy-MM-dd or yyyy-MM"));
            }
        }
    }

    private void validateActivityValue(String value, Integer rowNumber, List<CeSheet656ValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_VALUE_DOMAIN", rowNumber, descriptor("f014"),
                    "activity value must be greater than zero"));
            }
        } catch (NumberFormatException e) {
            issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("f014"),
                "activity value must be numeric"));
        }
    }

    private Map<String, String> toClientValueMap(CeSheet656ValidationRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        if (request == null || request.getFieldValues() == null) {
            return values;
        }
        for (CeSheet656FieldValue fieldValue : request.getFieldValues()) {
            if (fieldValue == null || StringUtils.isBlank(fieldValue.getSourceColumnCode())) {
                continue;
            }
            String code = fieldValue.getSourceColumnCode().trim();
            if (!FIELD_BY_CODE.containsKey(code)) {
                continue;
            }
            values.put(code, normalize(fieldValue.getValue()));
        }
        return values;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private CeSheet656FieldDescriptor toFieldDescriptor(FieldDescriptor field) {
        CeSheet656FieldDescriptor descriptor = new CeSheet656FieldDescriptor();
        descriptor.setFieldOrder(field.fieldOrder());
        descriptor.setSourceColumnCode(field.sourceColumnCode());
        descriptor.setSourceColumnName(field.sourceColumnName());
        descriptor.setSourceRequired(field.sourceRequired());
        descriptor.setRowValueRequired(field.rowValueRequired());
        descriptor.setDerivedField(field.derivedField());
        return descriptor;
    }

    private static Map<String, FieldDescriptor> buildFieldIndex() {
        Map<String, FieldDescriptor> fieldByCode = new LinkedHashMap<>();
        for (FieldDescriptor descriptor : FIELD_ORDER) {
            fieldByCode.put(descriptor.sourceColumnCode(), descriptor);
        }
        return fieldByCode;
    }

    private FieldDescriptor descriptor(String code) {
        return FIELD_BY_CODE.get(code);
    }

    private CeSheet656FieldValue fieldValue(FieldDescriptor descriptor, String value) {
        CeSheet656FieldValue fieldValue = new CeSheet656FieldValue();
        fieldValue.setSourceColumnCode(descriptor.sourceColumnCode());
        fieldValue.setSourceColumnName(descriptor.sourceColumnName());
        fieldValue.setValue(value);
        return fieldValue;
    }

    private CeSheet656ValidationIssue issue(String severity, String code, Integer rowNumber, FieldDescriptor descriptor, String message) {
        CeSheet656ValidationIssue issue = new CeSheet656ValidationIssue();
        issue.setSeverity(severity);
        issue.setCode(code);
        issue.setRowNumber(rowNumber);
        issue.setSourceColumnCode(descriptor.sourceColumnCode());
        issue.setSourceColumnName(descriptor.sourceColumnName());
        issue.setMessage(message);
        return issue;
    }

    private record FieldDescriptor(Integer fieldOrder, String sourceColumnCode, String sourceColumnName,
                                   boolean sourceRequired, boolean rowValueRequired, boolean derivedField) {
    }
}
