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

    @NotNull(message = "emissionSourceId cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Long emissionSourceId;

    @NotBlank(message = "activityPeriod cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String activityPeriod;

    @NotNull(message = "activityValue cannot be null", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal activityValue;

    @NotBlank(message = "activityUnit cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String activityUnit;

    private Long factorConfirmationId;

    private BigDecimal calculatedEmission;

    private String dataStatus;

    private String remark;
}
