package org.dromara.carbon.enterprise.domain;

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

    @TableId(value = "id")
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

    private String dataSource;

    private String factorKey;

    private Boolean enabledFlag;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
