package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local factor cache record business object.
 */
@Data
@AutoMapper(target = CeFactorCacheRecord.class, reverseConvertGenerate = false)
public class CeFactorCacheRecordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotNull(message = "cacheVersionId cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Long cacheVersionId;

    private String factorTableCode;

    @NotBlank(message = "factorCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorCode;

    @NotBlank(message = "factorName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorName;

    @NotBlank(message = "factorCategory cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorCategory;

    @NotNull(message = "factorValue cannot be null", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal factorValue;

    @NotBlank(message = "factorUnit cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorUnit;

    private String factorKey;

    private String emissionSourceName;

    private String emissionSourceNameEn;

    private String fuelMaterialCategory;

    private String sourceUnit;

    private BigDecimal co2;

    private BigDecimal ch4;

    private BigDecimal n2o;

    private BigDecimal hfcs;

    private BigDecimal pfcs;

    private BigDecimal sf6;

    private BigDecimal nf3;

    private String applicableScope;

    private String factorSource;

    private BigDecimal gwpCh4;

    private BigDecimal gwpN2o;

    private BigDecimal gwpHfcs;

    private BigDecimal gwpPfcs;

    private BigDecimal gwpSf6;

    private BigDecimal gwpNf3;

    private BigDecimal factorGwp;

    private String versionProvinceCode;

    private String factorVersion;

    private String divisionCode;

    private String divisionName;

    private String regionName;

    private BigDecimal provinceFactor;

    private BigDecimal regionFactor;

    private BigDecimal nationalFactor;

    private BigDecimal nonFossilExcludedFactor;

    private BigDecimal nationalFossilPowerFactor;

    private Integer rowNo;

    private String fuelLevel1;

    private String fuelLevel2;

    private String fuelLevel3;

    private String fuelLevel4;

    private BigDecimal lowerHeatValue;

    private BigDecimal lowerHeatValueCv;

    private BigDecimal co2Factor;

    private BigDecimal co2FactorCv;

    private BigDecimal gwpValue;

    private BigDecimal convertedFactor;

    private String sourceRef;

    private String customFields;

    private Boolean enabledFlag;

    private Date syncedTime;
}
