package org.dromara.carbon.enterprise.domain.activity;

import lombok.Data;

import java.util.List;

/**
 * Validation request for one sheet_656 row.
 */
@Data
public class CeSheet656ValidationRequest {

    private Integer rowNumber;

    private List<CeSheet656FieldValue> fieldValues;
}
