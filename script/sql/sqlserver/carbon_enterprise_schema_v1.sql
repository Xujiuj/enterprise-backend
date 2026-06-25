-- Enterprise carbon data foundation, SQL Server migration skeleton.
-- Final acceptance target uses SQL Server 2016+.

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'rpt')
    EXEC('CREATE SCHEMA rpt');
GO

CREATE TABLE ce_template_version (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version_code NVARCHAR(64) NOT NULL,
    version_name NVARCHAR(128) NOT NULL,
    source_dir NVARCHAR(512) NOT NULL,
    workbook_count INT NOT NULL DEFAULT 0,
    sheet_count INT NOT NULL DEFAULT 0,
    field_count INT NOT NULL DEFAULT 0,
    status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    imported_by NVARCHAR(64) NULL,
    imported_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_template_version_code UNIQUE (version_code)
);
GO

CREATE TABLE ce_template_sheet (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    template_version_id BIGINT NOT NULL,
    source_file NVARCHAR(512) NOT NULL,
    source_group NVARCHAR(128) NOT NULL,
    sheet_name NVARCHAR(255) NOT NULL,
    sheet_type NVARCHAR(64) NOT NULL,
    header_row INT NOT NULL DEFAULT 0,
    field_count INT NOT NULL DEFAULT 0,
    module_code NVARCHAR(64) NOT NULL,
    target_table_code NVARCHAR(128) NOT NULL,
    allow_extension BIT NOT NULL DEFAULT 0,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_ce_template_sheet UNIQUE (template_version_id, target_table_code),
    CONSTRAINT fk_ce_template_sheet_version
        FOREIGN KEY (template_version_id) REFERENCES ce_template_version (id)
);
GO

CREATE TABLE ce_template_field (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    field_order INT NOT NULL,
    original_field_name NVARCHAR(255) NOT NULL,
    target_column_code NVARCHAR(64) NOT NULL,
    value_type NVARCHAR(32) NOT NULL DEFAULT 'text',
    required_flag BIT NOT NULL DEFAULT 0,
    original_field_flag BIT NOT NULL DEFAULT 1,
    extensible_flag BIT NOT NULL DEFAULT 0,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_ce_template_field UNIQUE (sheet_id, field_order),
    CONSTRAINT fk_ce_template_field_sheet
        FOREIGN KEY (sheet_id) REFERENCES ce_template_sheet (id)
);
GO

CREATE TABLE ce_capture_batch (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    template_version_id BIGINT NOT NULL,
    module_code NVARCHAR(64) NOT NULL,
    source_mode NVARCHAR(32) NOT NULL,
    batch_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    validation_status NVARCHAR(32) NOT NULL DEFAULT 'pending',
    submitted_by NVARCHAR(64) NULL,
    submitted_time DATETIME2 NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT fk_ce_capture_batch_version
        FOREIGN KEY (template_version_id) REFERENCES ce_template_version (id)
);
GO

CREATE TABLE ce_capture_row (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    sheet_id BIGINT NOT NULL,
    source_row_no INT NOT NULL DEFAULT 0,
    row_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    validation_level NVARCHAR(32) NOT NULL DEFAULT 'none',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_ce_capture_row_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id),
    CONSTRAINT fk_ce_capture_row_sheet
        FOREIGN KEY (sheet_id) REFERENCES ce_template_sheet (id)
);
GO

CREATE TABLE ce_capture_cell (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    row_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    text_value NVARCHAR(MAX) NULL,
    decimal_value DECIMAL(28, 10) NULL,
    date_value DATETIME2 NULL,
    value_status NVARCHAR(32) NOT NULL DEFAULT 'pending',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_ce_capture_cell UNIQUE (row_id, field_id),
    CONSTRAINT fk_ce_capture_cell_row
        FOREIGN KEY (row_id) REFERENCES ce_capture_row (id),
    CONSTRAINT fk_ce_capture_cell_field
        FOREIGN KEY (field_id) REFERENCES ce_template_field (id)
);
GO

CREATE TABLE ce_extension_field (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    template_version_id BIGINT NOT NULL,
    module_code NVARCHAR(64) NOT NULL,
    sheet_id BIGINT NOT NULL,
    field_code NVARCHAR(64) NOT NULL,
    field_name NVARCHAR(255) NOT NULL,
    value_type NVARCHAR(32) NOT NULL DEFAULT 'text',
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_ce_extension_field UNIQUE (template_version_id, sheet_id, field_code),
    CONSTRAINT fk_ce_extension_field_version
        FOREIGN KEY (template_version_id) REFERENCES ce_template_version (id),
    CONSTRAINT fk_ce_extension_field_sheet
        FOREIGN KEY (sheet_id) REFERENCES ce_template_sheet (id)
);
GO

CREATE TABLE ce_admin_division (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    division_code NVARCHAR(64) NOT NULL,
    division_name NVARCHAR(255) NOT NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_admin_division_code UNIQUE (division_code)
);
GO

CREATE TABLE ce_company_factory (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    company_sk NVARCHAR(64) NOT NULL,
    company_code NVARCHAR(64) NOT NULL,
    factory_code NVARCHAR(64) NOT NULL,
    company_name NVARCHAR(255) NOT NULL,
    factory_name NVARCHAR(255) NOT NULL,
    province_code NVARCHAR(64) NULL,
    province_name NVARCHAR(255) NULL,
    factory_type NVARCHAR(128) NULL,
    industry_section_code NVARCHAR(64) NULL,
    industry_section_name NVARCHAR(255) NULL,
    industry_division_code NVARCHAR(64) NULL,
    industry_division_name NVARCHAR(255) NULL,
    industry_group_code NVARCHAR(64) NULL,
    industry_group_name NVARCHAR(255) NULL,
    industry_class_code NVARCHAR(64) NULL,
    industry_class_name NVARCHAR(255) NULL,
    effective_date DATE NULL,
    expiry_date DATE NULL,
    is_active NCHAR(1) NOT NULL DEFAULT 'Y',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_company_factory UNIQUE (company_code, factory_code)
);
GO

CREATE INDEX idx_ce_company_factory_type ON ce_company_factory (factory_type);
GO

CREATE TABLE ce_emission_source_category (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    category_sk NVARCHAR(64) NOT NULL,
    business_key NVARCHAR(64) NOT NULL,
    ghg_scope NVARCHAR(128) NULL,
    ghg_scope_category_sort INT NULL,
    ghg_scope_category NVARCHAR(255) NULL,
    ghg_scope_en NVARCHAR(128) NULL,
    ghg_scope_category_en NVARCHAR(255) NULL,
    iso_category NVARCHAR(128) NULL,
    iso_category_en NVARCHAR(128) NULL,
    iso_category_description NVARCHAR(500) NULL,
    iso_category_description_en NVARCHAR(500) NULL,
    iso_custom_subcategory NVARCHAR(255) NULL,
    gb_scope_category NVARCHAR(255) NULL,
    gb_subcategory NVARCHAR(255) NULL,
    effective_date DATE NULL,
    expiry_date DATE NULL,
    is_current NCHAR(1) NOT NULL DEFAULT 'Y',
    version_no NVARCHAR(64) NULL,
    unified_standard_category NVARCHAR(255) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_emission_source_category UNIQUE (business_key, version_no)
);
GO

CREATE TABLE ce_base_year (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    factory_code NVARCHAR(64) NOT NULL,
    factory_name NVARCHAR(255) NULL,
    base_year INT NOT NULL,
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_base_year_factory UNIQUE (factory_code, base_year)
);
GO

CREATE TABLE ce_ef_factor (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    factor_sk NVARCHAR(64) NOT NULL,
    emission_source_name NVARCHAR(255) NULL,
    emission_source_name_en NVARCHAR(255) NULL,
    fuel_material_category NVARCHAR(255) NULL,
    source_unit NVARCHAR(64) NULL,
    co2 DECIMAL(28, 10) NULL,
    ch4 DECIMAL(28, 10) NULL,
    n2o DECIMAL(28, 10) NULL,
    hfcs DECIMAL(28, 10) NULL,
    pfcs DECIMAL(28, 10) NULL,
    sf6 DECIMAL(28, 10) NULL,
    nf3 DECIMAL(28, 10) NULL,
    applicable_scope NVARCHAR(255) NULL,
    factor_source NVARCHAR(255) NULL,
    gwp_ch4 DECIMAL(28, 10) NULL,
    gwp_n2o DECIMAL(28, 10) NULL,
    gwp_hfcs DECIMAL(28, 10) NULL,
    gwp_pfcs DECIMAL(28, 10) NULL,
    gwp_sf6 DECIMAL(28, 10) NULL,
    gwp_nf3 DECIMAL(28, 10) NULL,
    factor_gwp DECIMAL(28, 10) NULL,
    factor_unit NVARCHAR(128) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_ef_factor_sk UNIQUE (factor_sk)
);
GO

CREATE TABLE ce_electricity_factor (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version_province_code NVARCHAR(64) NOT NULL,
    factor_version NVARCHAR(64) NOT NULL,
    division_code NVARCHAR(64) NULL,
    division_name NVARCHAR(255) NULL,
    region_name NVARCHAR(255) NULL,
    province_factor DECIMAL(28, 10) NULL,
    region_factor DECIMAL(28, 10) NULL,
    national_factor DECIMAL(28, 10) NULL,
    non_fossil_excluded_factor DECIMAL(28, 10) NULL,
    national_fossil_power_factor DECIMAL(28, 10) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_electricity_factor UNIQUE (version_province_code)
);
GO

CREATE TABLE ce_electricity_factor_version_map (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    factor_version NVARCHAR(64) NOT NULL,
    effective_year INT NOT NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_electricity_factor_version_map UNIQUE (factor_version, effective_year)
);
GO

CREATE TABLE ce_fuel_factor_calc (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    calc_key NVARCHAR(64) NOT NULL,
    fuel_material_category NVARCHAR(255) NULL,
    lower_heat_value DECIMAL(28, 10) NULL,
    lower_heat_value_unit NVARCHAR(64) NULL,
    carbon_content DECIMAL(28, 10) NULL,
    carbon_content_unit NVARCHAR(64) NULL,
    oxidation_rate DECIMAL(18, 10) NULL,
    co2_factor DECIMAL(28, 10) NULL,
    factor_unit NVARCHAR(128) NULL,
    factor_source NVARCHAR(255) NULL,
    effective_date DATE NULL,
    expiry_date DATE NULL,
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_fuel_factor_calc UNIQUE (calc_key)
);
GO

CREATE TABLE ce_electricity_factor_scope (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    scope_key NVARCHAR(64) NOT NULL,
    scope_name NVARCHAR(255) NOT NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_electricity_factor_scope UNIQUE (scope_key)
);
GO

CREATE TABLE ce_greenhouse_gas (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    gas_code NVARCHAR(64) NOT NULL,
    gas_name NVARCHAR(128) NOT NULL,
    gas_name_en NVARCHAR(128) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_greenhouse_gas_code UNIQUE (gas_code)
);
GO

CREATE TABLE ce_emission_source (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    company_code NVARCHAR(64) NOT NULL,
    company_name NVARCHAR(255) NULL,
    factory_name NVARCHAR(255) NULL,
    source_category_key NVARCHAR(64) NOT NULL,
    scope_name NVARCHAR(128) NULL,
    scope_subcategory NVARCHAR(255) NULL,
    source_identification_code NVARCHAR(64) NOT NULL,
    source_identification_name NVARCHAR(255) NULL,
    emission_source_name NVARCHAR(255) NULL,
    responsible_dept NVARCHAR(255) NULL,
    data_source NVARCHAR(255) NULL,
    factor_key NVARCHAR(64) NULL,
    source_unit NVARCHAR(64) NULL,
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_emission_source_code UNIQUE (source_identification_code)
);
GO

CREATE TABLE ce_activity_data (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    batch_id BIGINT NULL,
    source_sheet_code NVARCHAR(64) NULL,
    source_identification_code NVARCHAR(64) NOT NULL,
    company_code NVARCHAR(64) NOT NULL,
    company_name NVARCHAR(255) NULL,
    factory_name NVARCHAR(255) NULL,
    source_category_key NVARCHAR(64) NULL,
    scope_name NVARCHAR(128) NULL,
    scope_subcategory NVARCHAR(255) NULL,
    source_identification_name NVARCHAR(255) NULL,
    emission_source_name NVARCHAR(255) NULL,
    activity_unit NVARCHAR(64) NULL,
    activity_year INT NULL,
    activity_month INT NULL,
    activity_date DATE NULL,
    activity_value DECIMAL(28, 10) NULL,
    responsible_dept NVARCHAR(255) NULL,
    data_source NVARCHAR(255) NULL,
    source_remark NVARCHAR(500) NULL,
    factor_key NVARCHAR(64) NULL,
    calculated_emission DECIMAL(28, 10) NULL,
    data_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT fk_ce_activity_data_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id)
);
GO

CREATE TABLE ce_green_power_certificate (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    factory_code NVARCHAR(64) NOT NULL,
    factory_name NVARCHAR(255) NULL,
    activity_year INT NULL,
    activity_month INT NULL,
    source_category_key NVARCHAR(64) NULL,
    scope_name NVARCHAR(128) NULL,
    scope_subcategory NVARCHAR(255) NULL,
    electricity_type NVARCHAR(128) NULL,
    electricity_type_desc NVARCHAR(255) NULL,
    quantity_kwh DECIMAL(28, 10) NULL,
    certificate_code NVARCHAR(128) NULL,
    issuing_org NVARCHAR(255) NULL,
    purchase_date DATE NULL,
    expiry_date DATE NULL,
    power_grid_region NVARCHAR(255) NULL,
    offset_power_source NVARCHAR(255) NULL,
    data_source NVARCHAR(255) NULL,
    source_remark NVARCHAR(500) NULL,
    emission_source_name NVARCHAR(255) NULL,
    factor_key NVARCHAR(64) NULL,
    proof_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL
);
GO

CREATE TABLE ce_intensity_denominator_rule (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    denominator_rule_key NVARCHAR(64) NOT NULL,
    factory_type NVARCHAR(128) NOT NULL,
    denominator_type NVARCHAR(128) NOT NULL,
    denominator_metric_name NVARCHAR(255) NOT NULL,
    intensity_unit_display NVARCHAR(128) NULL,
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_intensity_denominator_rule UNIQUE (denominator_rule_key)
);
GO

CREATE TABLE ce_intensity_target (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    factory_type NVARCHAR(128) NOT NULL,
    target_year INT NOT NULL,
    target_value DECIMAL(28, 10) NOT NULL,
    unit_name NVARCHAR(128) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_intensity_target UNIQUE (factory_type, target_year)
);
GO

CREATE TABLE ce_intensity_denominator_fact (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    batch_id BIGINT NULL,
    source_sheet_code NVARCHAR(64) NULL,
    factory_code NVARCHAR(64) NOT NULL,
    factory_name NVARCHAR(255) NULL,
    factory_type NVARCHAR(128) NULL,
    fact_year INT NOT NULL,
    fact_month INT NULL,
    denominator_type NVARCHAR(128) NOT NULL,
    denominator_metric_name NVARCHAR(255) NOT NULL,
    denominator_value DECIMAL(28, 10) NOT NULL,
    unit_name NVARCHAR(128) NULL,
    data_source NVARCHAR(255) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT fk_ce_denominator_fact_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id)
);
GO

CREATE TABLE ce_intensity_tolerance (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    tolerance_key NVARCHAR(64) NOT NULL,
    industry_section NVARCHAR(255) NOT NULL,
    tolerance_rate DECIMAL(18, 10) NOT NULL,
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_intensity_tolerance UNIQUE (tolerance_key)
);
GO

CREATE TABLE ce_intensity_metric (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    metric_code NVARCHAR(64) NOT NULL,
    metric_name NVARCHAR(255) NOT NULL,
    rule_code NVARCHAR(64) NULL,
    metric_period NVARCHAR(32) NOT NULL,
    numerator_emission DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_fact_id BIGINT NULL,
    denominator_value DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_unit NVARCHAR(64) NOT NULL,
    intensity_value DECIMAL(28, 10) NULL,
    target_code NVARCHAR(64) NULL,
    metric_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_intensity_metric UNIQUE (metric_code, metric_period),
    CONSTRAINT fk_ce_intensity_metric_denominator_fact
        FOREIGN KEY (denominator_fact_id) REFERENCES ce_intensity_denominator_fact (id)
);
GO

CREATE INDEX idx_ce_intensity_metric_rule
    ON ce_intensity_metric (rule_code);
GO

CREATE TABLE ce_report_template_file (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    template_code NVARCHAR(64) NOT NULL,
    template_name NVARCHAR(255) NOT NULL,
    template_type NVARCHAR(64) NOT NULL,
    file_name NVARCHAR(255) NOT NULL,
    file_path NVARCHAR(512) NOT NULL,
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_report_template_file UNIQUE (template_code)
);
GO

CREATE TABLE ce_license_state (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    license_id NVARCHAR(128) NOT NULL,
    customer_id NVARCHAR(128) NOT NULL,
    package_id BIGINT NULL,
    package_name NVARCHAR(64) NULL,
    install_id NVARCHAR(128) NOT NULL,
    key_id NVARCHAR(64) NOT NULL,
    algorithm NVARCHAR(64) NOT NULL,
    schema_version NVARCHAR(32) NOT NULL,
    valid_from DATETIME2 NOT NULL,
    valid_to DATETIME2 NOT NULL,
    last_verified_time DATETIME2 NULL,
    max_observed_time DATETIME2 NULL,
    feature_codes NVARCHAR(MAX) NULL,
    payload_digest NVARCHAR(128) NULL,
    current_summary NVARCHAR(1024) NULL,
    license_status NVARCHAR(32) NOT NULL DEFAULT 'VALID',
    CONSTRAINT uk_ce_license_state_license UNIQUE (license_id)
);
GO

CREATE TABLE ce_factor_confirmation (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    factor_code NVARCHAR(128) NOT NULL,
    factor_name NVARCHAR(255) NOT NULL,
    factor_version_code NVARCHAR(64) NOT NULL,
    factor_unit NVARCHAR(64) NOT NULL,
    factor_value DECIMAL(28, 10) NOT NULL,
    confirmation_status NVARCHAR(32) NULL,
    confirmed_by NVARCHAR(128) NULL,
    confirmed_time DATETIME2 NULL,
    license_id NVARCHAR(128) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_factor_confirmation UNIQUE (factor_code, factor_version_code, license_id)
);
GO

CREATE INDEX idx_ce_factor_confirmation_status
    ON ce_factor_confirmation (confirmation_status);
GO

CREATE INDEX idx_ce_factor_confirmation_license
    ON ce_factor_confirmation (license_id);
GO

CREATE TABLE ce_factor_cache_version (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    vendor_version_id NVARCHAR(128) NOT NULL,
    license_id NVARCHAR(128) NOT NULL,
    version_code NVARCHAR(64) NOT NULL,
    frozen_flag BIT NOT NULL DEFAULT 0,
    synced_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_ce_factor_cache_version UNIQUE (vendor_version_id, license_id)
);
GO

CREATE TABLE ce_factor_cache_record (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cache_version_id BIGINT NOT NULL,
    factor_table_code NVARCHAR(64) NOT NULL DEFAULT N'201ef',
    factor_code NVARCHAR(128) NOT NULL,
    factor_name NVARCHAR(255) NOT NULL,
    factor_category NVARCHAR(128) NOT NULL,
    factor_value DECIMAL(28, 10) NOT NULL,
    factor_unit NVARCHAR(64) NOT NULL,
    factor_key NVARCHAR(64) NULL,
    emission_source_name NVARCHAR(255) NULL,
    emission_source_name_en NVARCHAR(255) NULL,
    fuel_material_category NVARCHAR(255) NULL,
    source_unit NVARCHAR(64) NULL,
    co2 DECIMAL(28, 10) NULL,
    ch4 DECIMAL(28, 10) NULL,
    n2o DECIMAL(28, 10) NULL,
    hfcs DECIMAL(28, 10) NULL,
    pfcs DECIMAL(28, 10) NULL,
    sf6 DECIMAL(28, 10) NULL,
    nf3 DECIMAL(28, 10) NULL,
    applicable_scope NVARCHAR(255) NULL,
    factor_source NVARCHAR(512) NULL,
    gwp_ch4 DECIMAL(28, 10) NULL,
    gwp_n2o DECIMAL(28, 10) NULL,
    gwp_hfcs DECIMAL(28, 10) NULL,
    gwp_pfcs DECIMAL(28, 10) NULL,
    gwp_sf6 DECIMAL(28, 10) NULL,
    gwp_nf3 DECIMAL(28, 10) NULL,
    factor_gwp DECIMAL(28, 10) NULL,
    version_province_code NVARCHAR(128) NULL,
    factor_version NVARCHAR(64) NULL,
    division_code NVARCHAR(64) NULL,
    division_name NVARCHAR(128) NULL,
    region_name NVARCHAR(128) NULL,
    province_factor DECIMAL(28, 10) NULL,
    region_factor DECIMAL(28, 10) NULL,
    national_factor DECIMAL(28, 10) NULL,
    non_fossil_excluded_factor DECIMAL(28, 10) NULL,
    national_fossil_power_factor DECIMAL(28, 10) NULL,
    row_no INT NULL,
    fuel_level1 NVARCHAR(255) NULL,
    fuel_level2 NVARCHAR(255) NULL,
    fuel_level3 NVARCHAR(255) NULL,
    fuel_level4 NVARCHAR(255) NULL,
    lower_heat_value DECIMAL(28, 10) NULL,
    lower_heat_value_cv DECIMAL(28, 10) NULL,
    co2_factor DECIMAL(28, 10) NULL,
    co2_factor_cv DECIMAL(28, 10) NULL,
    gwp_value DECIMAL(28, 10) NULL,
    converted_factor DECIMAL(28, 10) NULL,
    source_ref NVARCHAR(512) NULL,
    enabled_flag BIT NOT NULL DEFAULT 1,
    synced_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_ce_factor_cache_record UNIQUE (cache_version_id, factor_table_code, factor_code),
    CONSTRAINT fk_ce_factor_cache_record_version
        FOREIGN KEY (cache_version_id) REFERENCES ce_factor_cache_version (id)
);
GO

CREATE INDEX idx_ce_factor_cache_record_code
    ON ce_factor_cache_record (factor_code);
GO

CREATE INDEX idx_ce_factor_cache_record_version
    ON ce_factor_cache_record (cache_version_id);
GO

CREATE INDEX idx_ce_factor_cache_record_table
    ON ce_factor_cache_record (factor_table_code);
GO

CREATE TABLE ce_extension_field_value (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    owner_table_code NVARCHAR(128) NOT NULL,
    owner_record_id BIGINT NOT NULL,
    extension_field_id BIGINT NOT NULL,
    text_value NVARCHAR(MAX) NULL,
    decimal_value DECIMAL(28, 10) NULL,
    date_value DATETIME2 NULL,
    boolean_value BIT NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_ce_extension_field_value UNIQUE (owner_table_code, owner_record_id, extension_field_id),
    CONSTRAINT fk_ce_extension_field_value_field
        FOREIGN KEY (extension_field_id) REFERENCES ce_extension_field (id)
);
GO

CREATE INDEX idx_ce_extension_field_value_field
    ON ce_extension_field_value (extension_field_id);
GO

IF NOT EXISTS (SELECT 1 FROM ce_report_template_file WHERE template_code = N'GHG_INVENTORY_V1')
BEGIN
    INSERT INTO ce_report_template_file (
        template_code, template_name, template_type, file_name, file_path, enabled_flag, remark
    )
    VALUES (
        N'GHG_INVENTORY_V1',
        N'Greenhouse gas inventory report template',
        N'inventory',
        N'greenhouse-gas-inventory-template.xlsx',
        N'enterprise/report-templates/greenhouse-gas-inventory-template.xlsx',
        1,
        N'Enterprise-side seed template; replace file_path during deployment'
    );
END
GO

CREATE VIEW rpt.v_LicenseGate AS
SELECT
    license_id,
    customer_id,
    package_id,
    package_name,
    install_id,
    license_status,
    valid_from,
    valid_to
FROM ce_license_state
WHERE license_status = 'VALID'
  AND valid_from <= SYSUTCDATETIME()
  AND valid_to >= SYSUTCDATETIME();
GO

CREATE VIEW rpt.v_CaptureRows AS
SELECT
    b.id AS batch_id,
    b.module_code,
    b.batch_status,
    b.validation_status,
    r.id AS row_id,
    r.sheet_id,
    r.row_status
FROM ce_capture_batch b
INNER JOIN ce_capture_row r ON r.batch_id = b.id
WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate);
GO

CREATE VIEW rpt.v_ActivityDataFact AS
SELECT
    a.id AS activity_data_id,
    a.batch_id,
    a.source_sheet_code,
    a.source_identification_code,
    a.company_code,
    a.company_name,
    a.factory_name,
    a.source_category_key,
    a.scope_name,
    a.scope_subcategory,
    a.source_identification_name,
    a.emission_source_name,
    a.activity_year,
    a.activity_month,
    a.activity_date,
    a.activity_value,
    a.activity_unit,
    a.responsible_dept,
    a.data_source,
    a.factor_key,
    a.calculated_emission,
    a.data_status,
    s.responsible_dept AS configured_responsible_dept
FROM ce_activity_data a
LEFT JOIN ce_emission_source s ON s.source_identification_code = a.source_identification_code
WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate);
GO

CREATE VIEW rpt.v_GreenElectricityFact AS
SELECT
    id AS certificate_id,
    factory_code,
    factory_name,
    activity_year,
    activity_month,
    source_category_key,
    scope_name,
    scope_subcategory,
    electricity_type,
    electricity_type_desc,
    quantity_kwh,
    certificate_code,
    issuing_org,
    purchase_date,
    expiry_date,
    power_grid_region,
    offset_power_source,
    data_source,
    emission_source_name,
    factor_key,
    proof_status
FROM ce_green_power_certificate
WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate);
GO

CREATE VIEW rpt.v_IntensityMetricFact AS
SELECT
    id AS metric_id,
    metric_code,
    metric_name,
    rule_code,
    metric_period,
    numerator_emission,
    denominator_fact_id,
    denominator_value,
    denominator_unit,
    intensity_value,
    target_code,
    metric_status
FROM ce_intensity_metric
WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate);
GO
