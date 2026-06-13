package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local carbon intensity metric.
 */
@Data
@TableName("ce_intensity_metric")
public class CeIntensityMetric implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String metricCode;

    private String metricName;

    private String metricPeriod;

    private BigDecimal numeratorEmission;

    private BigDecimal denominatorValue;

    private String denominatorUnit;

    private BigDecimal intensityValue;

    private String metricStatus;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
