package org.dromara.carbon.enterprise.vendor.client;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("dev")
class HttpCeVendorReportTemplateOpenClientTest {

    private HttpCeVendorReportTemplateOpenClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        CeVendorOpenApiHttpSupport httpSupport = new CeVendorOpenApiHttpSupport();
        client = new HttpCeVendorReportTemplateOpenClient(httpSupport);
        setField("vendorOpenBaseUrl", "http://vendor.test");
        server = MockRestServiceServer.bindTo(httpSupport.restTemplate()).build();
    }

    @Test
    void downloadsBinaryTemplateFile() {
        byte[] content = new byte[] {'P', 'K', 3, 4};
        server.expect(requestTo("http://vendor.test/open/report-templates/download-tokens/TOKEN-001"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(content, MediaType.APPLICATION_OCTET_STREAM));

        assertArrayEquals(content, client.downloadTemplateFile("TOKEN-001"));
        server.verify();
    }

    @Test
    void rejectsJsonErrorBodyFromVendorTokenEndpoint() {
        server.expect(requestTo("http://vendor.test/open/report-templates/download-tokens/TOKEN-001"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"code\":500,\"msg\":\"report template file does not exist\",\"data\":null}",
                MediaType.APPLICATION_JSON));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> client.downloadTemplateFile("TOKEN-001"));

        assertEquals("厂商报表模板文件不存在", exception.getMessage());
        server.verify();
    }

    @Test
    void sendsLicenseAsBearerTokenWhenListingTemplates() {
        server.expect(requestTo("http://vendor.test/open/report-templates?licenseId=LIC-001&installId=INSTALL-001"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer LIC-001"))
            .andRespond(withSuccess("{\"code\":200,\"msg\":\"ok\",\"data\":{\"licenseId\":\"LIC-001\",\"installId\":\"INSTALL-001\",\"templates\":[]}}",
                MediaType.APPLICATION_JSON));

        client.listTemplates("LIC-001", "INSTALL-001");

        server.verify();
    }

    @Test
    void reportsStableMessageWhenVendorReturnsNoTemplateData() {
        server.expect(requestTo("http://vendor.test/open/report-templates?licenseId=LIC-001&installId=INSTALL-001"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer LIC-001"))
            .andRespond(withSuccess("{\"code\":200,\"msg\":\"ok\",\"data\":null}",
                MediaType.APPLICATION_JSON));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> client.listTemplates("LIC-001", "INSTALL-001"));

        assertEquals("厂商模板列表查询失败", exception.getMessage());
        server.verify();
    }

    private void setField(String name, Object value) throws Exception {
        Field field = HttpCeVendorReportTemplateOpenClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(client, value);
    }
}
