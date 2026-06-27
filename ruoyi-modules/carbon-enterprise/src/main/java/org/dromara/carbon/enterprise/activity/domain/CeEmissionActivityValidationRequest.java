package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

import java.util.List;

/**
 * Validation request for one emission_activity row.
 */
@Data
public class CeEmissionActivityValidationRequest {

    private Integer rowNumber;

    private List<CeEmissionActivityFieldValue> fieldValues;
}
