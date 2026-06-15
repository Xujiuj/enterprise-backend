package org.dromara.carbon.enterprise.license;

import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Date;
import java.util.Objects;

/**
 * Periodically writes back expired enterprise licenses into local runtime state.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "carbon.license.expiry-write-back", name = "enabled", havingValue = "true")
public class CeLicenseExpiryScheduler {

    private final ICeLicenseStateService licenseStateService;
    private final Clock clock;

    public CeLicenseExpiryScheduler(ICeLicenseStateService licenseStateService, Clock clock) {
        this.licenseStateService = Objects.requireNonNull(licenseStateService, "licenseStateService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Scheduled(cron = "${carbon.license.expiry-write-back.cron}")
    public void expireValidLicenses() {
        int expiredRows = licenseStateService.expireValidLicenses(Date.from(clock.instant()));
        log.info("Enterprise license expiry write-back completed, expiredRows={}", expiredRows);
    }
}
