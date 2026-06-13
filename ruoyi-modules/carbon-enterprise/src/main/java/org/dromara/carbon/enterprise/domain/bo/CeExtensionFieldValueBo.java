package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeExtensionFieldValue;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise extension field value business object.
 */
@Data
@AutoMapper(target = CeExtensionFieldValue.class, reverseConvertGenerate = false)
public class CeExtensionFieldValueBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "ownerTableCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String ownerTableCode;

    @NotNull(message = "ownerRecordId cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Long ownerRecordId;

    @NotNull(message = "extensionFieldId cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Long extensionFieldId;

    private String textValue;

    private BigDecimal decimalValue;

    private Date dateValue;

    private Boolean booleanValue;
}
