package org.dromara.carbon.enterprise.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise local data capture row query object.
 */
@Data
public class CeCaptureRowBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batchId;

    private Long sheetId;

    private String rowStatus;

    private String validationLevel;
}
