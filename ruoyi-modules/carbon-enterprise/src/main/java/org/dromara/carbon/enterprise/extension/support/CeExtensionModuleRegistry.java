package org.dromara.carbon.enterprise.extension.support;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * Central contract for enterprise-owned pages that may carry extension fields.
 */
public final class CeExtensionModuleRegistry {

    private static final Map<String, ModuleDefinition> MODULES = Map.ofEntries(
        module("company", "ce_company_factory", "company",
            "id", "companyCode", "companyName", "parentCode", "factoryCode", "factoryName",
            "companySk", "provinceCode", "provinceName", "factoryType", "industrySectionCode",
            "industrySectionName", "industryDivisionCode", "industryDivisionName", "industryGroupCode",
            "industryGroupName", "industryClassCode", "industryClassName", "effectiveDate", "expiryDate",
            "activeFlag", "status", "sortOrder", "remark", "createTime", "updateTime"),
        module("base-year", "ce_base_year", "base-year",
            "id", "recordCode", "recordName", "factoryCode", "factoryName", "baseYearKey",
            "description", "baseYear", "isCurrent", "currentBaseFlag", "status", "sortOrder",
            "remark", "createTime", "updateTime"),
        module("ef-factor", "ce_ef_factor", "ef-factor",
            "id", "recordCode", "recordName", "factorSk", "emissionSourceName",
            "emissionSourceNameEn", "fuelMaterialCategory", "sourceUnit", "co2", "ch4", "n2o",
            "hfcs", "pfcs", "sf6", "nf3", "applicableScope", "factorSource", "gwpCh4",
            "gwpN2o", "gwpHfcs", "gwpPfcs", "gwpSf6", "gwpNf3", "factorGwp", "factorUnit",
            "status", "sortOrder", "remark", "createTime", "updateTime"),
        module("ef-electricity-version", "ce_electricity_factor_version_map", "ef-electricity-version",
            "id", "recordCode", "recordName", "factorVersion", "effectiveYear", "status",
            "sortOrder", "remark", "createTime", "updateTime"),
        module("intensity-denominator", "ce_intensity_denominator_rule", "intensity-denominator",
            "id", "recordCode", "recordName", "denominatorType", "denominatorMetricName",
            "intensityUnitDisplay", "enabledText", "status", "sortOrder", "remark", "createTime", "updateTime"),
        module("intensity-target", "ce_intensity_target", "intensity-target",
            "id", "recordCode", "recordName", "targetYear", "targetValue", "unitName",
            "enabledText", "status", "sortOrder", "remark", "createTime", "updateTime"),
        module("denominator-fact", "ce_intensity_denominator_fact", "denominator-fact",
            "id", "recordCode", "recordName", "factYear", "factMonth", "denominatorValue",
            "unitName", "dataSource", "status", "sortOrder", "remark", "createTime", "updateTime"),
        module("intensity-tolerance", "ce_intensity_tolerance", "intensity-tolerance",
            "id", "recordCode", "recordName", "toleranceRate", "enabledText", "status",
            "sortOrder", "remark", "createTime", "updateTime"),
        module("emission_source", "ce_emission_source", null,
            "id", "companyCode", "companyName", "factoryCode", "factoryName", "sourceCategoryKey",
            "scopeName", "scopeSubcategory", "sourceIdentificationCode", "sourceIdentificationName",
            "emissionSourceName", "responsibleDept", "dataFrequency", "responsibleUserId",
            "responsibleUserName", "dataSource", "factorKey", "sourceUnit", "enabledFlag",
            "remark", "createTime", "updateTime"),
        module("activity_data", "ce_activity_data", null,
            "id", "batchId", "emissionSourceId", "activityPeriod", "sourceSheetCode",
            "sourceIdentificationCode", "companyCode", "companyName", "factoryCode", "factoryName",
            "sourceCategoryKey", "scopeName", "scopeSubcategory", "sourceIdentificationName",
            "emissionSourceName", "activityUnit", "activityYear", "activityMonth", "activityDate",
            "activityValue", "responsibleDept", "dataSource", "sourceRemark", "factorKey",
            "calculatedEmission", "dataStatus", "remark", "createTime", "updateTime"),
        module("green_electricity", "ce_green_power_certificate", null,
            "id", "factoryCode", "factoryName", "activityYear", "activityMonth", "sourceCategoryKey",
            "scopeName", "scopeSubcategory", "electricityType", "electricityTypeDesc", "quantityKwh",
            "certificateCode", "issuingOrg", "purchaseDate", "expiryDate", "powerGridRegion",
            "offsetPowerSource", "dataSource", "sourceRemark", "emissionSourceName", "factorKey",
            "proofStatus", "remark", "createTime", "updateTime"),
        module("intensity_metric", "ce_intensity_metric", null,
            "id", "metricCode", "metricName", "ruleCode", "metricPeriod", "numeratorEmission",
            "denominatorFactId", "denominatorValue", "denominatorUnit", "intensityValue",
            "targetCode", "metricStatus", "remark", "createTime", "updateTime")
    );

    private static final Set<String> VALUE_TYPES = Set.of("text", "textarea", "number", "decimal", "integer", "date", "boolean", "select");

    private CeExtensionModuleRegistry() {
    }

    public static ModuleDefinition require(String moduleCode) {
        ModuleDefinition definition = MODULES.get(moduleCode);
        if (definition == null) {
            throw new ServiceException("Unsupported enterprise extension module code: " + moduleCode);
        }
        return definition;
    }

    public static void validateValueType(String valueType) {
        if (StringUtils.isBlank(valueType)) {
            return;
        }
        if (!VALUE_TYPES.contains(valueType.toLowerCase())) {
            throw new ServiceException("Unsupported enterprise extension value type: " + valueType);
        }
    }

    private static Map.Entry<String, ModuleDefinition> module(String moduleCode, String ownerTableCode, String dimensionCode, String... reservedFields) {
        return Map.entry(moduleCode, new ModuleDefinition(moduleCode, ownerTableCode, dimensionCode, Set.of(reservedFields)));
    }

    public record ModuleDefinition(String moduleCode, String ownerTableCode, String dimensionCode, Set<String> reservedFields) {

        public boolean isDimensionProjection() {
            return StringUtils.isNotBlank(dimensionCode);
        }

        public boolean ownsTable(String ownerTableCode) {
            return this.ownerTableCode.equals(ownerTableCode);
        }

        public boolean isReservedField(String fieldCode) {
            if (StringUtils.isBlank(fieldCode)) {
                return false;
            }
            return reservedFields.stream().anyMatch(field -> field.equalsIgnoreCase(fieldCode));
        }
    }
}
