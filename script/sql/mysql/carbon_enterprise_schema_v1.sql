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

CREATE TABLE IF NOT EXISTS ce_dimension_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dimension_code VARCHAR(64) NOT NULL,
    record_code VARCHAR(128) NOT NULL,
    record_name VARCHAR(255) NOT NULL,
    parent_code VARCHAR(128) DEFAULT NULL,
    field01 VARCHAR(255) DEFAULT NULL,
    field02 VARCHAR(255) DEFAULT NULL,
    field03 VARCHAR(255) DEFAULT NULL,
    field04 VARCHAR(255) DEFAULT NULL,
    field05 VARCHAR(255) DEFAULT NULL,
    field06 VARCHAR(255) DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status CHAR(1) NOT NULL DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_dimension_record (dimension_code, record_code),
    KEY idx_ce_dimension_record_name (dimension_code, record_name),
    KEY idx_ce_dimension_record_status (dimension_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise configurable dimension record';

CREATE TABLE IF NOT EXISTS ce_emission_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_code VARCHAR(64) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    source_category_code VARCHAR(64) NOT NULL,
    source_category_name VARCHAR(255) NOT NULL,
    facility_name VARCHAR(255) DEFAULT NULL,
    boundary_scope VARCHAR(64) NOT NULL DEFAULT 'enterprise_local',
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_emission_source_code (source_code),
    KEY idx_ce_emission_source_category (source_category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local emission source configuration';

CREATE TABLE IF NOT EXISTS ce_factor_confirmation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    factor_code VARCHAR(128) NOT NULL,
    factor_name VARCHAR(255) NOT NULL,
    factor_version_code VARCHAR(64) NOT NULL,
    factor_unit VARCHAR(64) NOT NULL,
    factor_value DECIMAL(28, 10) NOT NULL,
    confirmation_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    confirmed_by VARCHAR(64) DEFAULT NULL,
    confirmed_time DATETIME DEFAULT NULL,
    license_id VARCHAR(128) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_factor_confirmation (factor_code, factor_version_code),
    KEY idx_ce_factor_confirmation_status (confirmation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local emission factor confirmation';

CREATE TABLE IF NOT EXISTS ce_activity_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT DEFAULT NULL,
    emission_source_id BIGINT NOT NULL,
    activity_period VARCHAR(32) NOT NULL,
    activity_value DECIMAL(28, 10) NOT NULL,
    activity_unit VARCHAR(64) NOT NULL,
    factor_confirmation_id BIGINT DEFAULT NULL,
    calculated_emission DECIMAL(28, 10) DEFAULT NULL,
    data_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_activity_data_period (activity_period, data_status),
    KEY idx_ce_activity_data_source (emission_source_id),
    CONSTRAINT fk_ce_activity_data_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id),
    CONSTRAINT fk_ce_activity_data_source
        FOREIGN KEY (emission_source_id) REFERENCES ce_emission_source (id),
    CONSTRAINT fk_ce_activity_data_factor
        FOREIGN KEY (factor_confirmation_id) REFERENCES ce_factor_confirmation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local activity data';

CREATE TABLE IF NOT EXISTS ce_green_power_certificate (
    id BIGINT NOT NULL AUTO_INCREMENT,
    certificate_code VARCHAR(128) NOT NULL,
    certificate_type VARCHAR(64) NOT NULL,
    energy_period VARCHAR(32) NOT NULL,
    energy_amount DECIMAL(28, 10) NOT NULL,
    energy_unit VARCHAR(64) NOT NULL DEFAULT 'MWh',
    issuing_org VARCHAR(255) DEFAULT NULL,
    purchase_date DATETIME DEFAULT NULL,
    expiry_date DATETIME DEFAULT NULL,
    offset_source_code VARCHAR(64) DEFAULT NULL,
    proof_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_green_power_certificate (certificate_code),
    KEY idx_ce_green_power_period (energy_period, proof_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local green electricity and certificate proof';

CREATE TABLE IF NOT EXISTS ce_intensity_metric (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metric_code VARCHAR(64) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_period VARCHAR(32) NOT NULL,
    numerator_emission DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_value DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_unit VARCHAR(64) NOT NULL,
    intensity_value DECIMAL(28, 10) DEFAULT NULL,
    metric_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_intensity_metric (metric_code, metric_period),
    KEY idx_ce_intensity_metric_status (metric_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local carbon intensity metric';

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

INSERT INTO ce_dimension_record (
    dimension_code, record_code, record_name, parent_code,
    field01, field02, field03, field04, field05, field06,
    sort_order, status, remark
)
SELECT * FROM (
    SELECT 'company' AS dimension_code, 'ENT-001' AS record_code, '宁波低碳示范企业' AS record_name, NULL AS parent_code, '法人主体' AS field01, '330200' AS field02, '91330200MA000001X1' AS field03, NULL AS field04, NULL AS field05, NULL AS field06, 1 AS sort_order, '0' AS status, '企业主体示例' AS remark UNION ALL
    SELECT 'emission-source', 'ES-001', '总部办公楼外购电', NULL, 'ENT-001', 'SCOPE2-PURCHASED-ELEC', 'kWh', 'EF-ELEC-ZJ-2025', NULL, NULL, 1, '0', '排放源示例' UNION ALL
    SELECT 'ef-factor', 'EF-DIESEL-2025', '柴油燃烧排放因子', NULL, 'tCO2e/t', '生态环境部指南', 'ES-柴油', '2025', NULL, NULL, 1, '0', '排放因子示例' UNION ALL
    SELECT 'emission-activity-data', 'AD-2026-001', '2026年1月总部外购电', NULL, 'ES-001', '2026-01', '120000', 'kWh', '行政部', NULL, 1, '0', '活动数据示例' UNION ALL
    SELECT 'green-electricity-data', 'GP-2026-001', '2026年1月绿证抵扣', NULL, '绿证', '5000', 'MWh', '2026-01-15', '2027-01-14', NULL, 1, '0', '绿电绿证示例' UNION ALL
    SELECT 'intensity-denominator', 'DEN-REVENUE', '营业收入', NULL, '营收', '万元', 'ENT-001', NULL, NULL, NULL, 1, '0', '强度分母示例' UNION ALL
    SELECT 'intensity-target', 'TARGET-2026-REV', '2026营收强度目标', NULL, '2026', '单位营收排放强度', '0.85', 'BASE-2025', NULL, NULL, 1, '0', '强度目标示例' UNION ALL
    SELECT 'denominator-fact', 'FACT-2026-01-REV', '2026年1月营业收入', NULL, '2026-01', 'DEN-REVENUE', '8600', '财务部', NULL, NULL, 1, '0', '分母事实示例' UNION ALL
    SELECT 'intensity-tolerance', 'TOL-REV-2026', '营收强度波动容忍率', NULL, '单位营收排放强度', '10', '2026-01', '2026-12', NULL, NULL, 1, '0', '容忍率示例' UNION ALL
    SELECT 'data-validation', 'RULE-AD-NOT-NULL', '活动数据必填校验', NULL, '生产部', '月度', '强错误', 'activity_value != null', NULL, NULL, 1, '0', '验证规则示例'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM ce_dimension_record existing
    WHERE existing.dimension_code = seed.dimension_code
      AND existing.record_code = seed.record_code
);

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
    factor_code VARCHAR(128) NOT NULL,
    factor_name VARCHAR(255) NOT NULL,
    factor_category VARCHAR(128) NOT NULL,
    factor_value DECIMAL(28, 10) NOT NULL,
    factor_unit VARCHAR(64) NOT NULL,
    source_ref VARCHAR(512) DEFAULT NULL,
    enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
    synced_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_factor_cache_record (cache_version_id, factor_code),
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
