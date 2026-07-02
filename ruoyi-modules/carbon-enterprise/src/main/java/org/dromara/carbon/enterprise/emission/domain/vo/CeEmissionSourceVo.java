package org.dromara.carbon.enterprise.emission.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;

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

    private String companyCode;

    private String companyName;

    private String factoryName;

    private String sourceCategoryKey;

    private String scopeName;

    private String scopeSubcategory;

    private String sourceIdentificationCode;

    private String sourceIdentificationName;

    private String emissionSourceName;

    private String responsibleDept;

    private String dataFrequency;

    private Long responsibleUserId;

    private String responsibleUserName;

    private String dataSource;

    private String factorKey;

    private Boolean enabledFlag;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
