package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeEmissionSourceCategory;

import java.io.Serial;
import java.io.Serializable;

/**
 * 排放源分类维度视图对象.
 */
@Data
@AutoMapper(target = CeEmissionSourceCategory.class)
public class CeEmissionSourceCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String categorySk;

    private String businessKey;

    private String ghgScope;

    private Integer ghgScopeCategorySort;

    private String ghgScopeCategory;

    private String ghgScopeEn;

    private String ghgScopeCategoryEn;

    private String isoCategory;

    private String isoCategoryEn;

    private String isoCategoryDescription;

    private String isoCategoryDescriptionEn;

    private String isoCustomSubcategory;

    private String gbScopeCategory;

    private String gbSubcategory;

    private String effectiveDate;

    private String expiryDate;

    private String isCurrent;

    private String versionNo;

    private String unifiedStandardCategory;

    private String remark;
}
