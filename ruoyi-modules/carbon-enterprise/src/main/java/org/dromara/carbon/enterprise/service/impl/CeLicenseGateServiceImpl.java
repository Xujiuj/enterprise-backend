package org.dromara.carbon.enterprise.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.license.CeLicenseGateResult;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.service.ICeLicenseGateService;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * License gate backed by the current persisted enterprise license state.
 */
@RequiredArgsConstructor
@Service
public class CeLicenseGateServiceImpl implements ICeLicenseGateService {

    private static final long STATE_CACHE_TTL_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final ICeLicenseStateService licenseStateService;
    private volatile CeLicenseStateVo cachedCurrentState;
    private volatile long cachedCurrentStateAtNanos;
    private volatile boolean cachedCurrentStateLoaded;

    @Override
    public CeLicenseGateResult evaluateCurrent(String expectedInstallId, Date evaluationTime, String requiredFeatureCode) {
        CeLicenseStateVo currentState = queryCurrentStateCached();
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
        if (StringUtils.isNotBlank(requiredFeatureCode) && !hasFeature(currentState.getFeatureCodes(), requiredFeatureCode)) {
            return new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", currentState);
        }
        return new CeLicenseGateResult("ALLOW", "VALID", currentState);
    }

    private CeLicenseStateVo queryCurrentStateCached() {
        long now = System.nanoTime();
        CeLicenseStateVo state = cachedCurrentState;
        if (cachedCurrentStateLoaded && now - cachedCurrentStateAtNanos <= STATE_CACHE_TTL_NANOS) {
            return state;
        }
        synchronized (this) {
            now = System.nanoTime();
            state = cachedCurrentState;
            if (cachedCurrentStateLoaded && now - cachedCurrentStateAtNanos <= STATE_CACHE_TTL_NANOS) {
                return state;
            }
            state = licenseStateService.queryCurrent();
            cachedCurrentState = state;
            cachedCurrentStateAtNanos = now;
            cachedCurrentStateLoaded = true;
            return state;
        }
    }

    private boolean hasFeature(String featureCodes, String requiredFeatureCode) {
        if (StringUtils.isBlank(featureCodes)) {
            return false;
        }
        return Arrays.stream(featureCodes.split("[,;\\s]+"))
            .filter(StringUtils::isNotBlank)
            .anyMatch(requiredFeatureCode::equals);
    }
}
