package org.dromara.carbon.enterprise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Enterprise module scheduling infrastructure.
 */
@Configuration
@EnableScheduling
public class CeEnterpriseSchedulingConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
