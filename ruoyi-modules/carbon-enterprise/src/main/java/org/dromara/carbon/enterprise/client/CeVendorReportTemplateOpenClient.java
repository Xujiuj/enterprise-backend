package org.dromara.carbon.enterprise.client;

import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateDownloadResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateListResponse;

/**
 * Enterprise-side client for vendor open report template APIs.
 */
public interface CeVendorReportTemplateOpenClient {

    /**
     * List authorized report templates from vendor open API.
     *
     * @param licenseId license id
     * @param installId install id
     * @return authorized templates
     */
    CeVendorReportTemplateListResponse listTemplates(String licenseId, String installId);

    /**
     * Download authorized report template metadata from vendor open API.
     *
     * @param templateId vendor template id
     * @param licenseId license id
     * @param installId install id
     * @return download metadata
     */
    CeVendorReportTemplateDownloadResponse downloadTemplate(Long templateId, String licenseId, String installId);

    /**
     * Consume vendor one-time download token and return template bytes.
     *
     * @param downloadToken one-time download token
     * @return template file bytes
     */
    byte[] downloadTemplateFile(String downloadToken);
}
