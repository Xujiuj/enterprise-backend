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

CREATE TABLE IF NOT EXISTS ce_license_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    license_id VARCHAR(128) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    install_id VARCHAR(128) NOT NULL,
    key_id VARCHAR(64) NOT NULL,
    algorithm VARCHAR(64) NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    last_verified_time DATETIME DEFAULT NULL,
    max_observed_time DATETIME DEFAULT NULL,
    license_status VARCHAR(32) NOT NULL DEFAULT 'active',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ce_license_state_license (license_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise local license runtime state';

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
