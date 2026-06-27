package org.dromara.carbon.enterprise.sourcea.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Source(A) data quality or relationship validation issue.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CeSourceAValidationIssue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String severity;

    private String code;

    private String message;

    private int count;
}
