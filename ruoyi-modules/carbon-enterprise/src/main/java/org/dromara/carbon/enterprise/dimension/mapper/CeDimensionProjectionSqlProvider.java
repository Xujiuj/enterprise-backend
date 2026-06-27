package org.dromara.carbon.enterprise.dimension.mapper;

import java.util.Map;

/**
 * SQL provider for concrete-table dimension projections.
 */
public class CeDimensionProjectionSqlProvider {

    public String selectByDimensionCode(Map<String, Object> params) {
        return projectionSql((String) params.get("dimensionCode"));
    }

    private String projectionSql(String dimensionCode) {
        return switch (dimensionCode) {
            case "admin-division" -> """
                select id,
                       'admin-division' as dimension_code,
                       division_code as record_code,
                       division_name as record_name,
                       null as parent_code,
                       division_code as field01,
                       division_name as field02,
                       null as field03,
                       null as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_admin_division
                 order by division_code
                """;
            case "company" -> """
                select id,
                       'company' as dimension_code,
                       company_code as record_code,
                       company_name as record_name,
                       factory_code as parent_code,
                       company_sk as field01,
                       factory_name as field02,
                       province_code as field03,
                       province_name as field04,
                       factory_type as field05,
                       industry_section_code as field06,
                       industry_section_name as field07,
                       industry_division_code as field08,
                       industry_division_name as field09,
                       industry_group_code as field10,
                       industry_group_name as field11,
                       industry_class_code as field12,
                       industry_class_name as field13,
                       cast(effective_date as char) as field14,
                       cast(expiry_date as char) as field15,
                       is_active as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       case when is_active = 'Y' then '0' else '1' end as status,
                       create_time,
                       update_time,
                       remark
                  from ce_company_factory
                 order by company_code, factory_code
                """;
            case "emission-source-category" -> """
                select id,
                       'emission-source-category' as dimension_code,
                       business_key as record_code,
                       coalesce(unified_standard_category, ghg_scope_category, iso_category, gb_scope_category, business_key) as record_name,
                       null as parent_code,
                       category_sk as field01,
                       business_key as field02,
                       ghg_scope as field03,
                       cast(ghg_scope_category_sort as char) as field04,
                       ghg_scope_category as field05,
                       ghg_scope_en as field06,
                       ghg_scope_category_en as field07,
                       iso_category as field08,
                       iso_category_en as field09,
                       iso_category_description as field10,
                       iso_category_description_en as field11,
                       iso_custom_subcategory as field12,
                       gb_scope_category as field13,
                       gb_subcategory as field14,
                       cast(effective_date as char) as field15,
                       cast(expiry_date as char) as field16,
                       is_current as field17,
                       version_no as field18,
                       unified_standard_category as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       coalesce(ghg_scope_category_sort, id) as sort_order,
                       case when is_current = 'Y' then '0' else '1' end as status,
                       create_time,
                       update_time,
                       remark
                  from ce_emission_source_category
                 order by coalesce(ghg_scope_category_sort, id), business_key
                """;
            case "base-year" -> """
                select id,
                       'base-year' as dimension_code,
                       factory_code as record_code,
                       coalesce(factory_name, factory_code) as record_name,
                       null as parent_code,
                       cast(base_year as char) as field01,
                       case when enabled_flag = 1 then 'Y' else 'N' end as field02,
                       null as field03,
                       null as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       case when enabled_flag = 1 then '0' else '1' end as status,
                       create_time,
                       update_time,
                       remark
                  from ce_base_year
                 order by factory_code, base_year
                """;
            case "ef-factor" -> """
                select id,
                       'ef-factor' as dimension_code,
                       factor_sk as record_code,
                       coalesce(emission_source_name, factor_sk) as record_name,
                       null as parent_code,
                       emission_source_name_en as field01,
                       fuel_material_category as field02,
                       source_unit as field03,
                       cast(co2 as char) as field04,
                       cast(ch4 as char) as field05,
                       cast(n2o as char) as field06,
                       cast(hfcs as char) as field07,
                       cast(pfcs as char) as field08,
                       cast(sf6 as char) as field09,
                       cast(nf3 as char) as field10,
                       applicable_scope as field11,
                       factor_source as field12,
                       cast(gwp_ch4 as char) as field13,
                       cast(gwp_n2o as char) as field14,
                       cast(gwp_hfcs as char) as field15,
                       cast(gwp_pfcs as char) as field16,
                       cast(gwp_sf6 as char) as field17,
                       cast(gwp_nf3 as char) as field18,
                       cast(factor_gwp as char) as field19,
                       factor_unit as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_ef_factor
                 order by factor_sk
                """;
            case "ef-electricity-factor" -> """
                select id,
                       'ef-electricity-factor' as dimension_code,
                       version_province_code as record_code,
                       coalesce(division_name, region_name, version_province_code) as record_name,
                       division_code as parent_code,
                       factor_version as field01,
                       division_code as field02,
                       division_name as field03,
                       region_name as field04,
                       cast(province_factor as char) as field05,
                       cast(region_factor as char) as field06,
                       cast(national_factor as char) as field07,
                       cast(non_fossil_excluded_factor as char) as field08,
                       cast(national_fossil_power_factor as char) as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_electricity_factor
                 order by factor_version, division_code
                """;
            case "ef-electricity-version" -> """
                select id,
                       'ef-electricity-version' as dimension_code,
                       factor_version as record_code,
                       concat(factor_version, ' ', effective_year) as record_name,
                       null as parent_code,
                       factor_version as field01,
                       cast(effective_year as char) as field02,
                       null as field03,
                       null as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_electricity_factor_version_map
                 order by effective_year asc, factor_version asc
                """;
            case "ef-electricity-scope" -> """
                select id,
                       'ef-electricity-scope' as dimension_code,
                       scope_key as record_code,
                       scope_name as record_name,
                       null as parent_code,
                       scope_key as field01,
                       scope_name as field02,
                       null as field03,
                       null as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_electricity_factor_scope
                 order by scope_key
                """;
            case "greenhouse-gas" -> """
                select id,
                       'greenhouse-gas' as dimension_code,
                       gas_code as record_code,
                       gas_name as record_name,
                       null as parent_code,
                       gas_name_en as field01,
                       null as field02,
                       null as field03,
                       null as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_greenhouse_gas
                 order by gas_code
                """;
            case "intensity-denominator" -> """
                select id,
                       'intensity-denominator' as dimension_code,
                       denominator_rule_key as record_code,
                       factory_type as record_name,
                       null as parent_code,
                       factory_type as field01,
                       denominator_type as field02,
                       denominator_metric_name as field03,
                       intensity_unit_display as field04,
                       case when enabled_flag = 1 then '是' else '否' end as field05,
                       remark as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       case when enabled_flag = 1 then '0' else '1' end as status,
                       create_time,
                       update_time,
                       remark
                  from ce_intensity_denominator_rule
                 order by factory_type, denominator_type
                """;
            case "intensity-target" -> """
                select id,
                       'intensity-target' as dimension_code,
                       factory_type as record_code,
                       cast(target_year as char) as record_name,
                       null as parent_code,
                       factory_type as field01,
                       cast(target_year as char) as field02,
                       cast(target_value as char) as field03,
                       unit_name as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_intensity_target
                 order by factory_type, target_year desc
                """;
            case "denominator-fact" -> """
                select id,
                       'denominator-fact' as dimension_code,
                       factory_code as record_code,
                       factory_name as record_name,
                       null as parent_code,
                       factory_name as field01,
                       factory_type as field02,
                       cast(fact_year as char) as field03,
                       cast(fact_month as char) as field04,
                       denominator_type as field05,
                       denominator_metric_name as field06,
                       cast(denominator_value as char) as field07,
                       unit_name as field08,
                       data_source as field09,
                       remark as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_intensity_denominator_fact
                 order by fact_year desc, fact_month desc, factory_code
                """;
            case "intensity-tolerance" -> """
                select id,
                       'intensity-tolerance' as dimension_code,
                       tolerance_key as record_code,
                       industry_section as record_name,
                       null as parent_code,
                       industry_section as field01,
                       cast(tolerance_rate as char) as field02,
                       case when enabled_flag = 1 then '是' else '否' end as field03,
                       remark as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       id as sort_order,
                       case when enabled_flag = 1 then '0' else '1' end as status,
                       create_time,
                       update_time,
                       remark
                  from ce_intensity_tolerance
                 order by industry_section, tolerance_key
                """;
            default -> """
                select null as id,
                       null as dimension_code,
                       null as record_code,
                       null as record_name,
                       null as parent_code,
                       null as field01,
                       null as field02,
                       null as field03,
                       null as field04,
                       null as field05,
                       null as field06,
                       null as field07,
                       null as field08,
                       null as field09,
                       null as field10,
                       null as field11,
                       null as field12,
                       null as field13,
                       null as field14,
                       null as field15,
                       null as field16,
                       null as field17,
                       null as field18,
                       null as field19,
                       null as field20,
                       null as field21,
                       null as field22,
                       null as sort_order,
                       null as status,
                       null as create_time,
                       null as update_time,
                       null as remark
                  where 1 = 0
                """;
        };
    }

    public String selectByDimensionCodeAndId(Map<String, Object> params) {
        return "select * from (" + projectionSql((String) params.get("dimensionCode")) + ") dimension_projection where id = #{id}";
    }

    public String insertByDimensionCode() {
        return """
            <script>
            <choose>
              <when test="record.dimensionCode == 'company'">
                insert into ce_company_factory (
                  company_sk, company_code, factory_code, company_name, factory_name,
                  province_code, province_name, factory_type,
                  industry_section_code, industry_section_name,
                  industry_division_code, industry_division_name,
                  industry_group_code, industry_group_name,
                  industry_class_code, industry_class_name,
                  effective_date, expiry_date, is_active, remark
                ) values (
                  #{record.field01}, #{record.recordCode}, #{record.parentCode}, #{record.recordName}, #{record.field02},
                  #{record.field03}, #{record.field04}, #{record.field05},
                  #{record.field06}, #{record.field07},
                  #{record.field08}, #{record.field09},
                  #{record.field10}, #{record.field11},
                  #{record.field12}, #{record.field13},
                  #{record.field14}, #{record.field15},
                  case when #{record.status} = '1' then 'N' else coalesce(#{record.field16}, 'Y') end,
                  #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'base-year'">
                insert into ce_base_year (
                  factory_code, factory_name, base_year, enabled_flag, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.field01},
                  case when #{record.status} = '1' or #{record.field02} = 'N' then 0 else 1 end,
                  #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'ef-factor'">
                insert into ce_ef_factor (
                  factor_sk, emission_source_name, emission_source_name_en, fuel_material_category, source_unit,
                  co2, ch4, n2o, hfcs, pfcs, sf6, nf3,
                  applicable_scope, factor_source,
                  gwp_ch4, gwp_n2o, gwp_hfcs, gwp_pfcs, gwp_sf6, gwp_nf3,
                  factor_gwp, factor_unit, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.field01}, #{record.field02}, #{record.field03},
                  #{record.field04}, #{record.field05}, #{record.field06}, #{record.field07}, #{record.field08}, #{record.field09}, #{record.field10},
                  #{record.field11}, #{record.field12},
                  #{record.field13}, #{record.field14}, #{record.field15}, #{record.field16}, #{record.field17}, #{record.field18},
                  #{record.field19}, #{record.field20}, #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'ef-electricity-version'">
                insert into ce_electricity_factor_version_map (
                  factor_version, effective_year, remark
                ) values (
                  #{record.recordCode}, #{record.field02}, #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'intensity-denominator'">
                insert into ce_intensity_denominator_rule (
                  denominator_rule_key, factory_type, denominator_type,
                  denominator_metric_name, intensity_unit_display, enabled_flag, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.field02},
                  #{record.field03}, #{record.field04},
                  case when #{record.field05} = '否' or #{record.status} = '1' then 0 else 1 end,
                  #{record.field06}
                )
              </when>
              <when test="record.dimensionCode == 'intensity-target'">
                insert into ce_intensity_target (
                  factory_type, target_year, target_value, unit_name, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.field03}, #{record.field04}, #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'denominator-fact'">
                insert into ce_intensity_denominator_fact (
                  factory_code, factory_name, factory_type, fact_year, fact_month,
                  denominator_type, denominator_metric_name, denominator_value,
                  unit_name, data_source, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.field02}, #{record.field03}, #{record.field04},
                  #{record.field05}, #{record.field06}, #{record.field07},
                  #{record.field08}, #{record.field09}, #{record.field10}
                )
              </when>
              <when test="record.dimensionCode == 'intensity-tolerance'">
                insert into ce_intensity_tolerance (
                  tolerance_key, industry_section, tolerance_rate, enabled_flag, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.field02},
                  case when #{record.field03} = '否' or #{record.status} = '1' then 0 else 1 end,
                  #{record.field04}
                )
              </when>
              <otherwise>
                select 0
              </otherwise>
            </choose>
            </script>
            """;
    }

    public String updateByDimensionCode() {
        return """
            <script>
            <choose>
              <when test="record.dimensionCode == 'company'">
                update ce_company_factory
                   set company_sk = #{record.field01},
                       company_code = #{record.recordCode},
                       factory_code = #{record.parentCode},
                       company_name = #{record.recordName},
                       factory_name = #{record.field02},
                       province_code = #{record.field03},
                       province_name = #{record.field04},
                       factory_type = #{record.field05},
                       industry_section_code = #{record.field06},
                       industry_section_name = #{record.field07},
                       industry_division_code = #{record.field08},
                       industry_division_name = #{record.field09},
                       industry_group_code = #{record.field10},
                       industry_group_name = #{record.field11},
                       industry_class_code = #{record.field12},
                       industry_class_name = #{record.field13},
                       effective_date = #{record.field14},
                       expiry_date = #{record.field15},
                       is_active = case when #{record.status} = '1' then 'N' else coalesce(#{record.field16}, 'Y') end,
                       update_time = now(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'base-year'">
                update ce_base_year
                   set factory_code = #{record.recordCode},
                       factory_name = #{record.recordName},
                       base_year = #{record.field01},
                       enabled_flag = case when #{record.status} = '1' or #{record.field02} = 'N' then 0 else 1 end,
                       update_time = now(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'ef-factor'">
                update ce_ef_factor
                   set factor_sk = #{record.recordCode},
                       emission_source_name = #{record.recordName},
                       emission_source_name_en = #{record.field01},
                       fuel_material_category = #{record.field02},
                       source_unit = #{record.field03},
                       co2 = #{record.field04},
                       ch4 = #{record.field05},
                       n2o = #{record.field06},
                       hfcs = #{record.field07},
                       pfcs = #{record.field08},
                       sf6 = #{record.field09},
                       nf3 = #{record.field10},
                       applicable_scope = #{record.field11},
                       factor_source = #{record.field12},
                       gwp_ch4 = #{record.field13},
                       gwp_n2o = #{record.field14},
                       gwp_hfcs = #{record.field15},
                       gwp_pfcs = #{record.field16},
                       gwp_sf6 = #{record.field17},
                       gwp_nf3 = #{record.field18},
                       factor_gwp = #{record.field19},
                       factor_unit = #{record.field20},
                       update_time = now(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'ef-electricity-version'">
                update ce_electricity_factor_version_map
                   set factor_version = #{record.recordCode},
                       effective_year = #{record.field02},
                       update_time = now(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'intensity-denominator'">
                update ce_intensity_denominator_rule
                   set denominator_rule_key = #{record.recordCode},
                       factory_type = #{record.recordName},
                       denominator_type = #{record.field02},
                       denominator_metric_name = #{record.field03},
                       intensity_unit_display = #{record.field04},
                       enabled_flag = case when #{record.field05} = '否' or #{record.status} = '1' then 0 else 1 end,
                       update_time = now(),
                       remark = #{record.field06}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'intensity-target'">
                update ce_intensity_target
                   set factory_type = #{record.recordCode},
                       target_year = #{record.recordName},
                       target_value = #{record.field03},
                       unit_name = #{record.field04},
                       update_time = now(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'denominator-fact'">
                update ce_intensity_denominator_fact
                   set factory_code = #{record.recordCode},
                       factory_name = #{record.recordName},
                       factory_type = #{record.field02},
                       fact_year = #{record.field03},
                       fact_month = #{record.field04},
                       denominator_type = #{record.field05},
                       denominator_metric_name = #{record.field06},
                       denominator_value = #{record.field07},
                       unit_name = #{record.field08},
                       data_source = #{record.field09},
                       update_time = now(),
                       remark = #{record.field10}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'intensity-tolerance'">
                update ce_intensity_tolerance
                   set tolerance_key = #{record.recordCode},
                       industry_section = #{record.recordName},
                       tolerance_rate = #{record.field02},
                       enabled_flag = case when #{record.field03} = '否' or #{record.status} = '1' then 0 else 1 end,
                       update_time = now(),
                       remark = #{record.field04}
                 where id = #{record.id}
              </when>
              <otherwise>
                select 0
              </otherwise>
            </choose>
            </script>
            """;
    }

    public String deleteByDimensionCodeAndId(Map<String, Object> params) {
        return switch ((String) params.get("dimensionCode")) {
            case "company" -> "delete from ce_company_factory where id = #{id}";
            case "base-year" -> "delete from ce_base_year where id = #{id}";
            case "ef-factor" -> "delete from ce_ef_factor where id = #{id}";
            case "ef-electricity-version" -> "delete from ce_electricity_factor_version_map where id = #{id}";
            case "intensity-denominator" -> "delete from ce_intensity_denominator_rule where id = #{id}";
            case "intensity-target" -> "delete from ce_intensity_target where id = #{id}";
            case "denominator-fact" -> "delete from ce_intensity_denominator_fact where id = #{id}";
            case "intensity-tolerance" -> "delete from ce_intensity_tolerance where id = #{id}";
            default -> "select 0";
        };
    }
}
