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
                "/enterprise/factor-sync/**",
                "/enterprise/report-template-sync/**",
                "/enterprise/report-template-file/download/**"
            )
            .excludePathPatterns(
                "/enterprise/license-state/**",
                "/enterprise/license-import/**",
                "/enterprise/license-gate/**"
            );
    }
}
