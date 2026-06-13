package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.sync.CeFactorSyncResponse;

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
