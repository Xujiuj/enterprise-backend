package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResult;

import java.util.Date;

/**
 * Enterprise-side license import and verification service.
 */
public interface ICeLicenseImportService {

    CeLicenseImportResult verifyLicense(String licenseContent, String publicKeyPem, String expectedInstallId,
                                        Date verificationTime, Date maxObservedTime);

    CeLicenseImportResult importLicense(String licenseContent, String publicKeyPem, String expectedInstallId,
                                        Date verificationTime);
}
