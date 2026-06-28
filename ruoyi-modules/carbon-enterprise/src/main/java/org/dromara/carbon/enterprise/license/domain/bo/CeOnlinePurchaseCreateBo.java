package org.dromara.carbon.enterprise.license.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Enterprise online purchase request forwarded to vendor open API.
 */
@Data
public class CeOnlinePurchaseCreateBo {

    @NotBlank(message = "授权套餐不能为空")
    private String packageId;

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String customerCode;

    private String contactName;

    private String contactEmail;

    private String contactPhone;

    private String licenseId;

    private String installId;

    private String idempotencyKey;

    private String returnUrl;
}
