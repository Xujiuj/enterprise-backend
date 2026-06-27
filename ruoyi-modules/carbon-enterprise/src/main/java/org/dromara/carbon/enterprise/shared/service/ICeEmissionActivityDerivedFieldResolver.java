package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityResolvedRow;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Enterprise-local seam for resolving emission_activity derived fields.
 */
public interface ICeEmissionActivityDerivedFieldResolver {

    Optional<CeEmissionActivityResolvedRow> resolve(String emissionSourceCode);

    default List<CeEmissionActivityResolvedRow> resolveByEntryFields(String companyName, String factoryName, String scope,
                                                             String scopeSubcategory, String sourceIdentificationName,
                                                             String emissionSourceName) {
        return Collections.emptyList();
    }
}
