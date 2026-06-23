package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeEmissionSourceCategory;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise emission source category view object.
 */
@Data
@AutoMapper(target = CeEmissionSourceCategory.class)
public class CeEmissionSourceCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String categorySk;

    private String businessKey;

    private String ghgScope;

    private String ghgScopeCategory;

    private String isCurrent;
}
