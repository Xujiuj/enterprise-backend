package org.dromara.carbon.enterprise.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise-local report content catalog business object.
 */
@Data
public class CeReportContentBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = { EditGroup.class })
    private Long id;

    @NotNull(message = "目录序号不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer directoryNo;

    @NotBlank(message = "目录名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String directoryName;

    @NotNull(message = "子目录序号不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer subdirectoryNo;

    @NotBlank(message = "子目录名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String subdirectoryName;

    private Integer displayOrder;

    private String remark;
}
