package org.dromara.carbon.enterprise.domain.activity;

import lombok.Data;

import java.util.List;

/**
 * Validation result for one sheet_656 row.
 */
@Data
public class CeSheet656ValidationResult {

    private Integer rowNumber;

    private boolean valid;

    private boolean blocking;

    private boolean draftSavable;

    private List<CeSheet656ValidationIssue> issues;

    private List<CeSheet656FieldValue> resolvedDerivedFieldValues;
}
