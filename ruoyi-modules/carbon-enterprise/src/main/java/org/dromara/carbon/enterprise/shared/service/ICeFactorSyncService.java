package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.factor.domain.CeFactorSyncResponse;

/**
 * Enterprise factor sync service.
 */
public interface ICeFactorSyncService {

    /**
     * Pull factor records from vendor open API into local enterprise cache.
     *
     * @return sync result
     */
    CeFactorSyncResponse syncCurrentLicenseFactors();
}
