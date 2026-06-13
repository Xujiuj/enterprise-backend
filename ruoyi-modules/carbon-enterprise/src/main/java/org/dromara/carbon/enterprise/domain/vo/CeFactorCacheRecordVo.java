package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local factor cache record view object.
 */
@Data
@AutoMapper(target = CeFactorCacheRecord.class)
public class CeFactorCacheRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
