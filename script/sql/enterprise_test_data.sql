-- Enterprise carbon data platform test data SQL.
-- Run after enterprise_init.sql against local MySQL database `enterprise`.
-- Scope: enterprise-backend only. Do not run this against the vendor database.

USE enterprise;
SET NAMES utf8mb4;

DELETE FROM ce_extension_field_value WHERE remark = 'test-data';
DELETE FROM ce_intensity_metric WHERE remark = 'test-data';
DELETE FROM ce_intensity_denominator_fact WHERE remark = 'test-data';
DELETE FROM ce_intensity_target WHERE remark = 'test-data';
DELETE FROM ce_intensity_tolerance WHERE remark = 'test-data';
DELETE FROM ce_intensity_denominator_rule WHERE remark = 'test-data';
DELETE FROM ce_green_power_certificate WHERE remark = 'test-data';
DELETE FROM ce_activity_data WHERE remark = 'test-data';
DELETE FROM ce_emission_source WHERE remark = 'test-data';
DELETE FROM ce_factor_cache_record WHERE factor_code LIKE 'TEST-%';
DELETE FROM ce_factor_cache_version WHERE license_id = 'LIC-TEST-2026';
DELETE FROM ce_factor_confirmation WHERE license_id = 'LIC-TEST-2026';
DELETE FROM ce_license_state WHERE license_id = 'LIC-TEST-2026';
DELETE FROM ce_report_template_file WHERE template_code LIKE 'TEST_%';
DELETE FROM ce_greenhouse_gas WHERE remark = 'test-data';
DELETE FROM ce_electricity_factor_scope WHERE remark = 'test-data';
DELETE FROM ce_electricity_factor_version_map WHERE remark = 'test-data';
DELETE FROM ce_electricity_factor WHERE remark = 'test-data';
DELETE FROM ce_fuel_factor_calc WHERE remark = 'test-data';
DELETE FROM ce_ef_factor WHERE remark = 'test-data';
DELETE FROM ce_base_year WHERE remark = 'test-data';
DELETE FROM ce_emission_source_category WHERE remark = 'test-data';
DELETE FROM ce_company_factory WHERE remark = 'test-data';
DELETE FROM ce_admin_division WHERE remark = 'test-data';
DELETE FROM sys_user_role WHERE user_id IN (900101, 900102, 900103);
DELETE FROM sys_user_post WHERE user_id IN (900101, 900102, 900103);
DELETE FROM sys_user WHERE user_id IN (900101, 900102, 900103);

INSERT INTO sys_user VALUES
(900101, '000000', 103, 'entry01', '测试填报员', 'sys_user', 'entry01@example.com', '13800000001', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), NULL, NULL, '测试用户，密码666666'),
(900102, '000000', 103, 'audit01', '测试审核员', 'sys_user', 'audit01@example.com', '13800000002', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), NULL, NULL, '测试用户，密码666666'),
(900103, '000000', 103, 'report01', '测试报表查看员', 'sys_user', 'report01@example.com', '13800000003', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), NULL, NULL, '测试用户，密码666666');

INSERT INTO sys_user_role VALUES
(900101, 900002),
(900102, 900003),
(900103, 900004);

INSERT INTO sys_user_post VALUES
(900101, 2),
(900102, 2),
(900103, 2);

INSERT INTO ce_admin_division (division_code, division_name, parent_code, division_level, remark) VALUES
('310000', '上海市', NULL, 'province', 'test-data'),
('310100', '上海市市辖区', '310000', 'city', 'test-data');

INSERT INTO ce_company_factory
(company_sk, company_code, factory_code, company_name, factory_name, province_code, province_name, factory_type, industry_section_code, industry_section_name, effective_date, is_active, remark)
VALUES
('TEST-COMPANY-001', 'C001', 'F001', '测试制造企业', '上海一厂', '310000', '上海市', '制造工厂', 'C', '制造业', '2026-01-01', 1, 'test-data');

INSERT INTO ce_base_year (factory_code, factory_name, base_year, enabled_flag, remark) VALUES
('F001', '上海一厂', 2025, 1, 'test-data');

INSERT INTO ce_emission_source_category
(category_sk, business_key, ghg_scope, ghg_scope_category_sort, ghg_scope_category, gb_scope_category, gb_subcategory, effective_date, is_current, version_no, unified_standard_category, remark)
VALUES
('CAT-FUEL', 'fuel-combustion', 'Scope 1', 1, '固定燃烧', '范围一', '固定燃烧', '2026-01-01', 1, '2026.1', '固定燃烧', 'test-data'),
('CAT-POWER', 'purchased-electricity', 'Scope 2', 2, '外购电力', '范围二', '外购电力', '2026-01-01', 1, '2026.1', '外购电力', 'test-data');

INSERT INTO ce_ef_factor
(factor_sk, emission_source_name, emission_source_name_en, fuel_material_category, source_unit, co2, ch4, n2o, applicable_scope, factor_source, gwp_ch4, gwp_n2o, factor_gwp, factor_unit, remark)
VALUES
('EF-NG-001', '天然气燃烧', 'Natural gas combustion', '天然气', 'Nm3', 2.1622000000, 0.0001000000, 0.0001000000, '固定燃烧', '测试因子库', 28, 265, 2.1915000000, 'kgCO2e/Nm3', 'test-data'),
('EF-POWER-SH-2026', '上海电网电力', 'Shanghai grid electricity', '电力', 'kWh', 0.5703000000, NULL, NULL, '外购电力', '测试因子库', NULL, NULL, 0.5703000000, 'kgCO2e/kWh', 'test-data');

INSERT INTO ce_electricity_factor
(version_province_code, factor_version, division_code, division_name, region_name, province_factor, region_factor, national_factor, remark)
VALUES
('2026-SH', '2026.1', '310000', '上海市', '华东', 0.5703000000, 0.5800000000, 0.5942000000, 'test-data');

INSERT INTO ce_electricity_factor_version_map (effective_year, factor_version, remark) VALUES
(2026, '2026.1', 'test-data');

INSERT INTO ce_electricity_factor_scope (scope_key, scope_name, remark) VALUES
('market-based', '市场化口径', 'test-data'),
('location-based', '地域口径', 'test-data');

INSERT INTO ce_greenhouse_gas (gas_code, gas_name, gas_name_en, remark) VALUES
('CO2', '二氧化碳', 'Carbon dioxide', 'test-data'),
('CH4', '甲烷', 'Methane', 'test-data'),
('N2O', '氧化亚氮', 'Nitrous oxide', 'test-data');

INSERT INTO ce_fuel_factor_calc
(calc_key, fuel_material_category, lower_heat_value, lower_heat_value_unit, carbon_content, carbon_content_unit, oxidation_rate, co2_factor, factor_unit, factor_source, enabled_flag, remark)
VALUES
('CALC-NG-001', '天然气', 389.3100000000, 'kJ/Nm3', 15.3000000000, 'tC/TJ', 0.9900000000, 2.1622000000, 'kgCO2/Nm3', '测试因子库', 1, 'test-data');

INSERT INTO ce_emission_source
(company_code, company_name, factory_code, factory_name, source_category_key, scope_name, scope_subcategory, source_identification_code, source_identification_name, emission_source_name, source_unit, responsible_dept, data_source, factor_key, enabled_flag, remark)
VALUES
('C001', '测试制造企业', 'F001', '上海一厂', 'CAT-FUEL', 'Scope 1', '固定燃烧', 'SRC-NG-BOILER', '天然气锅炉', '天然气燃烧', 'Nm3', '能源管理部', '手工填报', 'EF-NG-001', 1, 'test-data'),
('C001', '测试制造企业', 'F001', '上海一厂', 'CAT-POWER', 'Scope 2', '外购电力', 'SRC-GRID-POWER', '外购电力', '上海电网电力', 'kWh', '能源管理部', '手工填报', 'EF-POWER-SH-2026', 1, 'test-data');

INSERT INTO ce_activity_data
(emission_source_id, activity_period, source_sheet_code, source_identification_code, company_code, company_name, factory_code, factory_name, source_category_key, scope_name, scope_subcategory, source_identification_name, emission_source_name, activity_unit, activity_year, activity_month, activity_date, activity_value, responsible_dept, data_source, source_remark, factor_key, data_status, remark)
SELECT id, '2026-01', 'sheet-activity', source_identification_code, company_code, company_name, factory_code, factory_name, source_category_key, scope_name, scope_subcategory, source_identification_name, emission_source_name, source_unit, 2026, 1, '2026-01-31',
       CASE source_identification_code WHEN 'SRC-NG-BOILER' THEN 12000.0000000000 ELSE 850000.0000000000 END,
       responsible_dept, '测试数据', '一月测试填报', factor_key, 'submitted', 'test-data'
FROM ce_emission_source
WHERE remark = 'test-data';

INSERT INTO ce_green_power_certificate
(factory_code, factory_name, activity_year, activity_month, source_category_key, scope_name, scope_subcategory, electricity_type, electricity_type_desc, quantity_kwh, certificate_code, issuing_org, purchase_date, expiry_date, power_grid_region, offset_power_source, data_source, source_remark, emission_source_name, factor_key, proof_status, remark)
VALUES
('F001', '上海一厂', 2026, 1, 'CAT-POWER', 'Scope 2', '绿电绿证抵扣', 'green-certificate', '绿色电力证书', 100000.0000000000, 'GEC-TEST-2026-001', '测试签发机构', '2026-01-15', '2027-01-14', '华东电网', '外购电力', '测试数据', '一月绿证抵扣', '上海电网电力', 'EF-POWER-SH-2026', 'verified', 'test-data');

INSERT INTO ce_intensity_denominator_rule
(denominator_rule_key, factory_type, denominator_type, denominator_metric_name, intensity_unit_display, enabled_flag, remark)
VALUES
('RULE-MFG-OUTPUT', '制造工厂', '产量', '产品产量', 'tCO2e/吨产品', 1, 'test-data');

INSERT INTO ce_intensity_target (factory_type, target_year, target_value, unit_name, remark) VALUES
('制造工厂', 2026, 0.8500000000, 'tCO2e/吨产品', 'test-data');

INSERT INTO ce_intensity_tolerance (tolerance_key, industry_section, tolerance_rate, enabled_flag, remark) VALUES
('TOL-MFG-2026', '制造业', 0.050000, 1, 'test-data');

INSERT INTO ce_intensity_denominator_fact
(source_sheet_code, factory_code, factory_name, factory_type, fact_year, fact_month, denominator_type, denominator_metric_name, denominator_value, unit_name, data_source, remark)
VALUES
('503', 'F001', '上海一厂', '制造工厂', 2026, 1, '产量', '产品产量', 1500.0000000000, '吨', '测试数据', 'test-data');

INSERT INTO ce_intensity_metric
(denominator_fact_id, factory_code, factory_name, metric_year, metric_month, numerator_value, denominator_value, intensity_value, unit_name, metric_status, remark)
SELECT id, factory_code, factory_name, fact_year, fact_month, 530.0000000000, denominator_value, 530.0000000000 / denominator_value, 'tCO2e/吨产品', 'calculated', 'test-data'
FROM ce_intensity_denominator_fact
WHERE remark = 'test-data';

INSERT INTO ce_license_state
(license_id, customer_id, package_id, package_name, install_id, key_id, algorithm, schema_version, valid_from, valid_to, last_verified_time, max_observed_time, feature_codes, payload_digest, current_summary, license_status)
VALUES
('LIC-TEST-2026', 'CUST-TEST-001', 1, '测试套餐', 'INSTALL-TEST-001', 'test-key-2026', 'SHA256withRSA', '1.0', '2026-01-01 00:00:00', '2026-12-31 23:59:59', NOW(), NOW(), 'enterprise:factorSync,enterprise:reportTemplateSync', 'TEST-DIGEST', '测试授权，覆盖企业端同步与报表模板能力', 'VALID');

INSERT INTO ce_factor_confirmation
(factor_code, factor_name, factor_version_code, factor_unit, factor_value, confirmation_status, confirmed_by, confirmed_time, license_id, remark)
VALUES
('EF-NG-001', '天然气燃烧', '2026.1', 'kgCO2e/Nm3', 2.1915000000, 'confirmed', 'audit01', NOW(), 'LIC-TEST-2026', 'test-data'),
('EF-POWER-SH-2026', '上海电网电力', '2026.1', 'kgCO2e/kWh', 0.5703000000, 'confirmed', 'audit01', NOW(), 'LIC-TEST-2026', 'test-data');

INSERT INTO ce_factor_cache_version (vendor_version_id, license_id, version_code, frozen_flag, synced_time) VALUES
('VENDOR-FACTOR-TEST-2026', 'LIC-TEST-2026', '2026.1', 1, NOW());

INSERT INTO ce_factor_cache_record
(cache_version_id, factor_table_code, factor_code, factor_name, factor_category, factor_value, factor_unit, factor_key, emission_source_name, source_unit, factor_gwp, enabled_flag)
SELECT id, '201ef', 'TEST-EF-NG-001', '天然气燃烧', '燃料燃烧', 2.1915000000, 'kgCO2e/Nm3', 'EF-NG-001', '天然气燃烧', 'Nm3', 2.1915000000, 1
FROM ce_factor_cache_version
WHERE vendor_version_id = 'VENDOR-FACTOR-TEST-2026';

INSERT INTO ce_report_template_file
(template_code, template_name, template_type, file_name, file_path, enabled_flag, remark)
VALUES
('TEST_MONTHLY_REPORT', '测试月度碳数据报表模板', 'monthly', 'test-monthly-carbon-report.xlsx', 'enterprise/report-templates/test-monthly-carbon-report.xlsx', 1, 'test-data');
