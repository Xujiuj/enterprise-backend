package org.dromara.carbon.enterprise.license;

import org.dromara.carbon.enterprise.config.CeEnterpriseSchedulingConfig;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeLicenseExpirySchedulerTest {

    @Test
    void expiresValidLicensesUsingCurrentClockTime() {
        ICeLicenseStateService stateService = mock(ICeLicenseStateService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T08:30:00Z"), ZoneOffset.UTC);
        CeLicenseExpiryScheduler scheduler = new CeLicenseExpiryScheduler(stateService, clock);

        scheduler.expireValidLicenses();

        verify(stateService).expireValidLicenses(Date.from(clock.instant()));
    }

    @Nested
    class ConfigurationTest {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                CeEnterpriseSchedulingConfig.class,
                CeLicenseExpiryScheduler.class,
                TestBeans.class
            );

        @Test
        void createsSchedulerWhenExpiryWriteBackIsEnabledAndCronIsConfigured() {
            contextRunner
                .withPropertyValues(
                    "carbon.license.expiry-write-back.enabled=true",
                    "carbon.license.expiry-write-back.cron=0 0/5 * * * ?"
                )
                .run(context -> assertThat(context).hasSingleBean(CeLicenseExpiryScheduler.class));
        }

        @Test
        void skipsSchedulerWhenExpiryWriteBackIsDisabled() {
            contextRunner
                .withPropertyValues(
                    "carbon.license.expiry-write-back.enabled=false",
                    "carbon.license.expiry-write-back.cron=0 0/5 * * * ?"
                )
                .run(context -> assertThat(context).doesNotHaveBean(CeLicenseExpiryScheduler.class));
        }

        @Test
        void failsFastWhenEnabledWithoutCron() {
            contextRunner
                .withPropertyValues("carbon.license.expiry-write-back.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                        .hasMessageContaining("carbon.license.expiry-write-back.cron");
                });
        }
    }

    @Configuration
    static class TestBeans {

        @Bean
        ICeLicenseStateService licenseStateService() {
            ICeLicenseStateService service = mock(ICeLicenseStateService.class);
            when(service.expireValidLicenses(any(Date.class))).thenReturn(0);
            return service;
        }
    }
}
