package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeTemplateVersion;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise Excel template version view object.
 */
@Data
@AutoMapper(target = CeTemplateVersion.class)
public class CeTemplateVersionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
