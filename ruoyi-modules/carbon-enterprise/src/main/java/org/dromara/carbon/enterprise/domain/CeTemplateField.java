package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise original field preservation inventory.
 */
@Data
@TableName("ce_template_field")
public class CeTemplateField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long sheetId;

    private Integer fieldOrder;

    private String originalFieldName;

    private String targetColumnCode;

    private String valueType;

    private Boolean requiredFlag;

    private Boolean originalFieldFlag;

    private Boolean extensibleFlag;

    private Date createTime;
}
