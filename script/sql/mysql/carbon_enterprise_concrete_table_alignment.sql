-- Enterprise concrete business table alignment.
-- Database boundary: enterprise only.
-- This script is idempotent for MySQL 8.x and upgrades old generated tables
-- to the current customer-sample concrete table shape.

DELIMITER //

DROP PROCEDURE IF EXISTS ce_add_column_if_missing//
CREATE PROCEDURE ce_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT(
            'ALTER TABLE ',
            p_table_name,
            ' ADD COLUMN ',
            p_column_name,
            ' ',
            p_column_definition
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_create_index_if_missing//
CREATE PROCEDURE ce_create_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql = CONCAT('CREATE INDEX ', p_index_name, ' ON ', p_table_name, ' ', p_index_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL ce_add_column_if_missing('ce_emission_source', 'company_code', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'company_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'factory_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'source_category_key', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'scope_name', 'VARCHAR(128) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'scope_subcategory', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'source_identification_code', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'source_identification_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'emission_source_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'responsible_dept', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'data_source', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_emission_source', 'factor_key', 'VARCHAR(64) DEFAULT NULL');
CALL ce_create_index_if_missing('ce_emission_source', 'idx_ce_emission_source_company', '(company_code)');
CALL ce_create_index_if_missing('ce_emission_source', 'idx_ce_emission_source_category', '(source_category_key)');

CALL ce_add_column_if_missing('ce_activity_data', 'source_sheet_code', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'source_identification_code', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'company_code', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'company_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'factory_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'source_category_key', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'scope_name', 'VARCHAR(128) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'scope_subcategory', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'source_identification_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'emission_source_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'activity_year', 'INT DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'activity_month', 'INT DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'activity_date', 'DATE DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'responsible_dept', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'data_source', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'source_remark', 'VARCHAR(500) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_activity_data', 'factor_key', 'VARCHAR(64) DEFAULT NULL');
CALL ce_create_index_if_missing('ce_activity_data', 'idx_ce_activity_data_period', '(activity_year, activity_month, data_status)');
CALL ce_create_index_if_missing('ce_activity_data', 'idx_ce_activity_data_source', '(source_identification_code)');
CALL ce_create_index_if_missing('ce_activity_data', 'idx_ce_activity_data_company', '(company_code)');

CALL ce_add_column_if_missing('ce_green_power_certificate', 'factory_code', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'factory_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'activity_year', 'INT DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'activity_month', 'INT DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'source_category_key', 'VARCHAR(64) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'scope_name', 'VARCHAR(128) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'scope_subcategory', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'electricity_type', 'VARCHAR(128) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'electricity_type_desc', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'quantity_kwh', 'DECIMAL(28, 10) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'power_grid_region', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'offset_power_source', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'data_source', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'source_remark', 'VARCHAR(500) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'emission_source_name', 'VARCHAR(255) DEFAULT NULL');
CALL ce_add_column_if_missing('ce_green_power_certificate', 'factor_key', 'VARCHAR(64) DEFAULT NULL');
CALL ce_create_index_if_missing('ce_green_power_certificate', 'idx_ce_green_power_factory', '(factory_code)');
CALL ce_create_index_if_missing('ce_green_power_certificate', 'idx_ce_green_power_period', '(activity_year, activity_month, proof_status)');

DROP PROCEDURE IF EXISTS ce_add_column_if_missing;
DROP PROCEDURE IF EXISTS ce_create_index_if_missing;
