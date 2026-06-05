package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationResult;

import java.util.List;

/**
 * Row-level validator for sheet_656 rows. EB-4 owns import header shape enforcement.
 */
public interface ICeSheet656ValidationService {

    List<CeSheet656FieldDescriptor> listFrozenFields();

    CeSheet656ValidationResult validate(CeSheet656ValidationRequest request);
}
