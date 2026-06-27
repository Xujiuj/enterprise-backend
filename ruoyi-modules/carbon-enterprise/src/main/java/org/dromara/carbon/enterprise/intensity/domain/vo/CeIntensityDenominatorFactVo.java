package org.dromara.carbon.enterprise.intensity.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityDenominatorFact;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local intensity denominator fact view object.
 */
@Data
@AutoMapper(target = CeIntensityDenominatorFact.class)
public class CeIntensityDenominatorFactVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long batchId;

    private String sourceSheetCode;

    private String factoryCode;

    private String factoryName;

    private String factoryType;

    private Integer factYear;

    private Integer factMonth;

    private String denominatorType;

    private String denominatorMetricName;

    private BigDecimal denominatorValue;

    private String unitName;

    private String dataSource;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
