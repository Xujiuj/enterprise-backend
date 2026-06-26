package org.dromara.carbon.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Activity data status update request.
 */
@Data
public class CeActivityDataStatusBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "请选择要提交的数据")
    private List<Long> ids;

    @NotBlank(message = "数据状态不能为空")
    private String dataStatus;
}
