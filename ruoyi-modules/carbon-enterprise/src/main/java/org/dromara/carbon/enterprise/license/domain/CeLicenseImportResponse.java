package org.dromara.carbon.enterprise.license.domain;

import lombok.Data;

/**
 * Enterprise-side license import response.
 */
@Data
public class CeLicenseImportResponse {

    private final boolean valid;

    private final String status;

    private final String message;

    private final String syncMessage;

    private final CeLicenseImportStateResponse licenseState;

    public static CeLicenseImportResponse from(CeLicenseImportResult result) {
        String status = result.getStatus();
        return new CeLicenseImportResponse(result.isValid(), status, messageForStatus(status), result.getSyncMessage(),
            CeLicenseImportStateResponse.from(result.getLicenseState()));
    }

    public static CeLicenseImportResponse publicKeyUnavailable() {
        return new CeLicenseImportResponse(false, "PUBLIC_KEY_UNAVAILABLE", messageForStatus("PUBLIC_KEY_UNAVAILABLE"),
            null, null);
    }

    private static String messageForStatus(String status) {
        return switch (status) {
            case "VALID" -> "授权文件校验通过";
            case "MALFORMED_LICENSE" -> "授权文件内容格式不正确";
            case "UNSUPPORTED_SCHEMA" -> "授权文件版本不受支持";
            case "UNSUPPORTED_ALGORITHM" -> "授权文件签名算法不受支持";
            case "SIGNATURE_INVALID" -> "授权文件签名校验失败";
            case "KEY_ID_MISMATCH" -> "授权文件签名密钥不一致";
            case "NOT_YET_VALID" -> "授权尚未到生效时间";
            case "EXPIRED" -> "授权已过期";
            case "INSTALL_ID_MISMATCH" -> "授权文件的部署指纹与本机不匹配";
            case "CLOCK_ROLLBACK" -> "系统时间早于最近授权校验时间";
            case "LICENSE_BINDING_FAILED" -> "厂商授权绑定确认失败";
            case "VENDOR_LICENSE_NOT_ACTIVE" -> "厂商授权当前不可用";
            case "PUBLIC_KEY_UNAVAILABLE" -> "企业端授权公钥不可用";
            default -> "授权导入失败";
        };
    }
}
