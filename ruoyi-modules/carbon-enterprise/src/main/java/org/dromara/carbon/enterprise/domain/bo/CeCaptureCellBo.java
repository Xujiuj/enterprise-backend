package org.dromara.carbon.enterprise.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise local data capture cell query object.
 */
@Data
public class CeCaptureCellBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long rowId;

    private Long fieldId;

    private String valueStatus;
}
