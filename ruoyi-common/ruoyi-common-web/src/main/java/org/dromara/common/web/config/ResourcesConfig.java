package org.dromara.common.web.config;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.dromara.common.web.interceptor.PlusWebInvokeTimeInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Date;

/**
 * 通用配置
 *
 * @author Lion Li
 */
@AutoConfiguration
public class ResourcesConfig implements WebMvcConfigurer {

    private static final String DEFAULT_AVATAR_UPLOAD_DIR = "/opt/fx/www/uploads/avatar";
    private static final String AVATAR_UPLOAD_DIR_PROPERTY = "fx.avatar.storage-dir";
    private static final String AVATAR_UPLOAD_DIR_ENV = "FX_AVATAR_STORAGE_DIR";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PlusWebInvokeTimeInterceptor());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Date.class, source -> {
            DateTime parse = DateUtil.parse(source);
            if (ObjectUtils.isNull(parse)) {
                return null;
            }
            return parse.toJdkDate();
        });
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = resolveAvatarStorageDirectory().toUri().toString();
        registry.addResourceHandler("/uploads/avatar/**")
            .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }

    private Path resolveAvatarStorageDirectory() {
        String configuredPath = System.getProperty(AVATAR_UPLOAD_DIR_PROPERTY);
        if (StringUtils.isBlank(configuredPath)) {
            configuredPath = System.getenv(AVATAR_UPLOAD_DIR_ENV);
        }
        if (StringUtils.isBlank(configuredPath)) {
            configuredPath = DEFAULT_AVATAR_UPLOAD_DIR;
        }
        return Path.of(configuredPath).toAbsolutePath().normalize();
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 全局异常处理器
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
