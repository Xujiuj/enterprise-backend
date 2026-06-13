package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local activity data.
 */
@Data
@TableName("ce_activity_data")
public class CeActivityData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
