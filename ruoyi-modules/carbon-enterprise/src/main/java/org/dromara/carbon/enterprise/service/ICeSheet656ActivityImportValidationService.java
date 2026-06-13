package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ImportValidationResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validate-only import API boundary for sheet_656.
 */
public interface ICeSheet656ActivityImportValidationService {

    CeSheet656ImportValidationRequest parseImportFile(MultipartFile file);

    CeSheet656ImportValidationResult validateImport(CeSheet656ImportValidationRequest request);
}
