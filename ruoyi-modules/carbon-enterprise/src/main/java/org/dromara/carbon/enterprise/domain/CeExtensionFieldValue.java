package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise extension field value for allowed local forms.
 */
@Data
@TableName("ce_extension_field_value")
public class CeExtensionFieldValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String ownerTableCode;

    private Long ownerRecordId;

    private Long extensionFieldId;

    private String textValue;

    private BigDecimal decimalValue;

    private Date dateValue;

    private Boolean booleanValue;

    private Date createTime;

    private Date updateTime;
}
