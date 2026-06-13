package org.dromara.carbon.enterprise.config;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.interceptor.CeLicenseGateInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the thin enterprise-only license gate enforcement slice.
 */
@RequiredArgsConstructor
@Configuration
public class CeLicenseGateWebMvcConfigurer implements WebMvcConfigurer {

    private final CeLicenseGateInterceptor licenseGateInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licenseGateInterceptor)
            .addPathPatterns(
                "/enterprise/activity-data/**",
                "/enterprise/emission-source/**",
                "/enterprise/extension-field/**",
                "/enterprise/extension-field-value/**",
                "/enterprise/factor-sync/**",
                "/enterprise/factor-cache-version/**",
                "/enterprise/green-power-certificate/**",
                "/enterprise/factor-confirmation/**",
                "/enterprise/intensity-metric/**",
                "/enterprise/capture-batch/**",
                "/enterprise/capture-row/**",
                "/enterprise/capture-cell/**",
                "/enterprise/activity-import/**",
                "/enterprise/data-validation/**",
                "/enterprise/report-template-sync/**",
                "/enterprise/report-template-file/**",
                "/enterprise/dimension-record/**",
                "/enterprise/template-version/**",
                "/enterprise/template-sheet/**",
                "/enterprise/template-field/**"
            )
            .excludePathPatterns(
                "/enterprise/license-state/**",
                "/enterprise/license-import/**",
                "/enterprise/license-gate/**"
            );
    }
}
