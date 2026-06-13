package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise local emission source business object.
 */
@Data
@AutoMapper(target = CeEmissionSource.class, reverseConvertGenerate = false)
public class CeEmissionSourceBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "sourceCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String sourceCode;

    @NotBlank(message = "sourceName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String sourceName;

    @NotBlank(message = "sourceCategoryCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String sourceCategoryCode;

    @NotBlank(message = "sourceCategoryName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String sourceCategoryName;

    private String facilityName;

    private String boundaryScope;

    private Boolean enabledFlag;

    private String remark;
}
