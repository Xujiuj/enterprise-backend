package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeActivityData;
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

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    private Long batchId;

    private String sourceSheetCode;

    @NotBlank(message = "sourceIdentificationCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String sourceIdentificationCode;

    @NotBlank(message = "companyCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String companyCode;

    private String companyName;

    private String factoryName;

    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    private String sourceIdentificationName;

    private String emissionSourceName;

    @NotBlank(message = "activityUnit cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String activityUnit;

    private Integer activityYear;

    private Integer activityMonth;

    private java.util.Date activityDate;

    @NotNull(message = "activityValue cannot be null", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal activityValue;

    private String responsibleDept;

    private String dataSource;

    private String sourceRemark;

    private String factorKey;

    private BigDecimal calculatedEmission;

    private String dataStatus;

    private String remark;
}
