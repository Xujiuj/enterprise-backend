package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local factor cache record.
 */
@Data
@TableName("ce_factor_cache_record")
public class CeFactorCacheRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long cacheVersionId;

    private String factorCode;

    private String factorName;

    private String factorCategory;

    private BigDecimal factorValue;

    private String factorUnit;

    private String sourceRef;

    private Boolean enabledFlag;

    private Date syncedTime;
}
