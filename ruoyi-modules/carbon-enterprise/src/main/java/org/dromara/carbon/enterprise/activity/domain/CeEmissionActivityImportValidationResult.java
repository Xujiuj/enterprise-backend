package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

import java.util.List;

/**
 * Validate-only import result for the frozen emission_activity shape.
 */
@Data
public class CeEmissionActivityImportValidationResult {

    private boolean headerValid;

    private boolean valid;

    private boolean blocking;

    private List<CeEmissionActivityValidationIssue> headerIssues;

    private List<CeEmissionActivityValidationResult> rowResults;
}
