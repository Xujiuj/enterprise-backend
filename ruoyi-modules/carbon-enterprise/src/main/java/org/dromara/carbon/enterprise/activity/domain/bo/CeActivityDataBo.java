package org.dromara.carbon.enterprise.activity.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Enterprise local activity data business object.
 */
@Data
@AutoMapper(target = CeActivityData.class, reverseConvertGenerate = false)
public class CeActivityDataBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = { EditGroup.class })
    private Long id;

    private Long batchId;

    private String sourceSheetCode;

    @NotBlank(message = "排放源识别编号不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sourceIdentificationCode;

    @NotBlank(message = "公司编号不能为空", groups = { AddGroup.class, EditGroup.class })
    private String companyCode;

    private String companyName;

    private String factoryCode;

    private String factoryName;

    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    private String sourceIdentificationName;

    private String emissionSourceName;

    private String activityUnit;

    private Integer activityYear;

    private Integer activityMonth;

    private java.util.Date activityDate;

    @NotNull(message = "活动数据不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal activityValue;

    private String responsibleDept;

    private String dataSource;

    private String sourceRemark;

    private String factorKey;

    private BigDecimal calculatedEmission;

    private String dataStatus;

    private String remark;
}
