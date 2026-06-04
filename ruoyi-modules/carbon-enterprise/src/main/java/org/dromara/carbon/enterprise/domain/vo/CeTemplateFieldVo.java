package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeTemplateField;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise original field preservation inventory view object.
 */
@Data
@AutoMapper(target = CeTemplateField.class)
public class CeTemplateFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sheetId;

    private Integer fieldOrder;

    private String originalFieldName;

    private String targetColumnCode;

    private String valueType;

    private Boolean requiredFlag;

    private Boolean originalFieldFlag;

    private Boolean extensibleFlag;

    private Date createTime;
}
