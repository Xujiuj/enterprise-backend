package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeCaptureCell;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local data capture cell view object.
 */
@Data
@AutoMapper(target = CeCaptureCell.class)
public class CeCaptureCellVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long rowId;

    private Long fieldId;

    private String textValue;

    private BigDecimal decimalValue;

    private Date dateValue;

    private String valueStatus;

    private Date createTime;

    private Date updateTime;
}
