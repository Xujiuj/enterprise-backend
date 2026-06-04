package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;

import java.util.List;

/**
 * Signed license.v1 payload.
 */
@Data
public class CeLicensePayload {

    private String licenseId;

    private String customerId;

    private String customerName;

    private String edition;

    private List<String> features;

    private String installId;

    private String validFrom;

    private String validTo;

    private String issuedAt;

    private String issuer;

    private String keyId;

    private List<CeTemplateEntitlement> templateEntitlements;
}
