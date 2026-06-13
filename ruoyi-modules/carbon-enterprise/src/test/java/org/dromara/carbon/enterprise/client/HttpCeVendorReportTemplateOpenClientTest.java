package org.dromara.carbon.enterprise.client;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("dev")
class HttpCeVendorReportTemplateOpenClientTest {

    private HttpCeVendorReportTemplateOpenClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        client = new HttpCeVendorReportTemplateOpenClient();
        setField("vendorOpenBaseUrl", "http://vendor.test");
        RestTemplate restTemplate = (RestTemplate) getField("restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
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

        assertEquals("report template file does not exist", exception.getMessage());
        server.verify();
    }

    private void setField(String name, Object value) throws Exception {
        Field field = HttpCeVendorReportTemplateOpenClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(client, value);
    }

    private Object getField(String name) throws Exception {
        Field field = HttpCeVendorReportTemplateOpenClient.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(client);
    }
}
