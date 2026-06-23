package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise company and factory relation.
 */
@Data
@TableName("ce_company_factory")
public class CeCompanyFactory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String companyCode;

    private String factoryCode;

    private String companyName;

    private String factoryName;

    private String isActive;
}
