package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local intensity denominator fact.
 */
@Data
@TableName("ce_intensity_denominator_fact")
public class CeIntensityDenominatorFact implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
