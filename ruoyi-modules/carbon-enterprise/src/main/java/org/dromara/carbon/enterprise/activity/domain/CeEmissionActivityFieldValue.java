package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

/**
 * Single source column value for one emission_activity row.
 */
@Data
public class CeEmissionActivityFieldValue {

    private String fieldCode;

    private String fieldName;

    private String value;
}
