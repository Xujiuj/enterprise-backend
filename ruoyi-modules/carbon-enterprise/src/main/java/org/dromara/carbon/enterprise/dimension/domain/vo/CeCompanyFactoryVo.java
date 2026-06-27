package org.dromara.carbon.enterprise.dimension.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.dimension.domain.CeCompanyFactory;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise company and factory relation view object.
 */
@Data
@AutoMapper(target = CeCompanyFactory.class)
public class CeCompanyFactoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String companyCode;

    private String factoryCode;

    private String companyName;

    private String factoryName;

    private String isActive;
}
