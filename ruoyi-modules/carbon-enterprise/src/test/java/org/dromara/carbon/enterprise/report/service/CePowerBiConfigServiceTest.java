package org.dromara.carbon.enterprise.report.service;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class CePowerBiConfigServiceTest {

    @Test
    void acceptsOfficialPowerBiEmbedUrlWithTokenQuery() {
        String url = "https://app.powerbi.com/reportEmbed?reportId=abc&access_token=secret";

        assertEquals(url, CePowerBiConfigService.validateAndNormalizeUrl(url));
    }

    @Test
    void rejectsNonPowerBiOrNonHttpsUrls() {
        assertThrows(ServiceException.class,
            () -> CePowerBiConfigService.validateAndNormalizeUrl("http://app.powerbi.com/view?r=abc"));
        assertThrows(ServiceException.class,
            () -> CePowerBiConfigService.validateAndNormalizeUrl("https://example.com/reportEmbed?token=abc"));
    }
}
