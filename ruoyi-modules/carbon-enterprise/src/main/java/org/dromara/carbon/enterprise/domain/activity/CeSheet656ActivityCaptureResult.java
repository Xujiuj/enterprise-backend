package org.dromara.carbon.enterprise.domain.activity;

import lombok.Data;

/**
 * Persistence result for enterprise-local sheet_656 activity capture.
 */
@Data
public class CeSheet656ActivityCaptureResult {

    private boolean persisted;

    private Long batchId;

    private Integer persistedRowCount;

    private CeSheet656ImportValidationResult validationResult;
}
