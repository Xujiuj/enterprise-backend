package org.dromara.carbon.enterprise.dimension.mapper;

import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;

import java.util.Map;

/**
 * SQL provider for concrete-table dimension projections.
 */
public class CeDimensionProjectionSqlProvider {

    public String selectByDimensionCode(Map<String, Object> params) {
        return projectionSql((String) params.get("dimensionCode"));
    }

    public String selectPageByDimensionCode(Map<String, Object> params) {
        CeDimensionRecordBo query = (CeDimensionRecordBo) params.get("query");
        String dimensionCode = query == null ? null : query.getDimensionCode();
        String baseSql = stripTrailingOrderBy(projectionSql(dimensionCode));
        String orderBy = switch (dimensionCode) {
            case "ef-factor" -> "create_time desc, id desc";
            case "emission-source-category-history" ->
                "try_convert(decimal(30, 10), version_no) desc, version_no desc, sort_order, record_code";
            default -> "sort_order, record_code";
        };
        return """
            <script>
            select *
              from (
            """ + baseSql + """
              ) dimension_projection
             where 1 = 1
            <if test="query.recordCode != null and query.recordCode != ''">
               and record_code like concat('%', #{query.recordCode}, '%')
            </if>
            <if test="query.recordName != null and query.recordName != ''">
               and record_name like concat('%', #{query.recordName}, '%')
            </if>
            <if test="query.parentCode != null and query.parentCode != ''">
               and parent_code = #{query.parentCode}
            </if>
            <if test="query.status != null and query.status != ''">
               and status = #{query.status}
            </if>
            <if test="query.currentFlag != null and query.currentFlag != ''">
               and current_flag = #{query.currentFlag}
            </if>
            <if test="query.versionNo != null and query.versionNo != ''">
               and version_no = #{query.versionNo}
            </if>
            """ + " order by " + orderBy + "\n" + """
            </script>
            """;
    }

    private String projectionSql(String dimensionCode) {
        return switch (dimensionCode) {
            case "admin-division" -> """
                select id,
                       'admin-division' as dimension_code,
                       division_code as record_code,
                       division_name as record_name,
                       parent_code as parent_code,
                       level_type as level_type,
                       coalesce(sort_order, id) as sort_order,
                       coalesce(status, '0') as status,
                       create_time,
                       update_time,
                       remark
                  from ce_admin_division
                 order by coalesce(sort_order, id), division_code
                """;
            case "company" -> """
                select factory.dept_id as id,
                       'company' as dimension_code,
                       company.dept_category as record_code,
                       company.dept_name as record_name,
                       factory.factory_code as parent_code,
                       concat('SK_', company.dept_category, '_', factory.factory_code) as company_sk,
                       factory.dept_name as factory_name,
                       null as province_code,
                       null as province_name,
                       null as factory_type,
                       null as industry_section_code,
                       null as industry_section_name,
                       null as industry_division_code,
                       null as industry_division_name,
                       null as industry_group_code,
                       null as industry_group_name,
                       null as industry_class_code,
                       null as industry_class_name,
                       null as effective_date,
                       null as expiry_date,
                       case when company.status = '0' and factory.status = '0' then 'Y' else 'N' end as active_flag,
                       factory.order_num as sort_order,
                       case when company.status = '0' and factory.status = '0' then '0' else '1' end as status,
                       factory.create_time,
                       factory.update_time,
                       null as remark
                  from sys_dept factory
                  join sys_dept company on company.dept_id = factory.parent_id
                 where factory.del_flag = '0'
                   and company.del_flag = '0'
                   and factory.dept_category = company.dept_category
                   and nullif(ltrim(rtrim(factory.factory_code)), '') is not null
                   and not exists (
                       select 1 from sys_dept company_parent
                        where company_parent.dept_id = company.parent_id
                          and company_parent.del_flag = '0'
                          and nullif(ltrim(rtrim(company_parent.dept_category)), '') is not null
                   )
                 order by company.dept_category, factory.factory_code
                """;
            case "industry" -> """
                select id,
                       'industry' as dimension_code,
                       coalesce(nullif(industry_class_code, ''), nullif(industry_group_code, ''), nullif(industry_division_code, ''), industry_section_code) as record_code,
                       coalesce(nullif(industry_class_name, ''), nullif(industry_group_name, ''), nullif(industry_division_name, ''), industry_section_name) as record_name,
                       coalesce(nullif(industry_group_code, ''), nullif(industry_division_code, ''), nullif(industry_section_code, '')) as parent_code,
                       industry_section_code as industry_section_code,
                       industry_section_name as industry_section_name,
                       industry_division_code as industry_division_code,
                       industry_division_name as industry_division_name,
                       industry_group_code as industry_group_code,
                       industry_group_name as industry_group_name,
                       industry_class_code as industry_class_code,
                       industry_class_name as industry_class_name,
                       coalesce(sort_order, id) as sort_order,
                       coalesce(status, '0') as status,
                       create_time,
                       update_time,
                       remark
                  from ce_industry_classification
                 order by coalesce(sort_order, id), industry_section_code, industry_division_code, industry_group_code, industry_class_code
                """;
            case "emission-source-category" -> emissionSourceCategoryProjection(true);
            case "emission-source-category-history" -> emissionSourceCategoryProjection(false);
            case "base-year" -> """
                select id,
                       'base-year' as dimension_code,
                       base_year_key as record_code,
                       cast(base_year as char) as record_name,
                       null as parent_code,
                       base_year_key as base_year_key,
                       description as description,
                       cast(base_year as char) as base_year,
                       cast(is_current as char) as is_current,
                       case when enabled_flag = 1 then 'Y' else 'N' end as current_base_flag,
                       coalesce(sort_order, id) as sort_order,
                       coalesce(status, case when enabled_flag = 1 then '0' else '1' end) as status,
                       create_time,
                       update_time,
                       remark
                  from ce_base_year
                 order by coalesce(sort_order, id), base_year_key, base_year
                """;
            case "ef-factor" -> """
                select id,
                       'ef-factor' as dimension_code,
                       factor_sk as record_code,
                       coalesce(emission_source_name, factor_sk) as record_name,
                       null as parent_code,
                       emission_source_name_en as emission_source_name_en,
                       fuel_material_category as fuel_material_category,
                       source_unit as source_unit,
                       cast(co2 as char) as co2,
                       cast(ch4 as char) as ch4,
                       cast(n2o as char) as n2o,
                       cast(hfcs as char) as hfcs,
                       cast(pfcs as char) as pfcs,
                       cast(sf6 as char) as sf6,
                       cast(nf3 as char) as nf3,
                       applicable_scope as applicable_scope,
                       factor_source as factor_source,
                       cast(gwp_ch4 as char) as gwp_ch4,
                       cast(gwp_n2o as char) as gwp_n2o,
                       cast(gwp_hfcs as char) as gwp_hfcs,
                       cast(gwp_pfcs as char) as gwp_pfcs,
                       cast(gwp_sf6 as char) as gwp_sf6,
                       cast(gwp_nf3 as char) as gwp_nf3,
                       cast(factor_gwp as char) as factor_gwp,
                       factor_unit as factor_unit,
                       id as sort_order,
                       '0' as status,
                       create_time,
                       update_time,
                       remark
                  from ce_ef_factor
                 order by create_time desc, id desc
                """;
            case "ef-electricity-factor" -> """
                select id,
                       'ef-electricity-factor' as dimension_code,
                       version_province_code as record_code,
                       coalesce(division_name, region_name, version_province_code) as record_name,
                       division_code as parent_code,
                       factor_version as factor_version,
                       division_code as division_code,
                       division_name as division_name,
                       region_name as region_name,
                       cast(province_factor as char) as province_factor,
                       cast(region_factor as char) as region_factor,
                       cast(national_factor as char) as national_factor,
                       cast(non_fossil_excluded_factor as char) as non_fossil_excluded_factor,
                       cast(national_fossil_power_factor as char) as national_fossil_power_factor,
                       coalesce(sort_order, id) as sort_order,
                       coalesce(status, '0') as status,
                       create_time,
                       update_time,
                       remark
                  from ce_electricity_factor
                 order by coalesce(sort_order, id), factor_version, division_code
                """;
            case "ef-electricity-version" -> """
                select id,
                       'ef-electricity-version' as dimension_code,
                       cast(effective_year as char) as record_code,
                       factor_version as record_name,
                       null as parent_code,
                       factor_version as factor_version,
                       cast(effective_year as char) as effective_year,
                       coalesce(sort_order, id) as sort_order,
                       coalesce(status, '0') as status,
                       create_time,
                       update_time,
                       remark
                  from ce_electricity_factor_version_map
                 order by coalesce(sort_order, id), effective_year asc, factor_version asc
                """;
            case "ef-electricity-scope" -> """
                select id,
                       'ef-electricity-scope' as dimension_code,
                       scope_key as record_code,
                       scope_name as record_name,
                       null as parent_code,
                       scope_key as scope_key,
                       scope_name as scope_name,
                       coalesce(sort_order, id) as sort_order,
                       coalesce(status, '0') as status,
                       create_time,
                       update_time,
                       remark
                  from ce_electricity_factor_scope
                 order by coalesce(sort_order, id), scope_key
                """;
            case "greenhouse-gas" -> """
                select id,
                       'greenhouse-gas' as dimension_code,
                       gas_code as record_code,
                       gas_name as record_name,
                       null as parent_code,
                       gas_name_en as gas_name_en,
                       cast(gwp_value as char) as gwp_value,
                       gwp_version as gwp_version,
                       chemical_formula as chemical_formula,
                       coalesce(sort_order, id) as sort_order,
                       coalesce(status, '0') as status,
                       create_time,
                       update_time,
                       remark
                  from ce_greenhouse_gas
                 order by coalesce(sort_order, id), gas_code
                """;
            case "intensity-denominator" -> """
                select id,
                       'intensity-denominator' as dimension_code,
                       denominator_rule_key as record_code,
                       factory_type as record_name,
                       null as parent_code,
                       factory_type as factory_type,
                       denominator_type as denominator_type,
                       denominator_metric_name as denominator_metric_name,
                       intensity_unit_display as intensity_unit_display,
                       case when enabled_flag = 1 then '1' else '0' end as enabled_text,
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
                       factory_type as factory_type,
                       cast(target_year as char) as target_year,
                       cast(target_value as char) as target_value,
                       unit_name as unit_name,
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
                       factory_name as factory_name,
                       factory_type as factory_type,
                       cast(fact_year as char) as fact_year,
                       cast(fact_month as char) as fact_month,
                       denominator_type as denominator_type,
                       denominator_metric_name as denominator_metric_name,
                       cast(denominator_value as char) as denominator_value,
                       unit_name as unit_name,
                       data_source as data_source,
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
                       industry_section as industry_section,
                       cast(tolerance_rate as char) as tolerance_rate,
                       case when enabled_flag = 1 then '1' else '0' end as enabled_text,
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
                       null as sort_order,
                       null as status,
                       null as create_time,
                       null as update_time,
                       null as remark
                  where 1 = 0
                """;
        };
    }

    private String emissionSourceCategoryProjection(boolean latestOnly) {
        String latestVersionFilter = latestOnly ? """
             where coalesce(nullif(ltrim(rtrim(version_no)), ''), '1') = (
                   select top 1 coalesce(nullif(ltrim(rtrim(latest.version_no)), ''), '1')
                     from ce_emission_source_category latest
                    order by try_convert(decimal(30, 10), latest.version_no) desc,
                             latest.effective_date desc,
                             latest.version_no desc,
                             case when upper(ltrim(rtrim(latest.is_current))) in ('Y', '1', 'TRUE') then 1 else 0 end desc,
                             latest.id desc
                 )
            """ : "";
        return """
            select id,
                   'emission-source-category' as dimension_code,
                   category_sk as record_code,
                   coalesce(unified_standard_category, ghg_scope_category, iso_category, gb_scope_category, business_key) as record_name,
                   parent_code as parent_code,
                   category_sk as category_sk,
                   business_key as business_key,
                   category_name_en as category_name_en,
                   ghg_scope as ghg_scope,
                   cast(ghg_scope_category_sort as char) as ghg_scope_category_sort,
                   ghg_scope_category as ghg_scope_category,
                   ghg_scope_en as ghg_scope_en,
                   ghg_scope_category_en as ghg_scope_category_en,
                   iso_category as iso_category,
                   iso_category_en as iso_category_en,
                   iso_category_description as iso_category_description,
                   iso_category_description_en as iso_category_description_en,
                   iso_custom_subcategory as iso_custom_subcategory,
                   gb_scope_category as gb_scope_category,
                   gb_subcategory as gb_subcategory,
                   cast(effective_date as char) as effective_date,
                   cast(expiry_date as char) as expiry_date,
                   case when upper(ltrim(rtrim(is_current))) in ('Y', '1', 'TRUE') then 'Y' else 'N' end as current_flag,
                   coalesce(nullif(ltrim(rtrim(version_no)), ''), '1') as version_no,
                   unified_standard_category as unified_standard_category,
                   coalesce(sort_order, ghg_scope_category_sort, id) as sort_order,
                   coalesce(status, '0') as status,
                   create_time,
                   update_time,
                   remark
              from ce_emission_source_category
            """ + latestVersionFilter + """
             order by coalesce(sort_order, ghg_scope_category_sort, id), business_key
            """;
    }

    public String selectByDimensionCodeAndId(Map<String, Object> params) {
        return "select * from (" + stripTrailingOrderBy(projectionSql((String) params.get("dimensionCode"))) + ") dimension_projection where id = #{id}";
    }

    private String stripTrailingOrderBy(String sql) {
        return sql.replaceFirst("(?is)\\s+order\\s+by\\s+[^\\n]+\\s*$", "");
    }

    public String insertByDimensionCode() {
        return """
            <script>
            <choose>
              <when test="record.dimensionCode == 'industry'">
                insert into ce_industry_classification (
                  industry_section_code, industry_section_name,
                  industry_division_code, industry_division_name,
                  industry_group_code, industry_group_name,
                  industry_class_code, industry_class_name,
                  status, sort_order, remark
                ) values (
                  #{record.industrySectionCode}, #{record.industrySectionName},
                  #{record.industryDivisionCode}, #{record.industryDivisionName},
                  #{record.industryGroupCode}, #{record.industryGroupName},
                  #{record.industryClassCode}, #{record.industryClassName},
                  coalesce(#{record.status}, '0'), try_convert(int, nullif(#{record.sortOrder,jdbcType=VARCHAR}, '')), #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'base-year'">
                insert into ce_base_year (
                  base_year_key, base_year, description, is_current, enabled_flag, status, sort_order, remark
                ) values (
                  #{record.recordCode}, try_convert(int, nullif(#{record.recordName,jdbcType=VARCHAR}, '')),
                  #{record.description},
                  case when #{record.currentBaseFlag} = 'Y' or #{record.isCurrent} = '1' then 1 else 0 end,
                  case when #{record.status} = '1' or #{record.currentBaseFlag} = 'N' then 0 else 1 end,
                  coalesce(#{record.status}, '0'),
                  try_convert(int, nullif(#{record.sortOrder,jdbcType=VARCHAR}, '')),
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
                  #{record.recordCode}, #{record.recordName}, #{record.emissionSourceNameEn}, #{record.fuelMaterialCategory}, #{record.sourceUnit},
                  #{record.co2}, #{record.ch4}, #{record.n2o}, #{record.hfcs}, #{record.pfcs}, #{record.sf6}, #{record.nf3},
                  #{record.applicableScope}, #{record.factorSource},
                  #{record.gwpCh4}, #{record.gwpN2o}, #{record.gwpHfcs}, #{record.gwpPfcs}, #{record.gwpSf6}, #{record.gwpNf3},
                  #{record.factorGwp}, #{record.factorUnit}, #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'ef-electricity-version'">
                insert into ce_electricity_factor_version_map (
                  factor_version, effective_year, remark
                ) values (
                  #{record.recordName}, #{record.recordCode}, #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'intensity-denominator'">
                insert into ce_intensity_denominator_rule (
                  denominator_rule_key, factory_type, denominator_type,
                  denominator_metric_name, intensity_unit_display, enabled_flag, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.denominatorType},
                  #{record.denominatorMetricName}, #{record.intensityUnitDisplay},
                  case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end,
                  #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'intensity-target'">
                insert into ce_intensity_target (
                  factory_type, target_year, target_value, unit_name, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.targetValue}, #{record.unitName}, #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'denominator-fact'">
                insert into ce_intensity_denominator_fact (
                  factory_code, factory_name, factory_type, fact_year, fact_month,
                  denominator_type, denominator_metric_name, denominator_value,
                  unit_name, data_source, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.factoryType}, #{record.factYear}, #{record.factMonth},
                  #{record.denominatorType}, #{record.denominatorMetricName}, #{record.denominatorValue},
                  #{record.unitName}, #{record.dataSource}, #{record.remark}
                )
              </when>
              <when test="record.dimensionCode == 'intensity-tolerance'">
                insert into ce_intensity_tolerance (
                  tolerance_key, industry_section, tolerance_rate, enabled_flag, remark
                ) values (
                  #{record.recordCode}, #{record.recordName}, #{record.toleranceRate},
                  case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end,
                  #{record.remark}
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
              <when test="record.dimensionCode == 'industry'">
                update ce_industry_classification
                   set industry_section_code = #{record.industrySectionCode},
                       industry_section_name = #{record.industrySectionName},
                       industry_division_code = #{record.industryDivisionCode},
                       industry_division_name = #{record.industryDivisionName},
                       industry_group_code = #{record.industryGroupCode},
                       industry_group_name = #{record.industryGroupName},
                       industry_class_code = #{record.industryClassCode},
                       industry_class_name = #{record.industryClassName},
                       status = coalesce(#{record.status}, '0'),
                       sort_order = try_convert(int, nullif(#{record.sortOrder,jdbcType=VARCHAR}, '')),
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'base-year'">
                update ce_base_year
                   set base_year_key = #{record.recordCode},
                       base_year = try_convert(int, nullif(#{record.recordName,jdbcType=VARCHAR}, '')),
                       description = #{record.description},
                       is_current = case when #{record.currentBaseFlag} = 'Y' or #{record.isCurrent} = '1' then 1 else 0 end,
                       enabled_flag = case when #{record.status} = '1' or #{record.currentBaseFlag} = 'N' then 0 else 1 end,
                       status = coalesce(#{record.status}, '0'),
                       sort_order = try_convert(int, nullif(#{record.sortOrder,jdbcType=VARCHAR}, '')),
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'ef-factor'">
                update ce_ef_factor
                   set factor_sk = #{record.recordCode},
                       emission_source_name = #{record.recordName},
                       emission_source_name_en = #{record.emissionSourceNameEn},
                       fuel_material_category = #{record.fuelMaterialCategory},
                       source_unit = #{record.sourceUnit},
                       co2 = #{record.co2},
                       ch4 = #{record.ch4},
                       n2o = #{record.n2o},
                       hfcs = #{record.hfcs},
                       pfcs = #{record.pfcs},
                       sf6 = #{record.sf6},
                       nf3 = #{record.nf3},
                       applicable_scope = #{record.applicableScope},
                       factor_source = #{record.factorSource},
                       gwp_ch4 = #{record.gwpCh4},
                       gwp_n2o = #{record.gwpN2o},
                       gwp_hfcs = #{record.gwpHfcs},
                       gwp_pfcs = #{record.gwpPfcs},
                       gwp_sf6 = #{record.gwpSf6},
                       gwp_nf3 = #{record.gwpNf3},
                       factor_gwp = #{record.factorGwp},
                       factor_unit = #{record.factorUnit},
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'ef-electricity-version'">
                update ce_electricity_factor_version_map
                   set factor_version = #{record.recordName},
                       effective_year = #{record.recordCode},
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'intensity-denominator'">
                update ce_intensity_denominator_rule
                   set denominator_rule_key = #{record.recordCode},
                       factory_type = #{record.recordName},
                       denominator_type = #{record.denominatorType},
                       denominator_metric_name = #{record.denominatorMetricName},
                       intensity_unit_display = #{record.intensityUnitDisplay},
                       enabled_flag = case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end,
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'intensity-target'">
                update ce_intensity_target
                   set factory_type = #{record.recordCode},
                       target_year = #{record.recordName},
                       target_value = #{record.targetValue},
                       unit_name = #{record.unitName},
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'denominator-fact'">
                update ce_intensity_denominator_fact
                   set factory_code = #{record.recordCode},
                       factory_name = #{record.recordName},
                       factory_type = #{record.factoryType},
                       fact_year = #{record.factYear},
                       fact_month = #{record.factMonth},
                       denominator_type = #{record.denominatorType},
                       denominator_metric_name = #{record.denominatorMetricName},
                       denominator_value = #{record.denominatorValue},
                       unit_name = #{record.unitName},
                       data_source = #{record.dataSource},
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
                 where id = #{record.id}
              </when>
              <when test="record.dimensionCode == 'intensity-tolerance'">
                update ce_intensity_tolerance
                   set tolerance_key = #{record.recordCode},
                       industry_section = #{record.recordName},
                       tolerance_rate = #{record.toleranceRate},
                       enabled_flag = case when #{record.enabledText} = '0' or #{record.status} = '1' then 0 else 1 end,
                       update_time = SYSDATETIME(),
                       remark = #{record.remark}
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
            case "industry" -> "delete from ce_industry_classification where id = #{id}";
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
