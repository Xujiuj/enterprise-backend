package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

/**
 * Frozen row-level field contract for emission_activity.
 */
@Data
public class CeEmissionActivityFieldDescriptor {

    private Integer fieldOrder;

    private String fieldCode;

    private String fieldName;

    private boolean sourceRequired;

    private boolean rowValueRequired;

    private boolean derivedField;
}
