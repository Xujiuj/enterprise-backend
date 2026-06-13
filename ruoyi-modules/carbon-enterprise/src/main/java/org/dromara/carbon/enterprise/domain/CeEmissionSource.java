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
