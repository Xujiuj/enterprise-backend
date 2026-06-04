package org.dromara.carbon.enterprise.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.license.CeLicenseGateResult;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.service.ICeLicenseGateService;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

/**
 * License gate backed by the current persisted enterprise license state.
 */
@RequiredArgsConstructor
@Service
public class CeLicenseGateServiceImpl implements ICeLicenseGateService {

    private final ICeLicenseStateService licenseStateService;

    @Override
    public CeLicenseGateResult evaluateCurrent(String expectedInstallId, Date evaluationTime) {
        CeLicenseStateVo currentState = licenseStateService.queryCurrent();
        if (currentState == null || !"VALID".equals(currentState.getLicenseStatus())) {
            return new CeLicenseGateResult("DENY", "NO_VALID_LICENSE", currentState);
        }
        if (evaluationTime != null
            && currentState.getMaxObservedTime() != null
            && evaluationTime.before(currentState.getMaxObservedTime())) {
            return new CeLicenseGateResult("DENY", "CLOCK_ROLLBACK", currentState);
        }
        if (evaluationTime != null
            && currentState.getValidTo() != null
            && evaluationTime.after(currentState.getValidTo())) {
            return new CeLicenseGateResult("DENY", "EXPIRED", currentState);
        }
        if (!Objects.equals(expectedInstallId, currentState.getInstallId())) {
            return new CeLicenseGateResult("DENY", "INSTALL_ID_MISMATCH", currentState);
        }
        return new CeLicenseGateResult("ALLOW", "VALID", currentState);
    }
}
