package org.dromara.carbon.enterprise.domain.sourcea;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Source(A) composite import and audit result.
 */
@Data
public class CeSourceAImportResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean imported;

    private String sourceMode;

    private int workbookCount;

    private int sheetCount;

    private int sourceRowCount;

    private Map<String, Integer> tableRows = new LinkedHashMap<>();

    private List<String> warnings = new ArrayList<>();

    private List<String> errors = new ArrayList<>();

    private List<CeSourceAValidationIssue> validationIssues = new ArrayList<>();

    public void addTableRows(String tableName, int rowCount) {
        tableRows.put(tableName, tableRows.getOrDefault(tableName, 0) + rowCount);
    }

    public void warn(String warning) {
        warnings.add(warning);
    }

    public void error(String error) {
        errors.add(error);
    }

    public void issue(String severity, String code, String message, int count) {
        validationIssues.add(new CeSourceAValidationIssue(severity, code, message, count));
        if ("error".equals(severity)) {
            error(message);
        } else {
            warn(message);
        }
    }
}
