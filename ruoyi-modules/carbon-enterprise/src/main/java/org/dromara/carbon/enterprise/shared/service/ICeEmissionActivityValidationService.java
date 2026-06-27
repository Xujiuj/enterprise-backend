package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationResult;

import java.util.List;

/**
 * Row-level validator for emission_activity rows. Import header validation exposes only entry fields.
 */
public interface ICeEmissionActivityValidationService {

    List<CeEmissionActivityFieldDescriptor> listEntryFields();

    CeEmissionActivityValidationResult validate(CeEmissionActivityValidationRequest request);
}
