package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.sync.CeReportTemplateSyncResponse;

/**
 * Enterprise report template sync service.
 */
public interface ICeReportTemplateSyncService {

    /**
     * Sync report templates authorized by current active license.
     *
     * @return sync result
     */
    CeReportTemplateSyncResponse syncCurrentLicenseReportTemplates();
}
