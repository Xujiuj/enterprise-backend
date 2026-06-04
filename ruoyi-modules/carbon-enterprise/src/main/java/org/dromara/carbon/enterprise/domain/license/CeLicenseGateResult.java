package org.dromara.carbon.enterprise.domain.license;

import lombok.Data;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;

/**
 * Runtime decision for enterprise license gate consumers.
 */
@Data
public class CeLicenseGateResult {

    private final String decision;

    private final String reason;

    private final CeLicenseStateVo licenseState;
}
