package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.license.domain.CeLicenseImportResult;

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
