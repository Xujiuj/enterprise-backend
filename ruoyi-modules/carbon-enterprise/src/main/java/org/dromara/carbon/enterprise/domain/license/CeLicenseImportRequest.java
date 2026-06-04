package org.dromara.carbon.enterprise.domain.license;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Enterprise-side license import request.
 */
@Data
public class CeLicenseImportRequest {

    @NotBlank(message = "licenseContent cannot be blank")
    private String licenseContent;

    @NotBlank(message = "expectedInstallId cannot be blank")
    private String expectedInstallId;

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("unsupported license import request field: " + fieldName);
    }
}
