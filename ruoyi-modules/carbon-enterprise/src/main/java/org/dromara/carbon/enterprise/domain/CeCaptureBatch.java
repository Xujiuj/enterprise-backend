package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local data capture batch.
 */
@Data
@TableName("ce_capture_batch")
public class CeCaptureBatch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long templateVersionId;

    private String moduleCode;

    private String sourceMode;

    private String batchStatus;

    private String validationStatus;

    private String submittedBy;

    private Date submittedTime;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
