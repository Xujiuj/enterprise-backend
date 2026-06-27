-- Enterprise carbon data platform initialization SQL.
-- Target database: local MySQL database `enterprise`.
-- Scope: enterprise-backend only. Do not run this against the vendor database.

CREATE DATABASE IF NOT EXISTS enterprise DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE enterprise;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_role_dept;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user_post;
DROP TABLE IF EXISTS sys_oss_config;
DROP TABLE IF EXISTS sys_oss;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_logininfor;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_post;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_tenant_package;
DROP TABLE IF EXISTS sys_tenant;
DROP TABLE IF EXISTS sys_client;

DROP TABLE IF EXISTS ce_extension_field_value;
DROP TABLE IF EXISTS ce_factor_cache_record;
DROP TABLE IF EXISTS ce_factor_cache_version;
DROP TABLE IF EXISTS ce_factor_confirmation;
DROP TABLE IF EXISTS ce_license_state;
DROP TABLE IF EXISTS ce_report_content;
DROP TABLE IF EXISTS ce_report_template_file;
DROP TABLE IF EXISTS ce_intensity_metric;
DROP TABLE IF EXISTS ce_intensity_denominator_fact;
DROP TABLE IF EXISTS ce_intensity_target;
DROP TABLE IF EXISTS ce_intensity_tolerance;
DROP TABLE IF EXISTS ce_intensity_denominator_rule;
DROP TABLE IF EXISTS ce_green_power_certificate;
DROP TABLE IF EXISTS ce_activity_data;
DROP TABLE IF EXISTS ce_capture_cell;
DROP TABLE IF EXISTS ce_capture_row;
DROP TABLE IF EXISTS ce_capture_batch;
DROP TABLE IF EXISTS ce_emission_source;
DROP TABLE IF EXISTS ce_emission_source_category;
DROP TABLE IF EXISTS ce_greenhouse_gas;
DROP TABLE IF EXISTS ce_electricity_factor_scope;
DROP TABLE IF EXISTS ce_electricity_factor_version_map;
DROP TABLE IF EXISTS ce_electricity_factor;
DROP TABLE IF EXISTS ce_fuel_factor_calc;
DROP TABLE IF EXISTS ce_ef_factor;
DROP TABLE IF EXISTS ce_base_year;
DROP TABLE IF EXISTS ce_company_factory;
DROP TABLE IF EXISTS ce_admin_division;
DROP TABLE IF EXISTS ce_extension_field;
DROP TABLE IF EXISTS ce_template_field;
DROP TABLE IF EXISTS ce_template_sheet;
DROP TABLE IF EXISTS ce_template_version;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_tenant (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(20) NOT NULL,
    contact_user_name VARCHAR(20) DEFAULT NULL,
    contact_phone VARCHAR(20) DEFAULT NULL,
    company_name VARCHAR(64) DEFAULT NULL,
    license_number VARCHAR(64) DEFAULT NULL,
    address VARCHAR(200) DEFAULT NULL,
    intro VARCHAR(200) DEFAULT NULL,
    domain VARCHAR(200) DEFAULT NULL,
    remark VARCHAR(200) DEFAULT NULL,
    package_id BIGINT DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL,
    account_count INT DEFAULT -1,
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant table';

CREATE TABLE sys_tenant_package (
    package_id BIGINT NOT NULL,
    package_name VARCHAR(64) DEFAULT NULL,
    menu_ids VARCHAR(5000) DEFAULT NULL,
    remark VARCHAR(200) DEFAULT NULL,
    menu_check_strictly TINYINT(1) DEFAULT 1,
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant package table';

CREATE TABLE sys_dept (
    dept_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    parent_id BIGINT DEFAULT 0,
    ancestors VARCHAR(500) DEFAULT '',
    dept_name VARCHAR(64) DEFAULT '',
    dept_category VARCHAR(100) DEFAULT NULL,
    order_num INT DEFAULT 0,
    leader BIGINT DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    email VARCHAR(64) DEFAULT NULL,
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (dept_id),
    KEY idx_sys_dept_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Department table';

CREATE TABLE sys_user (
    user_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    dept_id BIGINT DEFAULT NULL,
    user_name VARCHAR(30) NOT NULL,
    nick_name VARCHAR(30) NOT NULL,
    user_type VARCHAR(10) DEFAULT 'sys_user',
    email VARCHAR(64) DEFAULT '',
    phonenumber VARCHAR(20) DEFAULT '',
    sex CHAR(1) DEFAULT '0',
    avatar BIGINT DEFAULT NULL,
    password VARCHAR(100) DEFAULT '',
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    login_ip VARCHAR(128) DEFAULT '',
    login_date DATETIME DEFAULT NULL,
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_sys_user_name (tenant_id, user_name),
    KEY idx_sys_user_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

CREATE TABLE sys_post (
    post_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    dept_id BIGINT NOT NULL,
    post_code VARCHAR(64) NOT NULL,
    post_category VARCHAR(100) DEFAULT NULL,
    post_name VARCHAR(64) NOT NULL,
    post_sort INT NOT NULL,
    status CHAR(1) NOT NULL DEFAULT '0',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Post table';

CREATE TABLE sys_role (
    role_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    role_name VARCHAR(64) NOT NULL,
    role_key VARCHAR(100) NOT NULL,
    role_sort INT NOT NULL,
    data_scope CHAR(1) DEFAULT '1',
    menu_check_strictly TINYINT(1) DEFAULT 1,
    dept_check_strictly TINYINT(1) DEFAULT 1,
    status CHAR(1) NOT NULL DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_sys_role_key (tenant_id, role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role table';

CREATE TABLE sys_menu (
    menu_id BIGINT NOT NULL,
    menu_name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    path VARCHAR(200) DEFAULT '',
    component VARCHAR(255) DEFAULT NULL,
    query_param VARCHAR(512) DEFAULT NULL,
    is_frame INT DEFAULT 1,
    is_cache INT DEFAULT 0,
    menu_type CHAR(1) DEFAULT '',
    visible CHAR(1) DEFAULT '0',
    status CHAR(1) DEFAULT '0',
    perms VARCHAR(100) DEFAULT NULL,
    icon VARCHAR(100) DEFAULT '#',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT '',
    PRIMARY KEY (menu_id),
    KEY idx_sys_menu_parent (parent_id),
    KEY idx_sys_menu_perms (perms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Menu and permission table';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User role relation';

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role menu relation';

CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role department relation';

CREATE TABLE sys_user_post (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User post relation';

CREATE TABLE sys_dict_type (
    dict_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    dict_name VARCHAR(100) DEFAULT '',
    dict_type VARCHAR(100) DEFAULT '',
    status CHAR(1) DEFAULT '0',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (dict_id),
    UNIQUE KEY uk_sys_dict_type (tenant_id, dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dictionary type table';

CREATE TABLE sys_dict_data (
    dict_code BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    dict_sort INT DEFAULT 0,
    dict_label VARCHAR(100) DEFAULT '',
    dict_value VARCHAR(100) DEFAULT '',
    dict_type VARCHAR(100) DEFAULT '',
    css_class VARCHAR(100) DEFAULT NULL,
    list_class VARCHAR(100) DEFAULT NULL,
    is_default CHAR(1) DEFAULT 'N',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (dict_code),
    KEY idx_sys_dict_data_type (tenant_id, dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dictionary data table';

CREATE TABLE sys_config (
    config_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    config_name VARCHAR(100) DEFAULT '',
    config_key VARCHAR(100) DEFAULT '',
    config_value VARCHAR(500) DEFAULT '',
    config_type CHAR(1) DEFAULT 'N',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_sys_config_key (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Config table';

CREATE TABLE sys_logininfor (
    info_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(20) DEFAULT '000000',
    user_name VARCHAR(50) DEFAULT '',
    status CHAR(1) DEFAULT '0',
    ipaddr VARCHAR(128) DEFAULT '',
    login_location VARCHAR(255) DEFAULT '',
    browser VARCHAR(50) DEFAULT '',
    os VARCHAR(50) DEFAULT '',
    msg VARCHAR(255) DEFAULT '',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (info_id),
    KEY idx_sys_logininfor_user (tenant_id, user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Login log table';

CREATE TABLE sys_oper_log (
    oper_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(20) DEFAULT '000000',
    title VARCHAR(50) DEFAULT '',
    business_type INT DEFAULT 0,
    method VARCHAR(200) DEFAULT '',
    request_method VARCHAR(20) DEFAULT '',
    operator_type INT DEFAULT 0,
    oper_name VARCHAR(50) DEFAULT '',
    dept_name VARCHAR(50) DEFAULT '',
    oper_url VARCHAR(255) DEFAULT '',
    oper_ip VARCHAR(128) DEFAULT '',
    oper_location VARCHAR(255) DEFAULT '',
    oper_param TEXT DEFAULT NULL,
    json_result TEXT DEFAULT NULL,
    status INT DEFAULT 0,
    error_msg TEXT DEFAULT NULL,
    oper_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    cost_time BIGINT DEFAULT 0,
    PRIMARY KEY (oper_id),
    KEY idx_sys_oper_log_time (oper_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation log table';

CREATE TABLE sys_oss (
    oss_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(20) DEFAULT '000000',
    file_name VARCHAR(255) NOT NULL DEFAULT '',
    original_name VARCHAR(255) NOT NULL DEFAULT '',
    file_suffix VARCHAR(20) DEFAULT '',
    url VARCHAR(500) NOT NULL DEFAULT '',
    service VARCHAR(20) NOT NULL DEFAULT 'local',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (oss_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OSS file table';

CREATE TABLE sys_oss_config (
    oss_config_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) DEFAULT '000000',
    config_key VARCHAR(20) NOT NULL,
    access_key VARCHAR(255) DEFAULT '',
    secret_key VARCHAR(255) DEFAULT '',
    bucket_name VARCHAR(255) DEFAULT '',
    prefix VARCHAR(255) DEFAULT '',
    endpoint VARCHAR(255) DEFAULT '',
    domain VARCHAR(255) DEFAULT '',
    is_https CHAR(1) DEFAULT 'N',
    region VARCHAR(64) DEFAULT '',
    access_policy CHAR(1) DEFAULT '1',
    status CHAR(1) DEFAULT '0',
    ext1 VARCHAR(255) DEFAULT '',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (oss_config_id),
    UNIQUE KEY uk_sys_oss_config_key (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OSS config table';

CREATE TABLE sys_client (
    id BIGINT NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    client_key VARCHAR(32) NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    grant_type VARCHAR(255) NOT NULL,
    device_type VARCHAR(32) NOT NULL,
    active_timeout INT DEFAULT 1800,
    timeout INT DEFAULT 604800,
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_dept BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth client table';

CREATE TABLE ce_template_version (
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

CREATE TABLE ce_template_sheet (
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
    CONSTRAINT fk_ce_template_sheet_version FOREIGN KEY (template_version_id) REFERENCES ce_template_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise source workbook sheet inventory';

CREATE TABLE ce_template_field (
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
    CONSTRAINT fk_ce_template_field_sheet FOREIGN KEY (sheet_id) REFERENCES ce_template_sheet (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise original field preservation inventory';

CREATE TABLE ce_extension_field (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_table_code VARCHAR(128) NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(32) NOT NULL DEFAULT 'text',
    required_flag TINYINT(1) NOT NULL DEFAULT 0,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_extension_field (owner_table_code, field_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise configurable extension fields';

CREATE TABLE ce_admin_division (
    id BIGINT NOT NULL AUTO_INCREMENT,
    division_code VARCHAR(64) NOT NULL,
    division_name VARCHAR(128) NOT NULL,
    parent_code VARCHAR(64) DEFAULT NULL,
    division_level VARCHAR(32) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_admin_division_code (division_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Administrative division dimension';

CREATE TABLE ce_company_factory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_sk VARCHAR(64) DEFAULT NULL,
    company_code VARCHAR(64) NOT NULL,
    factory_code VARCHAR(64) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    factory_name VARCHAR(255) NOT NULL,
    province_code VARCHAR(64) DEFAULT NULL,
    province_name VARCHAR(128) DEFAULT NULL,
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
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_company_factory (factory_code),
    KEY idx_ce_company_factory_company (company_code),
    KEY idx_ce_company_factory_province (province_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Company and factory dimension';

CREATE TABLE ce_base_year (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factory_code VARCHAR(64) NOT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    base_year INT NOT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_base_year (factory_code, base_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Base year dimension';

CREATE TABLE ce_emission_source_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_sk VARCHAR(64) NOT NULL,
    business_key VARCHAR(64) DEFAULT NULL,
    ghg_scope VARCHAR(64) DEFAULT NULL,
    ghg_scope_category_sort INT DEFAULT NULL,
    ghg_scope_category VARCHAR(255) DEFAULT NULL,
    ghg_scope_en VARCHAR(255) DEFAULT NULL,
    ghg_scope_category_en VARCHAR(255) DEFAULT NULL,
    iso_category VARCHAR(255) DEFAULT NULL,
    iso_category_en VARCHAR(255) DEFAULT NULL,
    iso_category_description TEXT DEFAULT NULL,
    iso_category_description_en TEXT DEFAULT NULL,
    iso_custom_subcategory VARCHAR(255) DEFAULT NULL,
    gb_scope_category VARCHAR(255) DEFAULT NULL,
    gb_subcategory VARCHAR(255) DEFAULT NULL,
    effective_date DATE DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 1,
    version_no VARCHAR(64) DEFAULT NULL,
    unified_standard_category VARCHAR(255) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_emission_source_category (category_sk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Emission source category dimension';

CREATE TABLE ce_ef_factor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factor_sk VARCHAR(64) NOT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    emission_source_name_en VARCHAR(255) DEFAULT NULL,
    fuel_material_category VARCHAR(255) DEFAULT NULL,
    source_unit VARCHAR(64) DEFAULT NULL,
    co2 DECIMAL(28,10) DEFAULT NULL,
    ch4 DECIMAL(28,10) DEFAULT NULL,
    n2o DECIMAL(28,10) DEFAULT NULL,
    hfcs DECIMAL(28,10) DEFAULT NULL,
    pfcs DECIMAL(28,10) DEFAULT NULL,
    sf6 DECIMAL(28,10) DEFAULT NULL,
    nf3 DECIMAL(28,10) DEFAULT NULL,
    applicable_scope VARCHAR(255) DEFAULT NULL,
    factor_source VARCHAR(512) DEFAULT NULL,
    gwp_ch4 DECIMAL(28,10) DEFAULT NULL,
    gwp_n2o DECIMAL(28,10) DEFAULT NULL,
    gwp_hfcs DECIMAL(28,10) DEFAULT NULL,
    gwp_pfcs DECIMAL(28,10) DEFAULT NULL,
    gwp_sf6 DECIMAL(28,10) DEFAULT NULL,
    gwp_nf3 DECIMAL(28,10) DEFAULT NULL,
    factor_gwp DECIMAL(28,10) DEFAULT NULL,
    factor_unit VARCHAR(64) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_ef_factor (factor_sk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Emission factor dimension';

CREATE TABLE ce_electricity_factor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version_province_code VARCHAR(128) DEFAULT NULL,
    factor_version VARCHAR(64) DEFAULT NULL,
    division_code VARCHAR(64) DEFAULT NULL,
    division_name VARCHAR(128) DEFAULT NULL,
    region_name VARCHAR(128) DEFAULT NULL,
    province_factor DECIMAL(28,10) DEFAULT NULL,
    region_factor DECIMAL(28,10) DEFAULT NULL,
    national_factor DECIMAL(28,10) DEFAULT NULL,
    non_fossil_excluded_factor DECIMAL(28,10) DEFAULT NULL,
    national_fossil_power_factor DECIMAL(28,10) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_electricity_factor_version (factor_version),
    KEY idx_ce_electricity_factor_division (division_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Electricity emission factor dimension';

CREATE TABLE ce_electricity_factor_version_map (
    id BIGINT NOT NULL AUTO_INCREMENT,
    effective_year INT NOT NULL,
    factor_version VARCHAR(64) NOT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_electricity_factor_version_map (effective_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Electricity factor version mapping';

CREATE TABLE ce_fuel_factor_calc (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calc_key VARCHAR(128) NOT NULL,
    fuel_material_category VARCHAR(255) DEFAULT NULL,
    lower_heat_value DECIMAL(28,10) DEFAULT NULL,
    lower_heat_value_unit VARCHAR(64) DEFAULT NULL,
    carbon_content DECIMAL(28,10) DEFAULT NULL,
    carbon_content_unit VARCHAR(64) DEFAULT NULL,
    oxidation_rate DECIMAL(28,10) DEFAULT NULL,
    co2_factor DECIMAL(28,10) DEFAULT NULL,
    factor_unit VARCHAR(64) DEFAULT NULL,
    factor_source VARCHAR(512) DEFAULT NULL,
    effective_date DATE DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_fuel_factor_calc (calc_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fuel factor calculation dimension';

CREATE TABLE ce_electricity_factor_scope (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scope_key VARCHAR(64) NOT NULL,
    scope_name VARCHAR(255) NOT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_electricity_factor_scope (scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Electricity factor scope dimension';

CREATE TABLE ce_greenhouse_gas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gas_code VARCHAR(64) NOT NULL,
    gas_name VARCHAR(128) NOT NULL,
    gas_name_en VARCHAR(128) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_greenhouse_gas (gas_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Greenhouse gas dimension';

CREATE TABLE ce_emission_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_code VARCHAR(64) DEFAULT NULL,
    company_name VARCHAR(255) DEFAULT NULL,
    factory_code VARCHAR(64) DEFAULT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    source_category_key VARCHAR(64) DEFAULT NULL,
    scope_name VARCHAR(128) DEFAULT NULL,
    scope_subcategory VARCHAR(255) DEFAULT NULL,
    source_identification_code VARCHAR(64) DEFAULT NULL,
    source_identification_name VARCHAR(255) DEFAULT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    source_unit VARCHAR(64) DEFAULT NULL,
    responsible_dept VARCHAR(255) DEFAULT NULL,
    data_source VARCHAR(255) DEFAULT NULL,
    factor_key VARCHAR(64) DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_emission_source_factory_source (factory_code, source_identification_code),
    KEY idx_ce_emission_source_company (company_code),
    KEY idx_ce_emission_source_category (source_category_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Emission source identification';

CREATE TABLE ce_capture_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_version_id BIGINT DEFAULT NULL,
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
    KEY idx_ce_capture_batch_module (module_code, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local data capture batch';

CREATE TABLE ce_capture_row (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    row_no INT NOT NULL,
    row_status VARCHAR(32) DEFAULT 'draft',
    validation_message TEXT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_capture_row (batch_id, row_no),
    CONSTRAINT fk_ce_capture_row_batch FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise captured row';

CREATE TABLE ce_capture_cell (
    id BIGINT NOT NULL AUTO_INCREMENT,
    row_id BIGINT NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    field_name VARCHAR(255) DEFAULT NULL,
    cell_value TEXT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_capture_cell (row_id, field_code),
    CONSTRAINT fk_ce_capture_cell_row FOREIGN KEY (row_id) REFERENCES ce_capture_row (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise captured cell';

CREATE TABLE ce_activity_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT DEFAULT NULL,
    emission_source_id BIGINT DEFAULT NULL,
    activity_period VARCHAR(32) DEFAULT NULL,
    source_sheet_code VARCHAR(64) DEFAULT NULL,
    source_identification_code VARCHAR(64) DEFAULT NULL,
    company_code VARCHAR(64) DEFAULT NULL,
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
    activity_value DECIMAL(28,10) DEFAULT NULL,
    responsible_dept VARCHAR(255) DEFAULT NULL,
    data_source VARCHAR(255) DEFAULT NULL,
    source_remark VARCHAR(500) DEFAULT NULL,
    factor_key VARCHAR(64) DEFAULT NULL,
    data_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_activity_data_period (activity_year, activity_month, data_status),
    KEY idx_ce_activity_data_source (source_identification_code),
    KEY idx_ce_activity_data_company (company_code),
    KEY idx_ce_activity_data_source_id (emission_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Emission activity data';

CREATE TABLE ce_green_power_certificate (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factory_code VARCHAR(64) DEFAULT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    activity_year INT DEFAULT NULL,
    activity_month INT DEFAULT NULL,
    source_category_key VARCHAR(64) DEFAULT NULL,
    scope_name VARCHAR(128) DEFAULT NULL,
    scope_subcategory VARCHAR(255) DEFAULT NULL,
    electricity_type VARCHAR(128) DEFAULT NULL,
    electricity_type_desc VARCHAR(255) DEFAULT NULL,
    quantity_kwh DECIMAL(28,10) DEFAULT NULL,
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
    KEY idx_ce_green_power_period (activity_year, activity_month, proof_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Green electricity and certificate data';

CREATE TABLE ce_intensity_denominator_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    denominator_rule_key VARCHAR(128) NOT NULL,
    factory_type VARCHAR(128) DEFAULT NULL,
    denominator_type VARCHAR(128) DEFAULT NULL,
    denominator_metric_name VARCHAR(255) DEFAULT NULL,
    intensity_unit_display VARCHAR(128) DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_denominator_rule (denominator_rule_key),
    KEY idx_ce_intensity_denominator_rule_type (factory_type, denominator_type, enabled_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Intensity denominator rule';

CREATE TABLE ce_intensity_target (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factory_type VARCHAR(128) NOT NULL,
    target_year INT NOT NULL,
    target_value DECIMAL(28,10) NOT NULL,
    unit_name VARCHAR(64) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_target (factory_type, target_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Intensity target';

CREATE TABLE ce_intensity_tolerance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tolerance_key VARCHAR(128) NOT NULL,
    industry_section VARCHAR(255) DEFAULT NULL,
    tolerance_rate DECIMAL(12,6) NOT NULL DEFAULT 0,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_tolerance (tolerance_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Intensity tolerance';

CREATE TABLE ce_intensity_denominator_fact (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_sheet_code VARCHAR(64) DEFAULT NULL,
    factory_code VARCHAR(64) DEFAULT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    factory_type VARCHAR(128) DEFAULT NULL,
    fact_year INT DEFAULT NULL,
    fact_month INT DEFAULT NULL,
    denominator_type VARCHAR(128) DEFAULT NULL,
    denominator_metric_name VARCHAR(255) DEFAULT NULL,
    denominator_value DECIMAL(28,10) DEFAULT NULL,
    unit_name VARCHAR(64) DEFAULT NULL,
    data_source VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_denominator_fact_quality (factory_code, fact_year, fact_month, denominator_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Intensity denominator fact';

CREATE TABLE ce_intensity_metric (
    id BIGINT NOT NULL AUTO_INCREMENT,
    denominator_fact_id BIGINT DEFAULT NULL,
    factory_code VARCHAR(64) DEFAULT NULL,
    factory_name VARCHAR(255) DEFAULT NULL,
    metric_year INT DEFAULT NULL,
    metric_month INT DEFAULT NULL,
    numerator_value DECIMAL(28,10) DEFAULT NULL,
    denominator_value DECIMAL(28,10) DEFAULT NULL,
    intensity_value DECIMAL(28,10) DEFAULT NULL,
    unit_name VARCHAR(64) DEFAULT NULL,
    metric_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_intensity_metric_factory (factory_code, metric_year, metric_month),
    CONSTRAINT fk_ce_intensity_metric_denominator FOREIGN KEY (denominator_fact_id) REFERENCES ce_intensity_denominator_fact (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Intensity metric';

CREATE TABLE ce_report_template_file (
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

CREATE TABLE ce_report_content (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise report content catalog';

CREATE TABLE ce_license_state (
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

CREATE TABLE ce_factor_confirmation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factor_code VARCHAR(128) NOT NULL,
    factor_name VARCHAR(255) NOT NULL,
    factor_version_code VARCHAR(64) NOT NULL,
    factor_unit VARCHAR(64) NOT NULL,
    factor_value DECIMAL(28,10) NOT NULL,
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

CREATE TABLE ce_factor_cache_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vendor_version_id VARCHAR(128) NOT NULL,
    license_id VARCHAR(128) NOT NULL,
    version_code VARCHAR(64) NOT NULL,
    frozen_flag TINYINT(1) NOT NULL DEFAULT 0,
    synced_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_factor_cache_version (vendor_version_id, license_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local factor cache version';

CREATE TABLE ce_factor_cache_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cache_version_id BIGINT NOT NULL,
    factor_table_code VARCHAR(64) NOT NULL DEFAULT '201ef',
    factor_code VARCHAR(128) NOT NULL,
    factor_name VARCHAR(255) NOT NULL,
    factor_category VARCHAR(128) NOT NULL,
    factor_value DECIMAL(28,10) NOT NULL,
    factor_unit VARCHAR(64) NOT NULL,
    factor_key VARCHAR(64) DEFAULT NULL,
    emission_source_name VARCHAR(255) DEFAULT NULL,
    emission_source_name_en VARCHAR(255) DEFAULT NULL,
    fuel_material_category VARCHAR(255) DEFAULT NULL,
    source_unit VARCHAR(64) DEFAULT NULL,
    co2 DECIMAL(28,10) DEFAULT NULL,
    ch4 DECIMAL(28,10) DEFAULT NULL,
    n2o DECIMAL(28,10) DEFAULT NULL,
    hfcs DECIMAL(28,10) DEFAULT NULL,
    pfcs DECIMAL(28,10) DEFAULT NULL,
    sf6 DECIMAL(28,10) DEFAULT NULL,
    nf3 DECIMAL(28,10) DEFAULT NULL,
    applicable_scope VARCHAR(255) DEFAULT NULL,
    factor_source VARCHAR(512) DEFAULT NULL,
    gwp_ch4 DECIMAL(28,10) DEFAULT NULL,
    gwp_n2o DECIMAL(28,10) DEFAULT NULL,
    gwp_hfcs DECIMAL(28,10) DEFAULT NULL,
    gwp_pfcs DECIMAL(28,10) DEFAULT NULL,
    gwp_sf6 DECIMAL(28,10) DEFAULT NULL,
    gwp_nf3 DECIMAL(28,10) DEFAULT NULL,
    factor_gwp DECIMAL(28,10) DEFAULT NULL,
    version_province_code VARCHAR(128) DEFAULT NULL,
    factor_version VARCHAR(64) DEFAULT NULL,
    division_code VARCHAR(64) DEFAULT NULL,
    division_name VARCHAR(128) DEFAULT NULL,
    region_name VARCHAR(128) DEFAULT NULL,
    province_factor DECIMAL(28,10) DEFAULT NULL,
    region_factor DECIMAL(28,10) DEFAULT NULL,
    national_factor DECIMAL(28,10) DEFAULT NULL,
    non_fossil_excluded_factor DECIMAL(28,10) DEFAULT NULL,
    national_fossil_power_factor DECIMAL(28,10) DEFAULT NULL,
    row_no INT DEFAULT NULL,
    fuel_level1 VARCHAR(255) DEFAULT NULL,
    fuel_level2 VARCHAR(255) DEFAULT NULL,
    fuel_level3 VARCHAR(255) DEFAULT NULL,
    fuel_level4 VARCHAR(255) DEFAULT NULL,
    lower_heat_value DECIMAL(28,10) DEFAULT NULL,
    lower_heat_value_cv DECIMAL(28,10) DEFAULT NULL,
    co2_factor DECIMAL(28,10) DEFAULT NULL,
    co2_factor_cv DECIMAL(28,10) DEFAULT NULL,
    gwp_value DECIMAL(28,10) DEFAULT NULL,
    converted_factor DECIMAL(28,10) DEFAULT NULL,
    source_ref VARCHAR(512) DEFAULT NULL,
    custom_fields TEXT DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    synced_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_factor_cache_record (cache_version_id, factor_table_code, factor_code),
    KEY idx_ce_factor_cache_record_table (factor_table_code),
    CONSTRAINT fk_ce_factor_cache_record_version FOREIGN KEY (cache_version_id) REFERENCES ce_factor_cache_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local factor cache record';

CREATE TABLE ce_extension_field_value (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_table_code VARCHAR(128) NOT NULL,
    owner_record_id BIGINT NOT NULL,
    extension_field_id BIGINT NOT NULL,
    text_value TEXT DEFAULT NULL,
    decimal_value DECIMAL(28,10) DEFAULT NULL,
    date_value DATETIME DEFAULT NULL,
    boolean_value TINYINT(1) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_extension_field_value (owner_table_code, owner_record_id, extension_field_id),
    KEY idx_ce_extension_field_value_field (extension_field_id),
    CONSTRAINT fk_ce_extension_field_value_field FOREIGN KEY (extension_field_id) REFERENCES ce_extension_field (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise extension field value';

INSERT INTO sys_tenant VALUES
(1, '000000', '企业管理员', '15888888888', '企业碳数据管理平台', NULL, NULL, '企业端私有化部署租户', NULL, NULL, NULL, NULL, -1, '0', '0', 103, 1, NOW(), NULL, NULL);

INSERT INTO sys_dept VALUES
(100, '000000', 0, '0', '企业总部', NULL, 0, 1, '15888888888', 'admin@example.com', '0', '0', 100, 1, NOW(), NULL, NULL),
(103, '000000', 100, '0,100', '碳管理部', NULL, 1, 1, '15888888888', 'carbon@example.com', '0', '0', 100, 1, NOW(), NULL, NULL);

INSERT INTO sys_post VALUES
(1, '000000', 103, 'carbon_admin', NULL, '碳管理负责人', 1, '0', 103, 1, NOW(), NULL, NULL, ''),
(2, '000000', 103, 'carbon_user', NULL, '碳数据专员', 2, '0', 103, 1, NOW(), NULL, NULL, '');

INSERT INTO sys_role VALUES
(1, '000000', '超级管理员', 'superadmin', 1, '1', 1, 1, '0', '0', 103, 1, NOW(), NULL, NULL, '系统超级管理员'),
(900001, '000000', '企业管理员', 'enterprise_admin', 10, '1', 1, 1, '0', '0', 103, 1, NOW(), NULL, NULL, '企业端预置管理员角色'),
(900002, '000000', '数据填报员', 'enterprise_data_entry', 11, '6', 1, 1, '0', '0', 103, 1, NOW(), NULL, NULL, '企业端预置数据填报角色'),
(900003, '000000', '数据审核员', 'enterprise_auditor', 12, '3', 1, 1, '0', '0', 103, 1, NOW(), NULL, NULL, '企业端预置审核与验证角色'),
(900004, '000000', '报表查看员', 'enterprise_report_viewer', 13, '1', 1, 1, '0', '0', 103, 1, NOW(), NULL, NULL, '企业端预置报表只读角色');

INSERT INTO sys_user VALUES
(1, '000000', 103, 'admin', '企业管理员', 'sys_user', 'admin@example.com', '15888888888', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), NULL, NULL, '初始管理员，密码沿用RuoYi默认样例');

INSERT INTO sys_user_role VALUES (1, 1), (1, 900001);
INSERT INTO sys_user_post VALUES (1, 1);

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
(1, '系统管理', 0, 8, 'system', 'Layout', '', 1, 0, 'M', '0', '0', '', 'system', 103, 1, NOW(), '系统管理目录'),
(100, '用户管理', 1, 1, 'user', 'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 103, 1, NOW(), '用户管理'),
(101, '角色管理', 1, 2, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 103, 1, NOW(), '角色管理'),
(102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 103, 1, NOW(), '菜单管理'),
(108, '日志管理', 1, 4, 'log', '', '', 1, 0, 'M', '0', '0', '', 'log', 103, 1, NOW(), '日志管理'),
(500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 103, 1, NOW(), '操作日志'),
(501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 103, 1, NOW(), '登录日志'),
(900100, '系统授权', 0, 1, 'system-auth', 'Layout', '', 1, 0, 'M', '0', '0', '', 'lock', 103, 1, NOW(), '系统授权目录'),
(900102, '授权管理', 900100, 1, 'license-import', 'enterprise/licenseImport/index', '', 1, 0, 'C', '0', '0', 'enterprise:licenseImport:import', 'lock', 103, 1, NOW(), '授权管理'),
(900110, '1 配置排放源', 0, 2, 'emission-source-config', 'Layout', '', 1, 0, 'M', '0', '0', '', 'tree', 103, 1, NOW(), '配置排放源'),
(900111, '101 行政区划', 900110, 1, 'admin-division', 'enterprise/dimension/index', '{\"code\":\"admin-division\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'tree', 103, 1, NOW(), '行政区划'),
(900112, '102 公司表', 900110, 2, 'company', 'enterprise/dimension/index', '{\"code\":\"company\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'company', 103, 1, NOW(), '公司表'),
(900113, '103 排放源分类', 900110, 3, 'emission-source-category', 'enterprise/dimension/index', '{\"code\":\"emission-source-category\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'tree', 103, 1, NOW(), '排放源分类'),
(900114, '104 排放源识别', 900110, 4, 'emission-source', 'enterprise/emissionSource/index', '', 1, 0, 'C', '0', '0', 'enterprise:emissionSource:list', 'form', 103, 1, NOW(), '排放源识别'),
(900115, '106 基准年维度表', 900110, 5, 'base-year', 'enterprise/dimension/index', '{\"code\":\"base-year\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'date-range', 103, 1, NOW(), '基准年维度表'),
(900120, '2 确认排放因子', 0, 3, 'factor-confirm', 'Layout', '', 1, 0, 'M', '0', '0', '', 'validCode', 103, 1, NOW(), '确认排放因子'),
(900121, '201 EF排放因子维度表', 900120, 1, 'ef-factor', 'enterprise/dimension/index', '{\"code\":\"ef-factor\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'search', 103, 1, NOW(), 'EF排放因子维度表'),
(900122, '202 EF电力因子维度表', 900120, 2, 'ef-electricity-factor', 'enterprise/dimension/index', '{\"code\":\"ef-electricity-factor\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'search', 103, 1, NOW(), 'EF电力因子维度表'),
(900123, '203 EF电力因子版本对应', 900120, 3, 'ef-electricity-version', 'enterprise/dimension/index', '{\"code\":\"ef-electricity-version\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'validCode', 103, 1, NOW(), 'EF电力因子版本对应'),
(900124, '205 EF电力因子口径维度', 900120, 4, 'ef-electricity-scope', 'enterprise/dimension/index', '{\"code\":\"ef-electricity-scope\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'validCode', 103, 1, NOW(), 'EF电力因子口径维度'),
(900125, '206 温室气体维度', 900120, 5, 'greenhouse-gas', 'enterprise/dimension/index', '{\"code\":\"greenhouse-gas\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'search', 103, 1, NOW(), '温室气体维度'),
(900130, '3 活动数据', 0, 4, 'activity-data', 'Layout', '', 1, 0, 'M', '0', '0', '', 'form', 103, 1, NOW(), '活动数据'),
(900131, '排放活动数据', 900130, 1, 'emission-activity-data', 'enterprise/activityData/index', '', 1, 0, 'C', '0', '0', 'enterprise:activityData:list', 'form', 103, 1, NOW(), '排放活动数据'),
(900140, '4 绿电绿证', 0, 5, 'green-electricity', 'Layout', '', 1, 0, 'M', '0', '0', '', 'international', 103, 1, NOW(), '绿电绿证'),
(900141, '401 绿电绿证数据', 900140, 1, 'green-electricity-data', 'enterprise/greenPowerCertificate/index', '', 1, 0, 'C', '0', '0', 'enterprise:greenPowerCertificate:list', 'international', 103, 1, NOW(), '绿电绿证数据'),
(900150, '5 强度管理', 0, 6, 'intensity', 'Layout', '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, NOW(), '强度管理'),
(900151, '501 碳排放强度分母维度表', 900150, 1, 'intensity-denominator', 'enterprise/dimension/index', '{\"code\":\"intensity-denominator\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'form', 103, 1, NOW(), '碳排放强度分母维度表'),
(900152, '502 强度目标表', 900150, 2, 'intensity-target', 'enterprise/intensityMetric/index', '', 1, 0, 'C', '0', '0', 'enterprise:intensityMetric:list', 'chart', 103, 1, NOW(), '强度目标表'),
(900153, '503 分母事实表', 900150, 3, 'denominator-fact', 'enterprise/dimension/index', '{\"code\":\"denominator-fact\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'form', 103, 1, NOW(), '分母事实表'),
(900154, '504 强度容忍率参数表', 900150, 4, 'intensity-tolerance', 'enterprise/dimension/index', '{\"code\":\"intensity-tolerance\"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'chart', 103, 1, NOW(), '强度容忍率参数表'),
(900160, '报表管理', 0, 7, 'report-management', 'Layout', '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, NOW(), '报表管理'),
(900161, 'Content', 900160, 1, 'content', 'enterprise/reports/index', '', 1, 0, 'C', '0', '0', 'enterprise:reports:view', 'chart', 103, 1, NOW(), 'Content'),
(900162, '数据验证', 900160, 2, 'data-validation', 'enterprise/dataValidation/index', '', 1, 0, 'C', '0', '0', 'enterprise:dataValidation:view', 'validCode', 103, 1, NOW(), '数据验证'),
(900163, '报表模板下载', 900160, 3, 'report-template-download', 'enterprise/reportTemplateFile/index', '', 1, 0, 'C', '0', '0', 'enterprise:reportTemplateFile:list', 'download', 103, 1, NOW(), '报表模板下载');

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
(1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 103, 1, NOW(), ''),
(1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 103, 1, NOW(), ''),
(1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 103, 1, NOW(), ''),
(1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 103, 1, NOW(), ''),
(1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 103, 1, NOW(), ''),
(1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 103, 1, NOW(), ''),
(1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 103, 1, NOW(), ''),
(1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 103, 1, NOW(), ''),
(1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 103, 1, NOW(), ''),
(1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 103, 1, NOW(), ''),
(1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 103, 1, NOW(), ''),
(1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 103, 1, NOW(), ''),
(1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 103, 1, NOW(), ''),
(1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 103, 1, NOW(), ''),
(1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 103, 1, NOW(), ''),
(900103, '授权导入接口', 900100, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:licenseImport:import', '#', 103, 1, NOW(), ''),
(900104, '授权状态查询', 900100, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:licenseState:query', '#', 103, 1, NOW(), ''),
(900105, '工作台总览', 900100, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:workbench:overview', '#', 103, 1, NOW(), ''),
(900170, '维度列表查询', 900110, 1, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:list', '#', 103, 1, NOW(), ''),
(900171, '维度详情查询', 900110, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:query', '#', 103, 1, NOW(), ''),
(900172, '维度新增', 900110, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:add', '#', 103, 1, NOW(), ''),
(900173, '维度修改', 900110, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:edit', '#', 103, 1, NOW(), ''),
(900174, '维度删除', 900110, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:remove', '#', 103, 1, NOW(), ''),
(900180, '排放源列表查询', 900130, 1, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:list', '#', 103, 1, NOW(), ''),
(900181, '排放源详情查询', 900130, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:query', '#', 103, 1, NOW(), ''),
(900182, '排放源新增', 900130, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:add', '#', 103, 1, NOW(), ''),
(900183, '排放源修改', 900130, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:edit', '#', 103, 1, NOW(), ''),
(900184, '排放源删除', 900130, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:remove', '#', 103, 1, NOW(), ''),
(900185, '活动数据列表查询', 900130, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityData:list', '#', 103, 1, NOW(), ''),
(900186, '活动数据详情查询', 900130, 7, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityData:query', '#', 103, 1, NOW(), ''),
(900187, '活动数据校验', 900130, 8, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityImportValidation:validate', '#', 103, 1, NOW(), ''),
(900188, '活动数据保存', 900130, 9, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activity:save', '#', 103, 1, NOW(), ''),
(900189, '活动数据导入', 900130, 10, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityImport:import', '#', 103, 1, NOW(), ''),
(900190, '因子确认列表查询', 900120, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:list', '#', 103, 1, NOW(), ''),
(900191, '因子确认详情查询', 900120, 7, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:query', '#', 103, 1, NOW(), ''),
(900192, '因子确认新增', 900120, 8, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:add', '#', 103, 1, NOW(), ''),
(900193, '因子确认修改', 900120, 9, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:edit', '#', 103, 1, NOW(), ''),
(900194, '因子确认删除', 900120, 10, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:remove', '#', 103, 1, NOW(), ''),
(900195, '绿电绿证列表查询', 900140, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:list', '#', 103, 1, NOW(), ''),
(900196, '绿电绿证详情查询', 900140, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:query', '#', 103, 1, NOW(), ''),
(900197, '绿电绿证新增', 900140, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:add', '#', 103, 1, NOW(), ''),
(900198, '绿电绿证修改', 900140, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:edit', '#', 103, 1, NOW(), ''),
(900199, '绿电绿证删除', 900140, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:remove', '#', 103, 1, NOW(), ''),
(900200, '强度指标列表查询', 900150, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:list', '#', 103, 1, NOW(), ''),
(900201, '强度指标详情查询', 900150, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:query', '#', 103, 1, NOW(), ''),
(900202, '强度指标新增', 900150, 7, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:add', '#', 103, 1, NOW(), ''),
(900203, '强度指标修改', 900150, 8, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:edit', '#', 103, 1, NOW(), ''),
(900204, '强度指标删除', 900150, 9, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:remove', '#', 103, 1, NOW(), ''),
(900205, '报表模板列表查询', 900160, 10, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:list', '#', 103, 1, NOW(), ''),
(900206, '报表模板详情查询', 900160, 11, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:query', '#', 103, 1, NOW(), ''),
(900210, '报表模板下载', 900160, 15, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:download', '#', 103, 1, NOW(), ''),
(900211, '厂商因子同步', 900120, 16, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorSync:run', '#', 103, 1, NOW(), ''),
(900212, '厂商模板同步', 900160, 17, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateSync:run', '#', 103, 1, NOW(), ''),
(900213, '因子缓存列表查询', 900120, 18, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorCacheRecord:list', '#', 103, 1, NOW(), ''),
(900214, '因子缓存详情查询', 900120, 19, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorCacheRecord:query', '#', 103, 1, NOW(), ''),
(900215, '扩展字段元数据查询', 900130, 11, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionField:list', '#', 103, 1, NOW(), ''),
(900216, '扩展字段值列表查询', 900130, 12, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionFieldValue:list', '#', 103, 1, NOW(), ''),
(900217, '扩展字段值新增', 900130, 13, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionFieldValue:add', '#', 103, 1, NOW(), ''),
(900218, '扩展字段值修改', 900130, 14, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionFieldValue:edit', '#', 103, 1, NOW(), ''),
(900219, 'Source(A)数据导入', 900130, 15, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:sourceA:import', '#', 103, 1, NOW(), ''),
(900220, 'Source(A)数据校验', 900130, 16, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:sourceA:validate', '#', 103, 1, NOW(), ''),
(900221, '活动数据修改', 900130, 17, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityDataRaw:edit', '#', 103, 1, NOW(), ''),
(900222, '活动数据删除', 900130, 18, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityDataRaw:remove', '#', 103, 1, NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 900001, menu_id FROM sys_menu
WHERE menu_id IN (1,100,101,102,108,500,501,1001,1002,1003,1004,1007,1008,1009,1010,1011,1013,1014,1015,1016,1040,1043,
900100,900102,900103,900104,900105,900110,900111,900112,900113,900114,900115,900120,900121,900122,900123,900124,900125,
900130,900131,900140,900141,900150,900151,900152,900153,900154,900160,900161,900162,900163,900170,900171,900172,900173,900174,
900180,900181,900182,900183,900184,900185,900186,900187,900188,900189,900190,900191,900192,900193,900194,900195,900196,900197,900198,900199,
900200,900201,900202,900203,900204,900205,900206,900210,900211,900212,900213,900214,900215,900216,900217,900218,900219,900220,900221,900222);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 900002, menu_id FROM sys_menu
WHERE menu_id IN (900105,900110,900111,900112,900113,900114,900115,900120,900121,900122,900123,900124,900125,
900130,900131,900140,900141,900150,900151,900152,900153,900154,900160,900162,900170,900171,900180,900181,900185,900186,
900187,900188,900189,900190,900191,900195,900196,900197,900198,900200,900201,900202,900203,900213,900214,900215,900216,900217,900218,900219,900220,900221);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 900003, menu_id FROM sys_menu
WHERE menu_id IN (900105,900110,900111,900112,900113,900114,900115,900120,900121,900122,900123,900124,900125,
900130,900131,900140,900141,900150,900151,900152,900153,900154,900160,900161,900162,900163,900170,900171,900180,900181,
900185,900186,900187,900190,900191,900195,900196,900200,900201,900205,900206,900210,900213,900214,900215,900216,900220);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 900004, menu_id FROM sys_menu
WHERE menu_id IN (900105,900160,900161,900162,900163,900205,900206,900210);

INSERT INTO sys_config VALUES
(1, '000000', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-green', 'Y', 103, 1, NOW(), NULL, NULL, '企业端默认绿色主题'),
(2, '000000', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 103, 1, NOW(), NULL, NULL, '初始化密码'),
(3, '000000', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 103, 1, NOW(), NULL, NULL, '侧边栏主题'),
(5, '000000', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 103, 1, NOW(), NULL, NULL, '企业端关闭自助注册');

INSERT INTO sys_client VALUES
(1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', 1800, 604800, '0', '0', 103, 1, NOW(), 1, NOW());

INSERT INTO sys_oss_config VALUES
(1, '000000', 'local', '', '', 'enterprise', '', 'local', '', 'N', '', '1', '0', '', 103, 1, NOW(), 1, NOW(), 'Local placeholder config');

INSERT INTO ce_report_template_file (template_code, template_name, template_type, file_name, file_path, enabled_flag, remark) VALUES
('GHG_INVENTORY_V1', '温室气体清单报表模板', 'inventory', 'greenhouse-gas-inventory-template.xlsx', 'enterprise/report-templates/greenhouse-gas-inventory-template.xlsx', 1, '初始化模板');

INSERT INTO ce_report_content (directory_no, directory_name, subdirectory_no, subdirectory_name, chart_names, display_order, remark) VALUES
(1, '数据质量', 1, '填报完成度', '提交状态矩阵,缺失项统计', 10, '初始化报表目录'),
(2, '排放核算', 1, '活动数据概览', '活动数据趋势,排放源分布', 20, '初始化报表目录');
