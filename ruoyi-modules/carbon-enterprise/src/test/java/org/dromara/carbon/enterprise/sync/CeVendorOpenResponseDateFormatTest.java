package org.dromara.carbon.enterprise.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.domain.sync.CeVendorAnnouncementListResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorFactorSyncResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateDownloadResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorReportTemplateListResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class CeVendorOpenResponseDateFormatTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesVendorOpenFactorTimeFormat() throws Exception {
        CeVendorFactorSyncResponse response = objectMapper.readValue(
            """
                {
                  "licenseId": "LIC-001",
                  "vendorVersionId": "1",
                  "versionCode": "FV-2026",
                  "publishedTime": "2026-06-11 01:59:05",
                  "changed": true,
                  "records": []
                }
                """,
            CeVendorFactorSyncResponse.class
        );

        assertNotNull(response.getPublishedTime());
        assertEquals("FV-2026", response.getVersionCode());
    }

    @Test
    void parsesVendorOpenReportTemplateTimeFormat() throws Exception {
        CeVendorReportTemplateListResponse response = objectMapper.readValue(
            """
                {
                  "licenseId": "LIC-001",
                  "templates": [
                    {
                      "templateId": 1,
                      "templateCode": "carbon-report-standard",
                      "templateVersion": "2026.1",
                      "publishedTime": "2026-06-11 01:59:05"
                    }
                  ]
                }
                """,
            CeVendorReportTemplateListResponse.class
        );

        assertEquals(1, response.getTemplates().size());
        assertNotNull(response.getTemplates().get(0).getPublishedTime());
    }

    @Test
    void parsesVendorOpenReportTemplateDownloadTimeFormat() throws Exception {
        CeVendorReportTemplateDownloadResponse response = objectMapper.readValue(
            """
                {
                  "licenseId": "LIC-001",
                  "templateId": 1,
                  "templateCode": "carbon-report-standard",
                  "templateVersion": "2026.1",
                  "downloadTokenExpiresTime": "2026-06-11 02:09:05",
                  "publishedTime": "2026-06-11 01:59:05"
                }
                """,
            CeVendorReportTemplateDownloadResponse.class
        );

        assertNotNull(response.getDownloadTokenExpiresTime());
        assertNotNull(response.getPublishedTime());
    }

    @Test
    void parsesVendorOpenAnnouncementTimeFormat() throws Exception {
        CeVendorAnnouncementListResponse response = objectMapper.readValue(
            """
                {
                  "licenseId": "LIC-001",
                  "announcements": [
                    {
                      "noticeId": 1,
                      "noticeTitle": "系统维护通知",
                      "noticeType": "1",
                      "createTime": "2026-06-10 22:49:20"
                    }
                  ]
                }
                """,
            CeVendorAnnouncementListResponse.class
        );

        assertEquals(1, response.getAnnouncements().size());
        assertNotNull(response.getAnnouncements().get(0).getCreateTime());
    }
}
