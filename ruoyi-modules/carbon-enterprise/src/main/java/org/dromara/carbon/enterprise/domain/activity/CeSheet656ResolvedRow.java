package org.dromara.carbon.enterprise.domain.activity;

import lombok.Data;

/**
 * Enterprise-local resolved master data for one sheet_656 emission source.
 */
@Data
public class CeSheet656ResolvedRow {

    private String emissionSourceCode;

    private String companyCode;

    private String companyName;

    private String factoryName;

    private String emissionSourceCategoryCode;

    private String scope;

    private String scopeSubcategory;

    private String emissionSourceIdentity;

    private String emissionSourceName;

    private String unit;

    private String emissionFactorCode;
}
