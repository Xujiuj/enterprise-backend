package org.dromara.carbon.enterprise.service.impl;

import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationResult;
import org.dromara.carbon.enterprise.service.ICeSheet656ActivityImportValidationService;
import org.dromara.carbon.enterprise.service.ICeSheet656DerivedFieldResolver;
import org.dromara.carbon.enterprise.service.ICeSheet656ValidationService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thin validate-only import service for the frozen sheet_656 shape.
 */
@Service
public class CeSheet656ActivityImportValidationServiceImpl implements ICeSheet656ActivityImportValidationService {

    private static final String SEVERITY_ERROR = "ERROR";
    private static final String DERIVED_RESOLVER_UNAVAILABLE = "DERIVED_RESOLVER_UNAVAILABLE";

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
        for (CeSheet656ValidationRequest row : rows) {
            if (row != null && row.getRowNumber() != null) {
                results.add(rowValidator.validate(row));
                continue;
            }
            CeSheet656ValidationResult malformedResult = new CeSheet656ValidationResult();
            malformedResult.setRowNumber(row == null ? null : row.getRowNumber());
            malformedResult.setValid(false);
            malformedResult.setBlocking(true);
            malformedResult.setDraftSavable(false);
            malformedResult.setResolvedDerivedFieldValues(Collections.emptyList());
            malformedResult.setIssues(List.of(headerIssue("ROW_NUMBER_MISSING", "rowNumber", "rowNumber",
                "rowNumber must be provided for each import row")));
            results.add(malformedResult);
        }
        return results;
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
}
