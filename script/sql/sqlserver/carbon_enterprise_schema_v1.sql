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

CREATE TABLE ce_emission_source (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    source_code NVARCHAR(64) NOT NULL,
    source_name NVARCHAR(255) NOT NULL,
    source_category_code NVARCHAR(64) NOT NULL,
    source_category_name NVARCHAR(255) NOT NULL,
    facility_name NVARCHAR(255) NULL,
    boundary_scope NVARCHAR(64) NOT NULL DEFAULT 'enterprise_local',
    enabled_flag BIT NOT NULL DEFAULT 1,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_emission_source_code UNIQUE (source_code)
);
GO

CREATE INDEX idx_ce_emission_source_category
    ON ce_emission_source (source_category_code);
GO

CREATE TABLE ce_factor_confirmation (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    factor_code NVARCHAR(128) NOT NULL,
    factor_name NVARCHAR(255) NOT NULL,
    factor_version_code NVARCHAR(64) NOT NULL,
    factor_unit NVARCHAR(64) NOT NULL,
    factor_value DECIMAL(28, 10) NOT NULL,
    confirmation_status NVARCHAR(32) NOT NULL DEFAULT 'pending',
    confirmed_by NVARCHAR(64) NULL,
    confirmed_time DATETIME2 NULL,
    license_id NVARCHAR(128) NULL,
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_factor_confirmation UNIQUE (factor_code, factor_version_code)
);
GO

CREATE INDEX idx_ce_factor_confirmation_status
    ON ce_factor_confirmation (confirmation_status);
GO

CREATE TABLE ce_activity_data (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    batch_id BIGINT NULL,
    emission_source_id BIGINT NOT NULL,
    activity_period NVARCHAR(32) NOT NULL,
    activity_value DECIMAL(28, 10) NOT NULL,
    activity_unit NVARCHAR(64) NOT NULL,
    factor_confirmation_id BIGINT NULL,
    calculated_emission DECIMAL(28, 10) NULL,
    data_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT fk_ce_activity_data_batch
        FOREIGN KEY (batch_id) REFERENCES ce_capture_batch (id),
    CONSTRAINT fk_ce_activity_data_source
        FOREIGN KEY (emission_source_id) REFERENCES ce_emission_source (id),
    CONSTRAINT fk_ce_activity_data_factor
        FOREIGN KEY (factor_confirmation_id) REFERENCES ce_factor_confirmation (id)
);
GO

CREATE INDEX idx_ce_activity_data_period
    ON ce_activity_data (activity_period, data_status);
GO

CREATE INDEX idx_ce_activity_data_source
    ON ce_activity_data (emission_source_id);
GO

CREATE TABLE ce_green_power_certificate (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    certificate_code NVARCHAR(128) NOT NULL,
    certificate_type NVARCHAR(64) NOT NULL,
    energy_period NVARCHAR(32) NOT NULL,
    energy_amount DECIMAL(28, 10) NOT NULL,
    energy_unit NVARCHAR(64) NOT NULL DEFAULT 'MWh',
    issuing_org NVARCHAR(255) NULL,
    purchase_date DATETIME2 NULL,
    expiry_date DATETIME2 NULL,
    offset_source_code NVARCHAR(64) NULL,
    proof_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_green_power_certificate UNIQUE (certificate_code)
);
GO

CREATE INDEX idx_ce_green_power_period
    ON ce_green_power_certificate (energy_period, proof_status);
GO

CREATE TABLE ce_intensity_metric (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    metric_code NVARCHAR(64) NOT NULL,
    metric_name NVARCHAR(255) NOT NULL,
    metric_period NVARCHAR(32) NOT NULL,
    numerator_emission DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_value DECIMAL(28, 10) NOT NULL DEFAULT 0,
    denominator_unit NVARCHAR(64) NOT NULL,
    intensity_value DECIMAL(28, 10) NULL,
    metric_status NVARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    update_time DATETIME2 NULL DEFAULT SYSUTCDATETIME(),
    remark NVARCHAR(500) NULL,
    CONSTRAINT uk_ce_intensity_metric UNIQUE (metric_code, metric_period)
);
GO

CREATE INDEX idx_ce_intensity_metric_status
    ON ce_intensity_metric (metric_status);
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
    install_id NVARCHAR(128) NOT NULL,
    key_id NVARCHAR(64) NOT NULL,
    algorithm NVARCHAR(64) NOT NULL,
    schema_version NVARCHAR(32) NOT NULL,
    valid_from DATETIME2 NOT NULL,
    valid_to DATETIME2 NOT NULL,
    last_verified_time DATETIME2 NULL,
    max_observed_time DATETIME2 NULL,
    license_status NVARCHAR(32) NOT NULL DEFAULT 'active',
    CONSTRAINT uk_ce_license_state_license UNIQUE (license_id)
);
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
    install_id,
    license_status,
    valid_from,
    valid_to
FROM ce_license_state
WHERE license_status = 'active'
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
