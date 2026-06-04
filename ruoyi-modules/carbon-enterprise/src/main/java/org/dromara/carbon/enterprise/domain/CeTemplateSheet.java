package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise source workbook sheet inventory.
 */
@Data
@TableName("ce_template_sheet")
public class CeTemplateSheet implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
