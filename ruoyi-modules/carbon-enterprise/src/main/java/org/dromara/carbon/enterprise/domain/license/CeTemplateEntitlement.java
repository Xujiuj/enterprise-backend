package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;

/**
 * Report template entitlement included in a license payload.
 */
@Data
public class CeTemplateEntitlement {

    private String templateCode;

    private String templateVersion;

    private String scope;
}
