package org.dromara.carbon.enterprise.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise local data capture batch query object.
 */
@Data
public class CeCaptureBatchBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long templateVersionId;

    private String moduleCode;

    private String sourceMode;

    private String batchStatus;

    private String validationStatus;
}
