package org.dromara.carbon.enterprise.activity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local data capture row.
 */
@Data
@TableName("ce_capture_row")
public class CeCaptureRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Long sheetId;

    private Integer sourceRowNo;

    private String rowStatus;

    private String validationLevel;

    private Date createTime;

    private Date updateTime;
}
