package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local factor cache version.
 */
@Data
@TableName("ce_factor_cache_version")
public class CeFactorCacheVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String vendorVersionId;

    private String licenseId;

    private String versionCode;

    private Boolean frozenFlag;

    private Date syncedTime;
}
