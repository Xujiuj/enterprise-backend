package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityCaptureResult;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;

/**
 * Enterprise-local capture boundary for emission_activity activity rows.
 */
public interface ICeEmissionActivityCaptureService {

    CeEmissionActivityCaptureResult saveManual(CeEmissionActivityValidationRequest request);

    CeEmissionActivityCaptureResult importRows(CeEmissionActivityImportValidationRequest request);
}
