package org.dromara.carbon.enterprise.dimension.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise dimension record business object.
 */
@Data
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

    private String field07;

    private String field08;

    private String field09;

    private String field10;

    private String field11;

    private String field12;

    private String field13;

    private String field14;

    private String field15;

    private String field16;

    private String field17;

    private String field18;

    private String field19;

    private String field20;

    private String field21;

    private String field22;

    private String sortOrder;

    private String status;

    private String remark;
}
