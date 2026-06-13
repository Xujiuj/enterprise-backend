package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise local report template download catalog business object.
 */
@Data
@AutoMapper(target = CeReportTemplateFile.class, reverseConvertGenerate = false)
public class CeReportTemplateFileBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "templateCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String templateCode;

    @NotBlank(message = "templateName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String templateName;

    @NotBlank(message = "templateType cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String templateType;

    @NotBlank(message = "fileName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String fileName;

    @NotBlank(message = "filePath cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String filePath;

    private Boolean enabledFlag;

    private String remark;
}
