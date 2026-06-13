package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local emission source view object.
 */
@Data
@AutoMapper(target = CeEmissionSource.class)
public class CeEmissionSourceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String sourceCode;

    private String sourceName;

    private String sourceCategoryCode;

    private String sourceCategoryName;

    private String facilityName;

    private String boundaryScope;

    private Boolean enabledFlag;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
