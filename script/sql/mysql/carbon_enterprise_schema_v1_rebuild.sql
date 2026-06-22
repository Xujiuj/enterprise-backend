-- Local-only enterprise schema rebuild for customer sample table refactor.
-- This script intentionally removes legacy and current enterprise carbon tables
-- before reloading carbon_enterprise_schema_v1.sql. Run only against local
-- development database `enterprise`.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ce_extension_field_value;
DROP TABLE IF EXISTS ce_factor_cache_record;
DROP TABLE IF EXISTS ce_factor_cache_version;
DROP TABLE IF EXISTS ce_license_state;
DROP TABLE IF EXISTS ce_report_template_file;
DROP TABLE IF EXISTS ce_intensity_metric;
DROP TABLE IF EXISTS ce_intensity_tolerance;
DROP TABLE IF EXISTS ce_intensity_denominator_fact;
DROP TABLE IF EXISTS ce_intensity_target;
DROP TABLE IF EXISTS ce_intensity_denominator_rule;
DROP TABLE IF EXISTS ce_green_power_certificate;
DROP TABLE IF EXISTS ce_activity_data;
DROP TABLE IF EXISTS ce_emission_source;
DROP TABLE IF EXISTS ce_greenhouse_gas;
DROP TABLE IF EXISTS ce_electricity_factor_scope;
DROP TABLE IF EXISTS ce_fuel_factor_calc;
DROP TABLE IF EXISTS ce_electricity_factor_version_map;
DROP TABLE IF EXISTS ce_electricity_factor;
DROP TABLE IF EXISTS ce_ef_factor;
DROP TABLE IF EXISTS ce_base_year;
DROP TABLE IF EXISTS ce_emission_source_category;
DROP TABLE IF EXISTS ce_company_factory;
DROP TABLE IF EXISTS ce_admin_division;
DROP TABLE IF EXISTS ce_dimension_record;
DROP TABLE IF EXISTS ce_capture_cell;
DROP TABLE IF EXISTS ce_capture_row;
DROP TABLE IF EXISTS ce_capture_batch;
DROP TABLE IF EXISTS ce_extension_field;
DROP TABLE IF EXISTS ce_template_field;
DROP TABLE IF EXISTS ce_template_sheet;
DROP TABLE IF EXISTS ce_template_version;

SET FOREIGN_KEY_CHECKS = 1;

SOURCE enterprise-backend/script/sql/mysql/carbon_enterprise_schema_v1.sql;
