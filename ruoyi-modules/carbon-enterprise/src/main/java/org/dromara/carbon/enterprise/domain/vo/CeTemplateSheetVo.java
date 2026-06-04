package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeTemplateSheet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise source workbook sheet inventory view object.
 */
@Data
@AutoMapper(target = CeTemplateSheet.class)
public class CeTemplateSheetVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long templateVersionId;

    private String sourceFile;

    private String sourceGroup;

    private String sheetName;

    private String sheetType;

    private Integer headerRow;

    private Integer fieldCount;

    private String moduleCode;

    private String targetTableCode;

    private Boolean allowExtension;

    private Date createTime;
}
