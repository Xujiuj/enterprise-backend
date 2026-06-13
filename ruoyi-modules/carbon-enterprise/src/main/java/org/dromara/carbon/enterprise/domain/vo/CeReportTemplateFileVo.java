package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local report template download catalog view object.
 */
@Data
@AutoMapper(target = CeReportTemplateFile.class)
public class CeReportTemplateFileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String templateCode;

    private String templateName;

    private String templateType;

    private String fileName;

    private String filePath;

    private Boolean enabledFlag;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
