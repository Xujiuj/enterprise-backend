package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeExtensionField;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise allowed extension fields view object.
 */
@Data
@AutoMapper(target = CeExtensionField.class)
public class CeExtensionFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long templateVersionId;

    private String moduleCode;

    private Long sheetId;

    private String fieldCode;

    private String fieldName;

    private String valueType;

    private Boolean enabledFlag;

    private Date createTime;
}
