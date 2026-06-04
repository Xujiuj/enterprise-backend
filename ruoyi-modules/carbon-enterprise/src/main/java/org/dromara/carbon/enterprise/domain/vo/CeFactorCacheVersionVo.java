package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeFactorCacheVersion;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local factor cache version view object.
 */
@Data
@AutoMapper(target = CeFactorCacheVersion.class)
public class CeFactorCacheVersionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String vendorVersionId;

    private String licenseId;

    private String versionCode;

    private Boolean frozenFlag;

    private Date syncedTime;
}
