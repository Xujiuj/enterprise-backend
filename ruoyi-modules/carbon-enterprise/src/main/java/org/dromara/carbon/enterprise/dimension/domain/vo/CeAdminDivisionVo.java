package org.dromara.carbon.enterprise.dimension.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.dimension.domain.CeAdminDivision;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 行政区划视图对象.
 */
@Data
@AutoMapper(target = CeAdminDivision.class)
public class CeAdminDivisionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String divisionCode;

    private String divisionName;

    private String parentCode;

    private String levelType;

    private Integer sortOrder;

    private String status;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
