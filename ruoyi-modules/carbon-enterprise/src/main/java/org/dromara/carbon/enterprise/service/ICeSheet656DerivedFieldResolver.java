package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.activity.CeSheet656ResolvedRow;

import java.util.Optional;

/**
 * Enterprise-local seam for resolving sheet_656 derived fields.
 */
public interface ICeSheet656DerivedFieldResolver {

    Optional<CeSheet656ResolvedRow> resolve(String emissionSourceCode);
}
