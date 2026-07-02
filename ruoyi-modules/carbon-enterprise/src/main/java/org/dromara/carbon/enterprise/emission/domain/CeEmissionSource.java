package org.dromara.carbon.enterprise.emission.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local emission source.
 */
@Data
@TableName("ce_emission_source")
public class CeEmissionSource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String companyCode;

    private String companyName;

    private String factoryCode;

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

    private String sourceUnit;

    private Boolean enabledFlag;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
