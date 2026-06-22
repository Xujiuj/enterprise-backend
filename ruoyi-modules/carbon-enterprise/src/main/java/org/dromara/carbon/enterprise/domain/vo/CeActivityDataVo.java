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

    private String sourceSheetCode;

    private String sourceIdentificationCode;

    private String companyCode;

    private String companyName;

    private String factoryName;

    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    private String sourceIdentificationName;

    private String emissionSourceName;

    private String activityUnit;

    private Integer activityYear;

    private Integer activityMonth;

    private Date activityDate;

    private BigDecimal activityValue;

    private String responsibleDept;

    private String dataSource;

    private String sourceRemark;

    private String factorKey;

    private BigDecimal calculatedEmission;

    private String dataStatus;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
