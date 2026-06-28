package org.dromara.carbon.enterprise.dimension.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise dimension record business object.
 */
@Data
public class CeDimensionRecordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "dimensionCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String dimensionCode;

    @NotBlank(message = "recordCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String recordCode;

    @NotBlank(message = "recordName cannot be blank", groups = { AddGroup.class, EditGroup.class })
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

    private String levelType;

    private String categoryNameEn;

    private String baseYear;

    private String baseYearKey;

    private String description;

    private String isCurrent;

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

    private String effectiveYear;

    private String gwpValue;

    private String gwpVersion;

    private String chemicalFormula;

    private String denominatorType;

    private String denominatorMetricName;

    private String intensityUnitDisplay;

    private String enabledText;

    private String targetValue;

    private String unitName;

    private String factoryTypeForFact;

    private String factYear;

    private String factMonth;

    private String denominatorValue;

    private String dataSource;

    private String toleranceRate;

    private String sortOrder;

    private String status;

    private String remark;
}
