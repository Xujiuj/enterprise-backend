package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeIntensityMetric;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Enterprise local carbon intensity metric business object.
 */
@Data
@AutoMapper(target = CeIntensityMetric.class, reverseConvertGenerate = false)
public class CeIntensityMetricBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "metricCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String metricCode;

    @NotBlank(message = "metricName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String metricName;

    @NotBlank(message = "metricPeriod cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String metricPeriod;

    private BigDecimal numeratorEmission;

    private BigDecimal denominatorValue;

    @NotBlank(message = "denominatorUnit cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String denominatorUnit;

    private BigDecimal intensityValue;

    private String metricStatus;

    private String remark;
}
