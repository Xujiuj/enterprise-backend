package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Factor record returned by vendor open factor API.
 */
@Data
public class CeVendorFactorRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String factorCode;

    private String factorName;

    private String factorCategory;

    private BigDecimal factorValue;

    private String factorUnit;

    private String sourceRef;
}
