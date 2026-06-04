package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local data capture cell.
 */
@Data
@TableName("ce_capture_cell")
public class CeCaptureCell implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
