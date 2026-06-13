package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeIntensityMetric;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local carbon intensity metric view object.
 */
@Data
@AutoMapper(target = CeIntensityMetric.class)
public class CeIntensityMetricVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
