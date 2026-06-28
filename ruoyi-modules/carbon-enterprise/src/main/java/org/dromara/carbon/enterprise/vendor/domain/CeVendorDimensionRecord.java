package org.dromara.carbon.enterprise.vendor.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * Vendor dimension record exposed by vendor open APIs.
 */
@Data
public class CeVendorDimensionRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String dimensionCode;
    private String recordCode;
    private String recordName;
    private String parentCode;
    private Integer sortOrder;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private String remark;

    private String levelType;

    private String categorySk;
    private String businessKey;
    private String categoryNameEn;
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
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String currentFlag;
    private String versionNo;
    private String standardCategory;

    private String baseYearKey;
    private String description;
    private Integer baseYear;
    private Integer isCurrent;

    private String factorVersion;
    private String divisionCode;
    private String divisionName;
    private String regionName;
    private BigDecimal provinceFactor;
    private BigDecimal regionFactor;
    private BigDecimal nationalFactor;
    private BigDecimal nonFossilExcludedFactor;
    private BigDecimal nationalFossilPowerFactor;

    private Integer effectiveYear;

    private String scopeKey;
    private String scopeName;

    private String gasCode;
    private String gasName;
    private String gasNameEn;
    private BigDecimal gwpValue;
    private String gwpVersion;
    private String chemicalFormula;
}
