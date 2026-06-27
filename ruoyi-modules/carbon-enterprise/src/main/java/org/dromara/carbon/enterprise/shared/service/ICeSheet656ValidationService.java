package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.activity.domain.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ValidationResult;

import java.util.List;

/**
 * Row-level validator for sheet_656 rows. EB-4 owns import header shape enforcement.
 */
public interface ICeSheet656ValidationService {

    List<CeSheet656FieldDescriptor> listFrozenFields();

    CeSheet656ValidationResult validate(CeSheet656ValidationRequest request);
}
