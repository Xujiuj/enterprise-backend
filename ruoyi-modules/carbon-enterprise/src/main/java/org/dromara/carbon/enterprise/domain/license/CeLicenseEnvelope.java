package org.dromara.carbon.enterprise.domain.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Versioned license file envelope.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CeLicenseEnvelope {

    private String schemaVersion;

    private String algorithm;

    private String keyId;

    private JsonNode payload;

    private String signature;
}
