package org.dromara.carbon.enterprise.vendor.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorAnnouncementListResponse;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionListResponse;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorFactorSyncResponse;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorLicenseCurrentResponse;
import org.dromara.carbon.enterprise.report.domain.CeVendorReportTemplateDownloadResponse;
import org.dromara.carbon.enterprise.report.domain.CeVendorReportTemplateListResponse;
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
    void parsesVendorOpenLicenseCurrentTimeFormat() throws Exception {
        CeVendorLicenseCurrentResponse response = objectMapper.readValue(
            """
                {
                  "licenseId": "LIC-001",
                  "status": "active",
                  "validFrom": "2026-07-01 00:00:00",
                  "validTo": "2027-07-01 00:00:00"
                }
                """,
            CeVendorLicenseCurrentResponse.class
        );

        assertNotNull(response.getValidFrom());
        assertNotNull(response.getValidTo());
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

    @Test
    void parsesVendorOpenDimensionRecordTimeFormat() throws Exception {
        CeVendorDimensionListResponse response = objectMapper.readValue(
            """
                {
                  "licenseId": "LIC-001",
                  "dimensionCode": "emission-source-category",
                  "total": 1,
                  "records": [
                    {
                      "id": 1,
                      "dimensionCode": "emission-source-category",
                      "recordCode": "stationary-combustion",
                      "recordName": "固定燃烧",
                      "createTime": "2026-06-13 17:22:17",
                      "updateTime": "2026-06-13 17:22:17"
                    }
                  ]
                }
                """,
            CeVendorDimensionListResponse.class
        );

        assertEquals(1, response.getRecords().size());
        assertNotNull(response.getRecords().get(0).getCreateTime());
        assertNotNull(response.getRecords().get(0).getUpdateTime());
    }
}
