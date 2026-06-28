package org.dromara.carbon.enterprise.license.domain;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Enterprise-side license import request.
 */
@Data
public class CeLicenseImportRequest {

    @NotBlank(message = "授权文件内容不能为空")
    private String licenseContent;

    @NotBlank(message = "部署指纹不能为空")
    private String expectedInstallId;

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("授权导入请求包含不支持的字段：" + fieldName);
    }
}
