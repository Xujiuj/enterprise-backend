package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.report.domain.CeReportTemplateSyncResponse;

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
