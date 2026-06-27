package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

/**
 * Persistence result for enterprise-local emission_activity activity capture.
 */
@Data
public class CeEmissionActivityCaptureResult {

    private boolean persisted;

    private Long batchId;

    private Integer persistedRowCount;

    private CeEmissionActivityImportValidationResult validationResult;
}
