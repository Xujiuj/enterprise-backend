package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise allowed extension fields.
 */
@Data
@TableName("ce_extension_field")
public class CeExtensionField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long templateVersionId;

    private String moduleCode;

    private Long sheetId;

    private String fieldCode;

    private String fieldName;

    private String valueType;

    private Boolean enabledFlag;

    private Date createTime;
}
