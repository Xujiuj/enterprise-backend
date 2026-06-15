package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeDimensionRecord;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise dimension record business object.
 */
@Data
@AutoMapper(target = CeDimensionRecord.class, reverseConvertGenerate = false)
public class CeDimensionRecordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "dimensionCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String dimensionCode;

    @NotBlank(message = "recordCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String recordCode;

    @NotBlank(message = "recordName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String recordName;

    private String parentCode;

    private String field01;

    private String field02;

    private String field03;

    private String field04;

    private String field05;

    private String field06;

    private Integer sortOrder;

    private String status;

    private String remark;
}
