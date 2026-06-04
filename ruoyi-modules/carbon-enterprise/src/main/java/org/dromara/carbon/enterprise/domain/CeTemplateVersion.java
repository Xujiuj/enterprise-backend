package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise Excel template version.
 */
@Data
@TableName("ce_template_version")
public class CeTemplateVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String versionCode;

    private String versionName;

    private String sourceDir;

    private Integer workbookCount;

    private Integer sheetCount;

    private Integer fieldCount;

    private String status;

    private String importedBy;

    private Date importedTime;

    private String remark;
}
