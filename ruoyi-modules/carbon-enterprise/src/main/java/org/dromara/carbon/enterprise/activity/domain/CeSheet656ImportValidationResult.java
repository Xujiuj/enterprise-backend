package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

import java.util.List;

/**
 * Validate-only import result for the frozen sheet_656 shape.
 */
@Data
public class CeSheet656ImportValidationResult {

    private boolean headerValid;

    private boolean valid;

    private boolean blocking;

    private List<CeSheet656ValidationIssue> headerIssues;

    private List<CeSheet656ValidationResult> rowResults;
}
