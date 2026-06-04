package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeCaptureRow;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local data capture row view object.
 */
@Data
@AutoMapper(target = CeCaptureRow.class)
public class CeCaptureRowVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long batchId;

    private Long sheetId;

    private Integer sourceRowNo;

    private String rowStatus;

    private String validationLevel;

    private Date createTime;

    private Date updateTime;
}
