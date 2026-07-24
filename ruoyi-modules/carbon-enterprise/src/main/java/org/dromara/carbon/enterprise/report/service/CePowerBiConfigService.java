package org.dromara.carbon.enterprise.report.service;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.domain.CePowerBiConfig;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stores the enterprise Power BI URL outside the frontend bundle.
 */
@RequiredArgsConstructor
@Service
public class CePowerBiConfigService {

    private static final String SETTING_KEY = "powerbi.embedUrl";
    private static final String DEFAULT_EMBED_URL =
        "https://app.powerbi.com/view?r=eyJrIjoiYjQzODVjYmEtYzFiMy00NDQ0LWIwZTAtMjM2YmVjOWNlZDAyIiwidCI6ImU2NDExZmRiLTZkNzctNGZmZC1iMDE1LTYxOWM3NWIxMzc2OCIsImMiOjEwfQ%3D%3D";

    private final JdbcTemplate jdbcTemplate;

    public CePowerBiConfig getConfig() {
        ensureTable();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT setting_value, update_by, update_time
              FROM ce_report_setting
             WHERE setting_key = ?
            """, SETTING_KEY);
        CePowerBiConfig config = new CePowerBiConfig();
        if (rows.isEmpty()) {
            config.setEmbedUrl(DEFAULT_EMBED_URL);
            return config;
        }
        Map<String, Object> row = rows.get(0);
        config.setEmbedUrl(String.valueOf(row.get("setting_value")));
        if (row.get("update_by") instanceof Number value) {
            config.setUpdateBy(value.longValue());
        }
        if (row.get("update_time") instanceof Date value) {
            config.setUpdateTime(value);
        }
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    public CePowerBiConfig saveConfig(CePowerBiConfig input) {
        ensureTable();
        String embedUrl = validateAndNormalizeUrl(input == null ? null : input.getEmbedUrl());
        Long userId = LoginHelper.getUserId();
        jdbcTemplate.update("""
            MERGE ce_report_setting AS target
            USING (SELECT CAST(? AS NVARCHAR(100)) AS setting_key) AS source
               ON target.setting_key = source.setting_key
            WHEN MATCHED THEN
                UPDATE SET setting_value = ?, update_by = ?, update_time = SYSDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (setting_key, setting_value, update_by, update_time)
                VALUES (?, ?, ?, SYSDATETIME());
            """, SETTING_KEY, embedUrl, userId, SETTING_KEY, embedUrl, userId);
        return getConfig();
    }

    static String validateAndNormalizeUrl(String rawUrl) {
        String embedUrl = StringUtils.trimToEmpty(rawUrl);
        if (StringUtils.isBlank(embedUrl)) {
            throw new ServiceException("Power BI嵌入链接不能为空");
        }
        if (embedUrl.length() > 8000) {
            throw new ServiceException("Power BI嵌入链接长度不能超过8000个字符");
        }
        try {
            URI uri = new URI(embedUrl);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || !("powerbi.com".equals(host) || host.endsWith(".powerbi.com"))) {
                throw new ServiceException("仅支持Power BI官方HTTPS嵌入链接");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            throw new ServiceException("Power BI嵌入链接格式不正确");
        }
    }

    private void ensureTable() {
        jdbcTemplate.execute("""
            IF OBJECT_ID(N'dbo.ce_report_setting', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.ce_report_setting (
                    setting_key NVARCHAR(100) NOT NULL CONSTRAINT pk_ce_report_setting PRIMARY KEY,
                    setting_value NVARCHAR(MAX) NOT NULL,
                    update_by BIGINT NULL,
                    update_time DATETIME2 NULL
                );
            END
            """);
    }
}
