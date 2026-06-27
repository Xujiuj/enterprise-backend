package org.dromara.carbon.enterprise.dimension.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.dimension.domain.CeBaseYear;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 基准年视图对象.
 */
@Data
@AutoMapper(target = CeBaseYear.class)
public class CeBaseYearVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String factoryCode;

    private String factoryName;

    private Integer baseYear;

    private Integer enabledFlag;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
