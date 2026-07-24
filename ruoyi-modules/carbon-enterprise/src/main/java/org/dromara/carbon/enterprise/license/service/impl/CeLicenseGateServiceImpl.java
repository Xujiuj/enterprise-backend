package org.dromara.carbon.enterprise.license.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.license.domain.CeLicenseGateResult;
import org.dromara.carbon.enterprise.license.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseGateService;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseStateService;
import org.dromara.carbon.enterprise.vendor.client.CeVendorLicenseOpenClient;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorLicenseCurrentResponse;
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
    private final CeVendorLicenseOpenClient vendorLicenseOpenClient;
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
        CeLicenseGateResult vendorResult = evaluateVendorBinding(currentState, requiredFeatureCode);
        if (vendorResult != null) {
            return vendorResult;
        }
        return new CeLicenseGateResult("ALLOW", "VALID", currentState);
    }

    private CeLicenseGateResult evaluateVendorBinding(CeLicenseStateVo currentState, String requiredFeatureCode) {
        try {
            CeVendorLicenseCurrentResponse response = vendorLicenseOpenClient.currentLicense(
                currentState.getLicenseId(),
                currentState.getInstallId(),
                currentState.getKeyId(),
                currentState.getCurrentSummary()
            );
            if (response == null || !"active".equalsIgnoreCase(response.getStatus())) {
                return new CeLicenseGateResult("DENY", "VENDOR_LICENSE_NOT_ACTIVE", currentState);
            }
            if (!Objects.equals(currentState.getLicenseId(), response.getLicenseId())) {
                return new CeLicenseGateResult("DENY", "LICENSE_BINDING_FAILED", currentState);
            }
            if (StringUtils.isNotBlank(requiredFeatureCode) && !hasFeature(response.getFeatureCodes(), requiredFeatureCode)) {
                return new CeLicenseGateResult("DENY", "FEATURE_NOT_ENABLED", currentState);
            }
            return null;
        } catch (Exception e) {
            return new CeLicenseGateResult("DENY", "LICENSE_BINDING_FAILED", currentState);
        }
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
        String normalized = featureCodes
            .replace("[", "")
            .replace("]", "")
            .replace("\"", "");
        return Arrays.stream(normalized.split("[,;\\s]+"))
            .filter(StringUtils::isNotBlank)
            .anyMatch(requiredFeatureCode::equals);
    }
}
