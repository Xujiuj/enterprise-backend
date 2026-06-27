package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

/**
 * Row-level validation issue for emission_activity.
 */
@Data
public class CeEmissionActivityValidationIssue {

    private String severity;

    private String code;

    private Integer rowNumber;

    private String fieldCode;

    private String fieldName;

    private String message;
}
