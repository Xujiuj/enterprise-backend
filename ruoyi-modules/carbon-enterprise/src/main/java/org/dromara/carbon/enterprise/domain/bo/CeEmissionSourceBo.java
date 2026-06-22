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

    private Integer rowNo;

    @NotBlank(message = "companyCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String companyCode;

    private String companyName;

    private String factoryName;

    @NotBlank(message = "sourceCategoryKey cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    @NotBlank(message = "sourceIdentificationCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String sourceIdentificationCode;

    private String sourceIdentificationName;

    private String emissionSourceName;

    private String responsibleDept;

    private String dataSource;

    private String factorKey;

    private Boolean enabledFlag;

    private String remark;
}
