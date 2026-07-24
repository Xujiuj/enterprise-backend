package org.dromara.carbon.enterprise.activity.domain;

import lombok.Data;

/**
 * Enterprise-local resolved master data for one emission_activity emission source.
 */
@Data
public class CeEmissionActivityResolvedRow {

    private String emissionSourceCode;

    private String companyCode;

    private String companyName;

    private String factoryCode;

    private String factoryName;

    private String emissionSourceCategoryCode;

    private String scope;

    private String scopeSubcategory;

    private String emissionSourceIdentity;

    private String emissionSourceName;

    private String unit;

    private String emissionFactorCode;

    private String responsibleDept;

    private String dataSource;
}
