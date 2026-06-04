package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeExtensionField;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise allowed extension fields business object.
 */
@Data
@AutoMapper(target = CeExtensionField.class, reverseConvertGenerate = false)
public class CeExtensionFieldBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotNull(message = "templateVersionId cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Long templateVersionId;

    @NotBlank(message = "moduleCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String moduleCode;

    @NotNull(message = "sheetId cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Long sheetId;

    @NotBlank(message = "fieldCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String fieldCode;

    @NotBlank(message = "fieldName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String fieldName;

    private String valueType;

    private Boolean enabledFlag;
}
