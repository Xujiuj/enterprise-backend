package org.dromara.carbon.enterprise.report.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.report.domain.CeReportContent;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local report content catalog view object.
 */
@Data
@AutoMapper(target = CeReportContent.class)
public class CeReportContentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Integer directoryNo;

    private String directoryName;

    private Integer subdirectoryNo;

    private String subdirectoryName;

    private Integer displayOrder;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
