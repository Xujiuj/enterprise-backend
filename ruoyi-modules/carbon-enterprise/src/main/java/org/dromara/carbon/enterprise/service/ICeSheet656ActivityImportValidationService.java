package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;

/**
 * Validate-only import API boundary for sheet_656.
 */
public interface ICeSheet656ActivityImportValidationService {

    CeSheet656ImportValidationResult validateImport(CeSheet656ImportValidationRequest request);
}
