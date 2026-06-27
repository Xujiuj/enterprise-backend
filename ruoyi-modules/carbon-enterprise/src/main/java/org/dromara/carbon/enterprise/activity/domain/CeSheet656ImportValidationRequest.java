package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

import java.util.List;

/**
 * Validate-only import request for the frozen sheet_656 shape.
 */
@Data
public class CeSheet656ImportValidationRequest {

    private List<CeSheet656FieldDescriptor> headerFields;

    private List<CeSheet656ValidationRequest> rows;
}
