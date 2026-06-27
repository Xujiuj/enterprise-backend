package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

import java.util.List;

/**
 * Validation result for one emission_activity row.
 */
@Data
public class CeEmissionActivityValidationResult {

    private Integer rowNumber;

    private boolean valid;

    private boolean blocking;

    private boolean draftSavable;

    private List<CeEmissionActivityValidationIssue> issues;

    private List<CeEmissionActivityFieldValue> resolvedDerivedFieldValues;
}
