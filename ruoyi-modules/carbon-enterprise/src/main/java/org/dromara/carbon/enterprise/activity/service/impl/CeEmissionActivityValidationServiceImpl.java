package org.dromara.carbon.enterprise.activity.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityResolvedRow;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationResult;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityDerivedFieldResolver;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityValidationService;
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
 * Row-level validator for the semantic emission_activity entry contract.
 */
@RequiredArgsConstructor
public class CeEmissionActivityValidationServiceImpl implements ICeEmissionActivityValidationService {

    private static final String SEVERITY_ERROR = "ERROR";
    private static final String SEVERITY_WARNING = "WARNING";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ROOT);
    private static final List<FieldDescriptor> ALL_FIELDS = List.of(
        new FieldDescriptor(1, "sourceIdentificationCode", "排放源识别编号", false, false, true),
        new FieldDescriptor(2, "companyCode", "公司编号", false, false, true),
        new FieldDescriptor(3, "companyName", "公司名称", false, true, false),
        new FieldDescriptor(4, "factoryName", "工厂", false, true, false),
        new FieldDescriptor(5, "sourceCategoryKey", "排放源分类", false, false, true),
        new FieldDescriptor(6, "scopeName", "范围", false, true, false),
        new FieldDescriptor(7, "scopeSubcategory", "范围子类别", false, true, false),
        new FieldDescriptor(8, "sourceIdentificationName", "排放源识别", false, false, true),
        new FieldDescriptor(9, "emissionSourceName", "排放源名称", false, true, false),
        new FieldDescriptor(10, "activityUnit", "单位", false, false, true),
        new FieldDescriptor(11, "activityPeriod", "活动期间", false, true, false),
        new FieldDescriptor(12, "activityYear", "年度", false, false, true),
        new FieldDescriptor(13, "activityMonth", "月份", false, false, true),
        new FieldDescriptor(14, "activityDate", "日期", false, true, false),
        new FieldDescriptor(15, "activityValue", "活动数据", false, true, false),
        new FieldDescriptor(16, "responsibleDept", "负责部门", false, true, false),
        new FieldDescriptor(17, "dataSource", "数据来源", false, true, false),
        new FieldDescriptor(18, "sourceRemark", "备注", false, false, true),
        new FieldDescriptor(19, "factorKey", "排放因子", false, false, true)
    );
    private static final Map<String, FieldDescriptor> FIELD_BY_CODE = buildFieldIndex();

    private final ICeEmissionActivityDerivedFieldResolver derivedFieldResolver;

    public static List<CeEmissionActivityFieldDescriptor> allFieldDescriptors() {
        return ALL_FIELDS.stream()
            .map(CeEmissionActivityValidationServiceImpl::toFieldDescriptor)
            .toList();
    }

    public static List<CeEmissionActivityFieldDescriptor> entryFieldDescriptors() {
        List<FieldDescriptor> entryFields = ALL_FIELDS.stream()
            .filter(field -> !field.derivedField())
            .toList();
        List<CeEmissionActivityFieldDescriptor> descriptors = new ArrayList<>(entryFields.size());
        for (int index = 0; index < entryFields.size(); index++) {
            CeEmissionActivityFieldDescriptor descriptor = toFieldDescriptor(entryFields.get(index));
            descriptor.setFieldOrder(index + 1);
            descriptors.add(descriptor);
        }
        return descriptors;
    }

    @Override
    public List<CeEmissionActivityFieldDescriptor> listEntryFields() {
        return entryFieldDescriptors();
    }

    @Override
    public CeEmissionActivityValidationResult validate(CeEmissionActivityValidationRequest request) {
        Integer rowNumber = request == null ? null : request.getRowNumber();
        List<CeEmissionActivityValidationIssue> issues = new ArrayList<>();
        Map<String, String> clientValues = toClientValueMap(request);

        for (FieldDescriptor field : ALL_FIELDS) {
            String value = clientValues.get(field.fieldCode());
            if (field.rowValueRequired() && StringUtils.isBlank(value)) {
                issues.add(issue(SEVERITY_ERROR, "REQUIRED_FIELD_MISSING", rowNumber, field,
                    "required field is missing"));
            }
        }

        normalizeActivityPeriod(clientValues, rowNumber, issues);
        validateYear(clientValues.get("activityYear"), rowNumber, issues);
        validateMonth(clientValues.get("activityMonth"), rowNumber, issues);
        validateDate(clientValues.get("activityDate"), rowNumber, issues);
        validateActivityValue(clientValues.get("activityValue"), rowNumber, issues);

        List<CeEmissionActivityFieldValue> resolvedDerivedFieldValues = new ArrayList<>();
        addPeriodDerivedValues(clientValues, resolvedDerivedFieldValues);

        String emissionSourceCode = clientValues.get("sourceIdentificationCode");
        if (StringUtils.isNotBlank(emissionSourceCode)) {
            Optional<CeEmissionActivityResolvedRow> resolvedRow = derivedFieldResolver.resolve(emissionSourceCode);
            if (resolvedRow.isEmpty()) {
                issues.add(issue(SEVERITY_ERROR, "MASTER_DATA_NOT_FOUND", rowNumber, descriptor("sourceIdentificationCode"),
                    "no enterprise-local master data match exists for the emission source"));
            } else {
                validateDerivedFields(clientValues, resolvedRow.get(), rowNumber, issues, resolvedDerivedFieldValues);
            }
        } else if (hasEntrySourceFields(clientValues)) {
            List<CeEmissionActivityResolvedRow> matches = derivedFieldResolver.resolveByEntryFields(
                clientValues.get("companyName"),
                clientValues.get("factoryName"),
                clientValues.get("scopeName"),
                clientValues.get("scopeSubcategory"),
                clientValues.get("emissionSourceName")
            );
            if (matches.isEmpty()) {
                issues.add(issue(SEVERITY_ERROR, "MASTER_DATA_NOT_FOUND", rowNumber, descriptor("emissionSourceName"),
                    "no enterprise-local master data match exists for the selected source fields"));
            } else if (matches.size() > 1) {
                issues.add(issue(SEVERITY_ERROR, "MASTER_DATA_NOT_UNIQUE", rowNumber, descriptor("emissionSourceName"),
                    "selected source fields match multiple enterprise-local emission sources"));
            } else {
                validateDerivedFields(clientValues, matches.get(0), rowNumber, issues, resolvedDerivedFieldValues);
            }
        }

        boolean blocking = issues.stream().anyMatch(issue -> SEVERITY_ERROR.equals(issue.getSeverity()));
        CeEmissionActivityValidationResult result = new CeEmissionActivityValidationResult();
        result.setRowNumber(rowNumber);
        result.setValid(!blocking);
        result.setBlocking(blocking);
        result.setDraftSavable(!blocking);
        result.setIssues(issues);
        result.setResolvedDerivedFieldValues(resolvedDerivedFieldValues);
        return result;
    }

    private void validateDerivedFields(Map<String, String> clientValues, CeEmissionActivityResolvedRow resolvedRow, Integer rowNumber,
                                       List<CeEmissionActivityValidationIssue> issues, List<CeEmissionActivityFieldValue> resolvedFields) {
        Map<String, String> serverValues = new LinkedHashMap<>();
        serverValues.put("sourceIdentificationCode", resolvedRow.getEmissionSourceCode());
        serverValues.put("companyCode", resolvedRow.getCompanyCode());
        serverValues.put("companyName", resolvedRow.getCompanyName());
        serverValues.put("factoryName", resolvedRow.getFactoryName());
        serverValues.put("sourceCategoryKey", resolvedRow.getEmissionSourceCategoryCode());
        serverValues.put("scopeName", resolvedRow.getScope());
        serverValues.put("scopeSubcategory", resolvedRow.getScopeSubcategory());
        serverValues.put("sourceIdentificationName", resolvedRow.getEmissionSourceIdentity());
        serverValues.put("emissionSourceName", resolvedRow.getEmissionSourceName());
        serverValues.put("activityUnit", resolvedRow.getUnit());
        serverValues.put("factorKey", resolvedRow.getEmissionFactorCode());

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

    private void addPeriodDerivedValues(Map<String, String> clientValues, List<CeEmissionActivityFieldValue> resolvedFields) {
        if (StringUtils.isNotBlank(clientValues.get("activityYear"))) {
            resolvedFields.add(fieldValue(descriptor("activityYear"), clientValues.get("activityYear")));
        }
        if (StringUtils.isNotBlank(clientValues.get("activityMonth"))) {
            resolvedFields.add(fieldValue(descriptor("activityMonth"), clientValues.get("activityMonth")));
        }
    }

    private boolean hasEntrySourceFields(Map<String, String> clientValues) {
        return StringUtils.isNotBlank(clientValues.get("companyName"))
            && StringUtils.isNotBlank(clientValues.get("factoryName"))
            && StringUtils.isNotBlank(clientValues.get("scopeName"))
            && StringUtils.isNotBlank(clientValues.get("scopeSubcategory"))
            && StringUtils.isNotBlank(clientValues.get("emissionSourceName"));
    }

    private void normalizeActivityPeriod(Map<String, String> clientValues, Integer rowNumber,
                                         List<CeEmissionActivityValidationIssue> issues) {
        String period = clientValues.get("activityPeriod");
        if (StringUtils.isBlank(period)) {
            return;
        }
        try {
            YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
            clientValues.put("activityYear", String.valueOf(yearMonth.getYear()));
            clientValues.put("activityMonth", String.valueOf(yearMonth.getMonthValue()));
        } catch (DateTimeParseException e) {
            issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("activityPeriod"),
                "activity period must be yyyy-MM"));
        }
    }

    private void validateYear(String value, Integer rowNumber, List<CeEmissionActivityValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            int year = Integer.parseInt(value);
            if (year < 1900 || year > 2200) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_VALUE_DOMAIN", rowNumber, descriptor("activityYear"),
                    "year must be between 1900 and 2200"));
            }
        } catch (NumberFormatException e) {
            issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("activityYear"),
                "year must be an integer"));
        }
    }

    private void validateMonth(String value, Integer rowNumber, List<CeEmissionActivityValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            int month = Integer.parseInt(value);
            if (month < 1 || month > 12) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_VALUE_DOMAIN", rowNumber, descriptor("activityMonth"),
                    "month must be between 1 and 12"));
            }
        } catch (NumberFormatException e) {
            issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("activityMonth"),
                "month must be an integer"));
        }
    }

    private void validateDate(String value, Integer rowNumber, List<CeEmissionActivityValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException firstFailure) {
            try {
                YearMonth.parse(value, PERIOD_FORMATTER);
            } catch (DateTimeParseException ignored) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("activityDate"),
                    "date must be yyyy-MM-dd or yyyy-MM"));
            }
        }
    }

    private void validateActivityValue(String value, Integer rowNumber, List<CeEmissionActivityValidationIssue> issues) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                issues.add(issue(SEVERITY_ERROR, "INVALID_VALUE_DOMAIN", rowNumber, descriptor("activityValue"),
                    "activity value must be greater than zero"));
            }
        } catch (NumberFormatException e) {
            issues.add(issue(SEVERITY_ERROR, "INVALID_TYPE", rowNumber, descriptor("activityValue"),
                "activity value must be numeric"));
        }
    }

    private Map<String, String> toClientValueMap(CeEmissionActivityValidationRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        if (request == null || request.getFieldValues() == null) {
            return values;
        }
        for (CeEmissionActivityFieldValue fieldValue : request.getFieldValues()) {
            if (fieldValue == null || StringUtils.isBlank(fieldValue.getFieldCode())) {
                continue;
            }
            String code = fieldValue.getFieldCode().trim();
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

    private static CeEmissionActivityFieldDescriptor toFieldDescriptor(FieldDescriptor field) {
        CeEmissionActivityFieldDescriptor descriptor = new CeEmissionActivityFieldDescriptor();
        descriptor.setFieldOrder(field.fieldOrder());
        descriptor.setFieldCode(field.fieldCode());
        descriptor.setFieldName(field.fieldName());
        descriptor.setSourceRequired(field.sourceRequired());
        descriptor.setRowValueRequired(field.rowValueRequired());
        descriptor.setDerivedField(field.derivedField());
        return descriptor;
    }

    private static Map<String, FieldDescriptor> buildFieldIndex() {
        Map<String, FieldDescriptor> fieldByCode = new LinkedHashMap<>();
        for (FieldDescriptor descriptor : ALL_FIELDS) {
            fieldByCode.put(descriptor.fieldCode(), descriptor);
        }
        return fieldByCode;
    }

    private FieldDescriptor descriptor(String code) {
        return FIELD_BY_CODE.get(code);
    }

    private CeEmissionActivityFieldValue fieldValue(FieldDescriptor descriptor, String value) {
        CeEmissionActivityFieldValue fieldValue = new CeEmissionActivityFieldValue();
        fieldValue.setFieldCode(descriptor.fieldCode());
        fieldValue.setFieldName(descriptor.fieldName());
        fieldValue.setValue(value);
        return fieldValue;
    }

    private CeEmissionActivityValidationIssue issue(String severity, String code, Integer rowNumber, FieldDescriptor descriptor, String message) {
        CeEmissionActivityValidationIssue issue = new CeEmissionActivityValidationIssue();
        issue.setSeverity(severity);
        issue.setCode(code);
        issue.setRowNumber(rowNumber);
        issue.setFieldCode(descriptor.fieldCode());
        issue.setFieldName(descriptor.fieldName());
        issue.setMessage(message);
        return issue;
    }

    private record FieldDescriptor(Integer fieldOrder, String fieldCode, String fieldName,
                                   boolean sourceRequired, boolean rowValueRequired, boolean derivedField) {
    }
}
