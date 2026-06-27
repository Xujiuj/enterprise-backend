package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityImportValidationResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validate-only import API boundary for emission_activity.
 */
public interface ICeEmissionActivityImportValidationService {

    CeEmissionActivityImportValidationRequest parseImportFile(MultipartFile file);

    CeEmissionActivityImportValidationResult validateImport(CeEmissionActivityImportValidationRequest request);
}
