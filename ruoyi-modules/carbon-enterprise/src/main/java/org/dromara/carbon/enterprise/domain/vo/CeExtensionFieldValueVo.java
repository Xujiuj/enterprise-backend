package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeExtensionFieldValue;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise extension field value view object.
 */
@Data
@AutoMapper(target = CeExtensionFieldValue.class)
public class CeExtensionFieldValueVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
