package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeCaptureBatch;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local data capture batch view object.
 */
@Data
@AutoMapper(target = CeCaptureBatch.class)
public class CeCaptureBatchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
