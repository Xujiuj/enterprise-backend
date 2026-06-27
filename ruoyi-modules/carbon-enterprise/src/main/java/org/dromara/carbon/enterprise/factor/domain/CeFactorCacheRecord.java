package org.dromara.carbon.enterprise.factor.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local factor cache record.
 */
@Data
@TableName("ce_factor_cache_record")
public class CeFactorCacheRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long cacheVersionId;

    private String factorTableCode;

    private String factorCode;

    private String factorName;

    private String factorCategory;

    private BigDecimal factorValue;

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
