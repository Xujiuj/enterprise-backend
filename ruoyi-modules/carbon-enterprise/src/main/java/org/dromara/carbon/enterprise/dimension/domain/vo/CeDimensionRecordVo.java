package org.dromara.carbon.enterprise.dimension.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise dimension record view object.
 */
@Data
public class CeDimensionRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String dimensionCode;

    private String recordCode;

    private String recordName;

    private String parentCode;

    private String companySk;

    private String factoryName;

    private String provinceCode;

    private String provinceName;

    private String factoryType;

    private String industrySectionCode;

    private String industrySectionName;

    private String industryDivisionCode;

    private String industryDivisionName;

    private String industryGroupCode;

    private String industryGroupName;

    private String industryClassCode;

    private String industryClassName;

    private String effectiveDate;

    private String expiryDate;

    private String activeFlag;

    private String categorySk;

    private String businessKey;

    private String ghgScope;

    private String ghgScopeCategorySort;

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

    private String currentFlag;

    private String versionNo;

    private String unifiedStandardCategory;

    private String baseYear;

    private String currentBaseFlag;

    private String emissionSourceNameEn;

    private String fuelMaterialCategory;

    private String sourceUnit;

    private String co2;

    private String ch4;

    private String n2o;

    private String hfcs;

    private String pfcs;

    private String sf6;

    private String nf3;

    private String applicableScope;

    private String factorSource;

    private String gwpCh4;

    private String gwpN2o;

    private String gwpHfcs;

    private String gwpPfcs;

    private String gwpSf6;

    private String gwpNf3;

    private String factorGwp;

    private String factorUnit;

    private String factorVersion;

    private String divisionCode;

    private String divisionName;

    private String regionName;

    private String provinceFactor;

    private String regionFactor;

    private String nationalFactor;

    private String nonFossilExcludedFactor;

    private String nationalFossilPowerFactor;

    private String effectiveYear;

    private String scopeKey;

    private String scopeName;

    private String gasNameEn;

    private String denominatorType;

    private String denominatorMetricName;

    private String intensityUnitDisplay;

    private String enabledText;

    private String targetYear;

    private String targetValue;

    private String unitName;

    private String factYear;

    private String factMonth;

    private String denominatorValue;

    private String dataSource;

    private String industrySection;

    private String toleranceRate;

    private String templateType;

    private String filePath;

    private String publishedAt;

    private String sortOrder;

    private String status;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
