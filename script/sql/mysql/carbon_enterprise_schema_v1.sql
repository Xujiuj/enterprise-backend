-- Enterprise carbon data foundation, MySQL development version.
-- This script keeps enterprise-local business data inside enterprise-backend.
-- SQL Server migration notes:
--   * BIGINT AUTO_INCREMENT -> BIGINT IDENTITY(1,1)
--   * TEXT -> NVARCHAR(MAX)
--   * TINYINT(1) -> BIT
--   * DATETIME -> DATETIME2

CREATE TABLE IF NOT EXISTS ce_template_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version_code VARCHAR(64) NOT NULL,
    version_name VARCHAR(128) NOT NULL,
    source_dir VARCHAR(512) NOT NULL,
    workbook_count INT NOT NULL DEFAULT 0,
    sheet_count INT NOT NULL DEFAULT 0,
    field_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    imported_by VARCHAR(64) DEFAULT NULL,
    imported_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_template_version_code (version_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise Excel template version inventory';

CREATE TABLE IF NOT EXISTS ce_template_sheet (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_version_id BIGINT NOT NULL,
    source_file VARCHAR(512) NOT NULL,
    source_group VARCHAR(128) NOT NULL,
    sheet_name VARCHAR(255) NOT NULL,
    sheet_type VARCHAR(64) NOT NULL,
    header_row INT NOT NULL DEFAULT 0,
    field_count INT NOT NULL DEFAULT 0,
    module_code VARCHAR(64) NOT NULL,
    target_table_code VARCHAR(128) NOT NULL,
    allow_extension TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_template_sheet (template_version_id, target_table_code),
    KEY idx_ce_template_sheet_module (module_code),
    CONSTRAINT fk_ce_template_sheet_version
        FOREIGN KEY (template_version_id) REFERENCES ce_template_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise source workbook sheet inventory';

CREATE TABLE IF NOT EXISTS ce_template_field (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sheet_id BIGINT NOT NULL,
    field_order INT NOT NULL,
    original_field_name VARCHAR(255) NOT NULL,
    target_column_code VARCHAR(64) NOT NULL,
    value_type VARCHAR(32) NOT NULL DEFAULT 'text',
    required_flag TINYINT(1) NOT NULL DEFAULT 0,
    original_field_flag TINYINT(1) NOT NULL DEFAULT 1,
    extensible_flag TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_template_field (sheet_id, field_order),
    KEY idx_ce_template_field_column (target_column_code),
    CONSTRAINT fk_ce_template_field_sheet
        FOREIGN KEY (sheet_id) REFERENCES ce_template_sheet (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise original field preservation inventory';

CREATE TABLE IF NOT EXISTS ce_capture_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_version_id BIGINT NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    source_mode VARCHAR(32) NOT NULL,
    batch_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    validation_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    submitted_by VARCHAR(64) DEFAULT NULL,
    submitted_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_capture_batch_module (module_code, batch_status),
    CONSTRAINT fk_ce_capture_batch_version
        FOREIGN KEY (template_version_id) REFERENCES ce_template_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local data capture batch';

CREATE TABLE IF NOT EXISTS ce_capture_row (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    sheet_id BIGINT NOT NULL,
    source_row_no INT NOT NULL DEFAULT 0,
    row_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    validation_level VARCHAR(32) NOT NULL DEFAULT 'none',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ce_capture_row_batch (batch_id, sheet_id),
    CONSTRAINT fk_ce_capture_row_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id),
    CONSTRAINT fk_ce_capture_row_sheet
        FOREIGN KEY (sheet_id) REFERENCES ce_template_sheet (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local data capture row';

CREATE TABLE IF NOT EXISTS ce_capture_cell (
    id BIGINT NOT NULL AUTO_INCREMENT,
    row_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    text_value TEXT DEFAULT NULL,
    decimal_value DECIMAL(28, 10) DEFAULT NULL,
    date_value DATETIME DEFAULT NULL,
    value_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_capture_cell (row_id, field_id),
    KEY idx_ce_capture_cell_field (field_id),
    CONSTRAINT fk_ce_capture_cell_row
        FOREIGN KEY (row_id) REFERENCES ce_capture_row (id),
    CONSTRAINT fk_ce_capture_cell_field
        FOREIGN KEY (field_id) REFERENCES ce_template_field (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local data capture cell';

CREATE TABLE IF NOT EXISTS ce_extension_field (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_version_id BIGINT NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    sheet_id BIGINT NOT NULL,
    field_code VARCHAR(64) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    value_type VARCHAR(32) NOT NULL DEFAULT 'text',
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_extension_field (template_version_id, sheet_id, field_code),
    KEY idx_ce_extension_field_module (module_code),
    CONSTRAINT fk_ce_extension_field_version
        FOREIGN KEY (template_version_id) REFERENCES ce_template_version (id),
    CONSTRAINT fk_ce_extension_field_sheet
        FOREIGN KEY (sheet_id) REFERENCES ce_template_sheet (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise allowed extension fields';

CREATE TABLE IF NOT EXISTS ce_admin_division (
    id BIGINT NOT NULL AUTO_INCREMENT,
    division_code VARCHAR(64) NOT NULL,
    division_name VARCHAR(255) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_admin_division_code (division_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='101 administrative division';

CREATE TABLE IF NOT EXISTS ce_company_factory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_sk VARCHAR(64) NOT NULL,
    company_code VARCHAR(64) NOT NULL,
    factory_code VARCHAR(64) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    factory_name VARCHAR(255) NOT NULL,
    province_code VARCHAR(64) DEFAULT NULL,
    province_name VARCHAR(255) DEFAULT NULL,
    factory_type VARCHAR(128) DEFAULT NULL,
    industry_section_code VARCHAR(64) DEFAULT NULL,
    industry_section_name VARCHAR(255) DEFAULT NULL,
    industry_division_code VARCHAR(64) DEFAULT NULL,
    industry_division_name VARCHAR(255) DEFAULT NULL,
    industry_group_code VARCHAR(64) DEFAULT NULL,
    industry_group_name VARCHAR(255) DEFAULT NULL,
    industry_class_code VARCHAR(64) DEFAULT NULL,
    industry_class_name VARCHAR(255) DEFAULT NULL,
    effective_date DATE DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,
    is_active CHAR(1) NOT NULL DEFAULT 'Y',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_company_factory (company_code, factory_code),
    UNIQUE KEY uk_ce_company_factory_factory (factory_code),
    KEY idx_ce_company_factory_type (factory_type),
    KEY idx_ce_company_factory_province (province_code),
    CONSTRAINT fk_ce_company_factory_province
        FOREIGN KEY (province_code) REFERENCES ce_admin_division (division_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='102 company and factory';

CREATE TABLE IF NOT EXISTS ce_emission_source_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_sk VARCHAR(64) NOT NULL,
    business_key VARCHAR(64) NOT NULL,
    ghg_scope VARCHAR(128) DEFAULT NULL,
    ghg_scope_category_sort INT DEFAULT NULL,
    ghg_scope_category VARCHAR(255) DEFAULT NULL,
    ghg_scope_en VARCHAR(128) DEFAULT NULL,
    ghg_scope_category_en VARCHAR(255) DEFAULT NULL,
    iso_category VARCHAR(128) DEFAULT NULL,
    iso_category_en VARCHAR(128) DEFAULT NULL,
    iso_category_description VARCHAR(500) DEFAULT NULL,
    iso_category_description_en VARCHAR(500) DEFAULT NULL,
    iso_custom_subcategory VARCHAR(255) DEFAULT NULL,
    gb_scope_category VARCHAR(255) DEFAULT NULL,
    gb_subcategory VARCHAR(255) DEFAULT NULL,
    effective_date DATE DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,
    is_current CHAR(1) NOT NULL DEFAULT 'Y',
    version_no VARCHAR(64) DEFAULT NULL,
    unified_standard_category VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_emission_source_category_sk (category_sk),
    UNIQUE KEY uk_ce_emission_source_category (business_key, version_no),
    KEY idx_ce_emission_source_category_scope (ghg_scope, ghg_scope_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='103 emission source category';

CREATE TABLE IF NOT EXISTS ce_base_year (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factory_code VARCHAR(64) NOT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    base_year INT NOT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_base_year_factory (factory_code, base_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='106 base year dimension';

CREATE TABLE IF NOT EXISTS ce_ef_factor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factor_sk VARCHAR(64) NOT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    emission_source_name_en VARCHAR(255) DEFAULT NULL,
    fuel_material_category VARCHAR(255) DEFAULT NULL,
    source_unit VARCHAR(64) DEFAULT NULL,
    co2 DECIMAL(28, 10) DEFAULT NULL,
    ch4 DECIMAL(28, 10) DEFAULT NULL,
    n2o DECIMAL(28, 10) DEFAULT NULL,
    hfcs DECIMAL(28, 10) DEFAULT NULL,
    pfcs DECIMAL(28, 10) DEFAULT NULL,
    sf6 DECIMAL(28, 10) DEFAULT NULL,
    nf3 DECIMAL(28, 10) DEFAULT NULL,
    applicable_scope VARCHAR(255) DEFAULT NULL,
    factor_source VARCHAR(255) DEFAULT NULL,
    gwp_ch4 DECIMAL(28, 10) DEFAULT NULL,
    gwp_n2o DECIMAL(28, 10) DEFAULT NULL,
    gwp_hfcs DECIMAL(28, 10) DEFAULT NULL,
    gwp_pfcs DECIMAL(28, 10) DEFAULT NULL,
    gwp_sf6 DECIMAL(28, 10) DEFAULT NULL,
    gwp_nf3 DECIMAL(28, 10) DEFAULT NULL,
    factor_gwp DECIMAL(28, 10) DEFAULT NULL,
    factor_unit VARCHAR(128) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_ef_factor_sk (factor_sk),
    KEY idx_ce_ef_factor_source (emission_source_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='201 emission factor dimension';

CREATE TABLE IF NOT EXISTS ce_electricity_factor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version_province_code VARCHAR(64) NOT NULL,
    factor_version VARCHAR(64) NOT NULL,
    division_code VARCHAR(64) DEFAULT NULL,
    division_name VARCHAR(255) DEFAULT NULL,
    region_name VARCHAR(255) DEFAULT NULL,
    province_factor DECIMAL(28, 10) DEFAULT NULL,
    region_factor DECIMAL(28, 10) DEFAULT NULL,
    national_factor DECIMAL(28, 10) DEFAULT NULL,
    non_fossil_excluded_factor DECIMAL(28, 10) DEFAULT NULL,
    national_fossil_power_factor DECIMAL(28, 10) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_electricity_factor (version_province_code),
    KEY idx_ce_electricity_factor_version (factor_version, division_code),
    CONSTRAINT fk_ce_electricity_factor_division
        FOREIGN KEY (division_code) REFERENCES ce_admin_division (division_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='202 electricity factor dimension';

CREATE TABLE IF NOT EXISTS ce_electricity_factor_version_map (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factor_version VARCHAR(64) NOT NULL,
    effective_year INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_electricity_factor_version_map (factor_version, effective_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='203 electricity factor version map';

CREATE TABLE IF NOT EXISTS ce_fuel_factor_calc (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calc_key VARCHAR(64) NOT NULL,
    fuel_material_category VARCHAR(255) DEFAULT NULL,
    lower_heat_value DECIMAL(28, 10) DEFAULT NULL,
    lower_heat_value_unit VARCHAR(64) DEFAULT NULL,
    carbon_content DECIMAL(28, 10) DEFAULT NULL,
    carbon_content_unit VARCHAR(64) DEFAULT NULL,
    oxidation_rate DECIMAL(18, 10) DEFAULT NULL,
    co2_factor DECIMAL(28, 10) DEFAULT NULL,
    factor_unit VARCHAR(128) DEFAULT NULL,
    factor_source VARCHAR(255) DEFAULT NULL,
    effective_date DATE DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_fuel_factor_calc (calc_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='204 fuel factor calculation';

CREATE TABLE IF NOT EXISTS ce_electricity_factor_scope (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scope_key VARCHAR(64) NOT NULL,
    scope_name VARCHAR(255) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_electricity_factor_scope (scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='205 electricity factor scope';

CREATE TABLE IF NOT EXISTS ce_greenhouse_gas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gas_code VARCHAR(64) NOT NULL,
    gas_name VARCHAR(128) NOT NULL,
    gas_name_en VARCHAR(128) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_greenhouse_gas_code (gas_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='206 greenhouse gas dimension';

CREATE TABLE IF NOT EXISTS ce_emission_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_code VARCHAR(64) NOT NULL,
    company_name VARCHAR(255) DEFAULT NULL,
    factory_code VARCHAR(64) DEFAULT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    source_category_key VARCHAR(64) NOT NULL,
    scope_name VARCHAR(128) DEFAULT NULL,
    scope_subcategory VARCHAR(255) DEFAULT NULL,
    source_identification_code VARCHAR(64) NOT NULL,
    source_identification_name VARCHAR(255) DEFAULT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    responsible_dept VARCHAR(255) DEFAULT NULL,
    data_source VARCHAR(255) DEFAULT NULL,
    factor_key VARCHAR(64) DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_emission_source_factory_code (company_code, source_identification_code),
    KEY idx_ce_emission_source_company (company_code),
    KEY idx_ce_emission_source_factory (factory_code),
    KEY idx_ce_emission_source_code (source_identification_code),
    KEY idx_ce_emission_source_category (source_category_key),
    KEY idx_ce_emission_source_factor (factor_key),
    CONSTRAINT fk_ce_emission_source_factory
        FOREIGN KEY (company_code) REFERENCES ce_company_factory (factory_code),
    CONSTRAINT fk_ce_emission_source_category
        FOREIGN KEY (source_category_key) REFERENCES ce_emission_source_category (category_sk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='104 emission source identification';

CREATE TABLE IF NOT EXISTS ce_activity_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT DEFAULT NULL,
    emission_source_id BIGINT DEFAULT NULL,
    activity_period VARCHAR(32) DEFAULT NULL,
    source_sheet_code VARCHAR(64) DEFAULT NULL,
    source_identification_code VARCHAR(64) NOT NULL,
    company_code VARCHAR(64) NOT NULL,
    company_name VARCHAR(255) DEFAULT NULL,
    factory_code VARCHAR(64) DEFAULT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    source_category_key VARCHAR(64) DEFAULT NULL,
    scope_name VARCHAR(128) DEFAULT NULL,
    scope_subcategory VARCHAR(255) DEFAULT NULL,
    source_identification_name VARCHAR(255) DEFAULT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    activity_unit VARCHAR(64) DEFAULT NULL,
    activity_year INT DEFAULT NULL,
    activity_month INT DEFAULT NULL,
    activity_date DATE DEFAULT NULL,
    activity_value DECIMAL(28, 10) DEFAULT NULL,
    responsible_dept VARCHAR(255) DEFAULT NULL,
    data_source VARCHAR(255) DEFAULT NULL,
    source_remark VARCHAR(500) DEFAULT NULL,
    factor_key VARCHAR(64) DEFAULT NULL,
    calculated_emission DECIMAL(28, 10) DEFAULT NULL,
    data_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_activity_data_source_period (company_code, source_identification_code, activity_year, activity_month),
    KEY idx_ce_activity_data_period (activity_period, data_status),
    KEY idx_ce_activity_data_source (source_identification_code),
    KEY idx_ce_activity_data_emission_source_id (emission_source_id),
    KEY idx_ce_activity_data_company (company_code),
    CONSTRAINT fk_ce_activity_data_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id),
    CONSTRAINT fk_ce_activity_data_source_id
        FOREIGN KEY (emission_source_id) REFERENCES ce_emission_source (id),
    CONSTRAINT fk_ce_activity_data_company
        FOREIGN KEY (company_code) REFERENCES ce_company_factory (factory_code),
    CONSTRAINT fk_ce_activity_data_category
        FOREIGN KEY (source_category_key) REFERENCES ce_emission_source_category (category_sk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='3 activity data';

CREATE TABLE IF NOT EXISTS ce_green_power_certificate (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factory_code VARCHAR(64) NOT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    activity_year INT DEFAULT NULL,
    activity_month INT DEFAULT NULL,
    source_category_key VARCHAR(64) DEFAULT NULL,
    scope_name VARCHAR(128) DEFAULT NULL,
    scope_subcategory VARCHAR(255) DEFAULT NULL,
    electricity_type VARCHAR(128) DEFAULT NULL,
    electricity_type_desc VARCHAR(255) DEFAULT NULL,
    quantity_kwh DECIMAL(28, 10) DEFAULT NULL,
    certificate_code VARCHAR(128) DEFAULT NULL,
    issuing_org VARCHAR(255) DEFAULT NULL,
    purchase_date DATE DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,
    power_grid_region VARCHAR(255) DEFAULT NULL,
    offset_power_source VARCHAR(255) DEFAULT NULL,
    data_source VARCHAR(255) DEFAULT NULL,
    source_remark VARCHAR(500) DEFAULT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    factor_key VARCHAR(64) DEFAULT NULL,
    proof_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_green_power_factory (factory_code),
    KEY idx_ce_green_power_period (activity_year, activity_month, proof_status),
    KEY idx_ce_green_power_certificate (certificate_code),
    KEY idx_ce_green_power_category (source_category_key),
    KEY idx_ce_green_power_factor (factor_key),
    CONSTRAINT fk_ce_green_power_factory
        FOREIGN KEY (factory_code) REFERENCES ce_company_factory (factory_code),
    CONSTRAINT fk_ce_green_power_category
        FOREIGN KEY (source_category_key) REFERENCES ce_emission_source_category (category_sk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='105 green power certificate activity data';

CREATE TABLE IF NOT EXISTS ce_intensity_denominator_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    denominator_rule_key VARCHAR(64) NOT NULL,
    factory_type VARCHAR(128) NOT NULL,
    denominator_type VARCHAR(128) NOT NULL,
    denominator_metric_name VARCHAR(255) NOT NULL,
    intensity_unit_display VARCHAR(128) DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_denominator_rule (denominator_rule_key),
    KEY idx_ce_intensity_denominator_rule_type (factory_type, denominator_type, enabled_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='501 intensity denominator rule';

CREATE TABLE IF NOT EXISTS ce_intensity_target (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factory_type VARCHAR(128) NOT NULL,
    target_year INT NOT NULL,
    target_value DECIMAL(28, 10) NOT NULL,
    unit_name VARCHAR(128) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_target (factory_type, target_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='502 intensity target';

CREATE TABLE IF NOT EXISTS ce_intensity_denominator_fact (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT DEFAULT NULL,
    source_sheet_code VARCHAR(64) DEFAULT NULL,
    factory_code VARCHAR(64) NOT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    factory_type VARCHAR(128) DEFAULT NULL,
    fact_year INT NOT NULL,
    fact_month INT DEFAULT NULL,
    denominator_type VARCHAR(128) NOT NULL,
    denominator_metric_name VARCHAR(255) NOT NULL,
    denominator_value DECIMAL(28, 10) NOT NULL,
    unit_name VARCHAR(128) DEFAULT NULL,
    data_source VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_denominator_fact_period (fact_year, fact_month),
    KEY idx_ce_denominator_fact_factory (factory_code),
    CONSTRAINT fk_ce_denominator_fact_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id),
    CONSTRAINT fk_ce_denominator_fact_factory
        FOREIGN KEY (factory_code) REFERENCES ce_company_factory (factory_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='503 denominator fact';

CREATE TABLE IF NOT EXISTS ce_intensity_tolerance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tolerance_key VARCHAR(64) NOT NULL,
    industry_section VARCHAR(255) NOT NULL,
    tolerance_rate DECIMAL(18, 10) NOT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_tolerance (tolerance_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='504 intensity tolerance';

CREATE TABLE IF NOT EXISTS ce_intensity_metric (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metric_code VARCHAR(64) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    rule_code VARCHAR(64) DEFAULT NULL,
    metric_period VARCHAR(32) NOT NULL,
    numerator_emission DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_fact_id BIGINT DEFAULT NULL,
    denominator_value DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_unit VARCHAR(64) NOT NULL,
    intensity_value DECIMAL(28, 10) DEFAULT NULL,
    target_code VARCHAR(64) DEFAULT NULL,
    metric_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_metric (metric_code, metric_period),
    KEY idx_ce_intensity_metric_rule (rule_code),
    KEY idx_ce_intensity_metric_status (metric_status),
    CONSTRAINT fk_ce_intensity_metric_denominator_fact
        FOREIGN KEY (denominator_fact_id) REFERENCES ce_intensity_denominator_fact (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local carbon intensity metric result';

CREATE TABLE IF NOT EXISTS ce_report_template_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_report_template_file (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local report template download catalog';

CREATE TABLE IF NOT EXISTS ce_report_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    directory_no INT DEFAULT NULL,
    directory_name VARCHAR(255) DEFAULT NULL,
    subdirectory_no INT DEFAULT NULL,
    subdirectory_name VARCHAR(255) DEFAULT NULL,
    chart_names TEXT DEFAULT NULL,
    display_order INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_report_content_order (display_order),
    KEY idx_ce_report_content_directory (directory_no, subdirectory_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise report Content catalog';

CREATE TABLE IF NOT EXISTS ce_license_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    license_id VARCHAR(128) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    package_id BIGINT DEFAULT NULL,
    package_name VARCHAR(64) DEFAULT NULL,
    install_id VARCHAR(128) NOT NULL,
    key_id VARCHAR(64) NOT NULL,
    algorithm VARCHAR(64) NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    last_verified_time DATETIME DEFAULT NULL,
    max_observed_time DATETIME DEFAULT NULL,
    feature_codes TEXT DEFAULT NULL,
    payload_digest VARCHAR(128) DEFAULT NULL,
    current_summary VARCHAR(1024) DEFAULT NULL,
    license_status VARCHAR(32) NOT NULL DEFAULT 'VALID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_license_state_license (license_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local license runtime state';

CREATE TABLE IF NOT EXISTS ce_factor_confirmation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factor_code VARCHAR(128) NOT NULL,
    factor_name VARCHAR(255) NOT NULL,
    factor_version_code VARCHAR(64) NOT NULL,
    factor_unit VARCHAR(64) NOT NULL,
    factor_value DECIMAL(28, 10) NOT NULL,
    confirmation_status VARCHAR(32) DEFAULT NULL,
    confirmed_by VARCHAR(128) DEFAULT NULL,
    confirmed_time DATETIME DEFAULT NULL,
    license_id VARCHAR(128) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_factor_confirmation (factor_code, factor_version_code, license_id),
    KEY idx_ce_factor_confirmation_status (confirmation_status),
    KEY idx_ce_factor_confirmation_license (license_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local emission factor confirmation';

CREATE TABLE IF NOT EXISTS ce_factor_cache_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vendor_version_id VARCHAR(128) NOT NULL,
    license_id VARCHAR(128) NOT NULL,
    version_code VARCHAR(64) NOT NULL,
    frozen_flag TINYINT(1) NOT NULL DEFAULT 0,
    synced_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_factor_cache_version (vendor_version_id, license_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local factor cache version';

CREATE TABLE IF NOT EXISTS ce_factor_cache_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cache_version_id BIGINT NOT NULL,
    factor_table_code VARCHAR(64) NOT NULL DEFAULT '201ef',
    factor_code VARCHAR(128) NOT NULL,
    factor_name VARCHAR(255) NOT NULL,
    factor_category VARCHAR(128) NOT NULL,
    factor_value DECIMAL(28, 10) NOT NULL,
    factor_unit VARCHAR(64) NOT NULL,
    factor_key VARCHAR(64) DEFAULT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    emission_source_name_en VARCHAR(255) DEFAULT NULL,
    fuel_material_category VARCHAR(255) DEFAULT NULL,
    source_unit VARCHAR(64) DEFAULT NULL,
    co2 DECIMAL(28, 10) DEFAULT NULL,
    ch4 DECIMAL(28, 10) DEFAULT NULL,
    n2o DECIMAL(28, 10) DEFAULT NULL,
    hfcs DECIMAL(28, 10) DEFAULT NULL,
    pfcs DECIMAL(28, 10) DEFAULT NULL,
    sf6 DECIMAL(28, 10) DEFAULT NULL,
    nf3 DECIMAL(28, 10) DEFAULT NULL,
    applicable_scope VARCHAR(255) DEFAULT NULL,
    factor_source VARCHAR(512) DEFAULT NULL,
    gwp_ch4 DECIMAL(28, 10) DEFAULT NULL,
    gwp_n2o DECIMAL(28, 10) DEFAULT NULL,
    gwp_hfcs DECIMAL(28, 10) DEFAULT NULL,
    gwp_pfcs DECIMAL(28, 10) DEFAULT NULL,
    gwp_sf6 DECIMAL(28, 10) DEFAULT NULL,
    gwp_nf3 DECIMAL(28, 10) DEFAULT NULL,
    factor_gwp DECIMAL(28, 10) DEFAULT NULL,
    version_province_code VARCHAR(128) DEFAULT NULL,
    factor_version VARCHAR(64) DEFAULT NULL,
    division_code VARCHAR(64) DEFAULT NULL,
    division_name VARCHAR(128) DEFAULT NULL,
    region_name VARCHAR(128) DEFAULT NULL,
    province_factor DECIMAL(28, 10) DEFAULT NULL,
    region_factor DECIMAL(28, 10) DEFAULT NULL,
    national_factor DECIMAL(28, 10) DEFAULT NULL,
    non_fossil_excluded_factor DECIMAL(28, 10) DEFAULT NULL,
    national_fossil_power_factor DECIMAL(28, 10) DEFAULT NULL,
    row_no INT DEFAULT NULL,
    fuel_level1 VARCHAR(255) DEFAULT NULL,
    fuel_level2 VARCHAR(255) DEFAULT NULL,
    fuel_level3 VARCHAR(255) DEFAULT NULL,
    fuel_level4 VARCHAR(255) DEFAULT NULL,
    lower_heat_value DECIMAL(28, 10) DEFAULT NULL,
    lower_heat_value_cv DECIMAL(28, 10) DEFAULT NULL,
    co2_factor DECIMAL(28, 10) DEFAULT NULL,
    co2_factor_cv DECIMAL(28, 10) DEFAULT NULL,
    gwp_value DECIMAL(28, 10) DEFAULT NULL,
    converted_factor DECIMAL(28, 10) DEFAULT NULL,
    source_ref VARCHAR(512) DEFAULT NULL,
    custom_fields TEXT DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    synced_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_factor_cache_record (cache_version_id, factor_table_code, factor_code),
    KEY idx_ce_factor_cache_record_version (cache_version_id),
    KEY idx_ce_factor_cache_record_table (factor_table_code),
    KEY idx_ce_factor_cache_record_code (factor_code),
    CONSTRAINT fk_ce_factor_cache_record_version
        FOREIGN KEY (cache_version_id) REFERENCES ce_factor_cache_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local factor cache record';

CREATE TABLE IF NOT EXISTS ce_extension_field_value (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_table_code VARCHAR(128) NOT NULL,
    owner_record_id BIGINT NOT NULL,
    extension_field_id BIGINT NOT NULL,
    text_value TEXT DEFAULT NULL,
    decimal_value DECIMAL(28, 10) DEFAULT NULL,
    date_value DATETIME DEFAULT NULL,
    boolean_value TINYINT(1) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_extension_field_value (owner_table_code, owner_record_id, extension_field_id),
    KEY idx_ce_extension_field_value_field (extension_field_id),
    CONSTRAINT fk_ce_extension_field_value_field
        FOREIGN KEY (extension_field_id) REFERENCES ce_extension_field (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise extension field value for allowed local forms';

INSERT INTO ce_report_template_file (
    template_code, template_name, template_type, file_name, file_path, enabled_flag, remark
)
SELECT
    'GHG_INVENTORY_V1',
    'Greenhouse gas inventory report template',
    'inventory',
    'greenhouse-gas-inventory-template.xlsx',
    'enterprise/report-templates/greenhouse-gas-inventory-template.xlsx',
    1,
    'Enterprise-side seed template; replace file_path during deployment'
WHERE NOT EXISTS (
    SELECT 1 FROM ce_report_template_file WHERE template_code = 'GHG_INVENTORY_V1'
);
