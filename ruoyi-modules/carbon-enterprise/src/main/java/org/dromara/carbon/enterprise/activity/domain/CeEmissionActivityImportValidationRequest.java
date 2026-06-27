package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

import java.util.List;

/**
 * Validate-only import request for the frozen emission_activity shape.
 */
@Data
public class CeEmissionActivityImportValidationRequest {

    private List<CeEmissionActivityFieldDescriptor> headerFields;

    private List<CeEmissionActivityValidationRequest> rows;
}
