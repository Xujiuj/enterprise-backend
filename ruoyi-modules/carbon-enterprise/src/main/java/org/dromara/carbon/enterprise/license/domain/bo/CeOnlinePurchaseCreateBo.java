package org.dromara.carbon.enterprise.license.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Enterprise online purchase request forwarded to vendor open API.
 */
@Data
public class CeOnlinePurchaseCreateBo {

    @NotBlank(message = "packageId cannot be blank")
    private String packageId;

    @NotBlank(message = "payChannel cannot be blank")
    private String payChannel;

    @NotBlank(message = "customerName cannot be blank")
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
