package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise emission source category.
 */
@Data
@TableName("ce_emission_source_category")
public class CeEmissionSourceCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String categorySk;

    private String businessKey;

    private String ghgScope;

    private String ghgScopeCategory;

    private String isCurrent;
}
