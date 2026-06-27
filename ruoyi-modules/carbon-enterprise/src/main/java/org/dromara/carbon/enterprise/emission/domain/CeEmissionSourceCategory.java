package org.dromara.carbon.enterprise.emission.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 排放源分类维度实体.
 */
@Data
@TableName("ce_emission_source_category")
public class CeEmissionSourceCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 分类代理键 */
    private String categorySk;

    /** 业务编码 */
    private String businessKey;

    /** GHG范围 */
    private String ghgScope;

    /** GHG范围分类排序 */
    private Integer ghgScopeCategorySort;

    /** GHG范围分类 */
    private String ghgScopeCategory;

    /** GHG范围（英文） */
    private String ghgScopeEn;

    /** GHG范围分类（英文） */
    private String ghgScopeCategoryEn;

    /** ISO分类 */
    private String isoCategory;

    /** ISO分类（英文） */
    private String isoCategoryEn;

    /** ISO分类描述 */
    private String isoCategoryDescription;

    /** ISO分类描述（英文） */
    private String isoCategoryDescriptionEn;

    /** ISO自定义子分类 */
    private String isoCustomSubcategory;

    /** 国标范围分类 */
    private String gbScopeCategory;

    /** 国标子分类 */
    private String gbSubcategory;

    /** 生效日期 */
    private String effectiveDate;

    /** 失效日期 */
    private String expiryDate;

    /** 是否当前版本 Y是 N否 */
    private String isCurrent;

    /** 版本号 */
    private String versionNo;

    /** 统一标准分类 */
    private String unifiedStandardCategory;

    /** 备注 */
    private String remark;
}
