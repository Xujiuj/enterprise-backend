package org.dromara.carbon.enterprise.domain.bo;

import lombok.Data;

/**
 * Enterprise option query filters.
 */
@Data
public class CeOptionQueryBo {

    private String dimensionCode;

    private String field;

    private String companyName;

    private String factoryName;

    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    private String sourceIdentificationName;
}
