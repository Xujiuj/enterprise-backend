package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local report template download catalog.
 */
@Data
@TableName("ce_report_template_file")
public class CeReportTemplateFile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
