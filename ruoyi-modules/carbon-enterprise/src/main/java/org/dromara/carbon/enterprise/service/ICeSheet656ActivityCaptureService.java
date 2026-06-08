package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.activity.CeSheet656ActivityCaptureResult;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;

/**
 * Enterprise-local capture boundary for sheet_656 activity rows.
 */
public interface ICeSheet656ActivityCaptureService {

    CeSheet656ActivityCaptureResult saveManual(CeSheet656ValidationRequest request);

    CeSheet656ActivityCaptureResult importRows(CeSheet656ImportValidationRequest request);
}
