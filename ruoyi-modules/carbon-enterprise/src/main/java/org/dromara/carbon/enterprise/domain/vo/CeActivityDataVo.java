package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeActivityData;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local activity data view object.
 */
@Data
@AutoMapper(target = CeActivityData.class)
public class CeActivityDataVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long batchId;

    private Long emissionSourceId;

    private String activityPeriod;

    private BigDecimal activityValue;

    private String activityUnit;

    private Long factorConfirmationId;

    private BigDecimal calculatedEmission;

    private String dataStatus;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
