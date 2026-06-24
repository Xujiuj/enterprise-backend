package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Vendor table field definition returned by open APIs.
 */
@Data
public class CeVendorTableFieldDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tableGroup;

    private String tableCode;

    private String fieldKey;

    private String fieldLabel;

    private String fieldType;

    private Integer fieldPrecision;

    private Integer fieldWidth;

    private Boolean requiredFlag;

    private Integer sortOrder;
}
