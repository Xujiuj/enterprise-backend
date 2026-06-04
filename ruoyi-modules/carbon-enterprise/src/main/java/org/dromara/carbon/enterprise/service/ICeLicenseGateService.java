package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.license.CeLicenseGateResult;

import java.util.Date;

/**
 * Runtime gate service for enterprise license consumers.
 */
public interface ICeLicenseGateService {

    CeLicenseGateResult evaluateCurrent(String expectedInstallId, Date evaluationTime);
}
