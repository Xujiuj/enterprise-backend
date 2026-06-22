-- Enterprise factor cache sample field alignment migration.
-- Database boundary: enterprise only.
-- This script is intentionally idempotent for local re-validation.

DELIMITER //

DROP PROCEDURE IF EXISTS ce_add_factor_cache_column//
CREATE PROCEDURE ce_add_factor_cache_column(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT,
    IN p_after_column VARCHAR(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ce_factor_cache_record'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT(
            'ALTER TABLE ce_factor_cache_record ADD COLUMN ',
            p_column_name,
            ' ',
            p_column_definition,
            ' AFTER ',
            p_after_column
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_drop_index_if_exists//
CREATE PROCEDURE ce_drop_index_if_exists(IN p_index_name VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ce_factor_cache_record'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ce_factor_cache_record DROP INDEX ', p_index_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_create_index_if_missing//
CREATE PROCEDURE ce_create_index_if_missing(
    IN p_index_name VARCHAR(64),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ce_factor_cache_record'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql = CONCAT('CREATE INDEX ', p_index_name, ' ON ce_factor_cache_record ', p_index_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL ce_add_factor_cache_column('factor_table_code', 'VARCHAR(64) NOT NULL DEFAULT ''201ef''', 'cache_version_id');
CALL ce_add_factor_cache_column('factor_key', 'VARCHAR(64) DEFAULT NULL', 'factor_unit');
CALL ce_add_factor_cache_column('emission_source_name', 'VARCHAR(255) DEFAULT NULL', 'factor_key');
CALL ce_add_factor_cache_column('emission_source_name_en', 'VARCHAR(255) DEFAULT NULL', 'emission_source_name');
CALL ce_add_factor_cache_column('fuel_material_category', 'VARCHAR(255) DEFAULT NULL', 'emission_source_name_en');
CALL ce_add_factor_cache_column('source_unit', 'VARCHAR(64) DEFAULT NULL', 'fuel_material_category');
CALL ce_add_factor_cache_column('co2', 'DECIMAL(28, 10) DEFAULT NULL', 'source_unit');
CALL ce_add_factor_cache_column('ch4', 'DECIMAL(28, 10) DEFAULT NULL', 'co2');
CALL ce_add_factor_cache_column('n2o', 'DECIMAL(28, 10) DEFAULT NULL', 'ch4');
CALL ce_add_factor_cache_column('hfcs', 'DECIMAL(28, 10) DEFAULT NULL', 'n2o');
CALL ce_add_factor_cache_column('pfcs', 'DECIMAL(28, 10) DEFAULT NULL', 'hfcs');
CALL ce_add_factor_cache_column('sf6', 'DECIMAL(28, 10) DEFAULT NULL', 'pfcs');
CALL ce_add_factor_cache_column('nf3', 'DECIMAL(28, 10) DEFAULT NULL', 'sf6');
CALL ce_add_factor_cache_column('applicable_scope', 'VARCHAR(255) DEFAULT NULL', 'nf3');
CALL ce_add_factor_cache_column('factor_source', 'VARCHAR(512) DEFAULT NULL', 'applicable_scope');
CALL ce_add_factor_cache_column('gwp_ch4', 'DECIMAL(28, 10) DEFAULT NULL', 'factor_source');
CALL ce_add_factor_cache_column('gwp_n2o', 'DECIMAL(28, 10) DEFAULT NULL', 'gwp_ch4');
CALL ce_add_factor_cache_column('gwp_hfcs', 'DECIMAL(28, 10) DEFAULT NULL', 'gwp_n2o');
CALL ce_add_factor_cache_column('gwp_pfcs', 'DECIMAL(28, 10) DEFAULT NULL', 'gwp_hfcs');
CALL ce_add_factor_cache_column('gwp_sf6', 'DECIMAL(28, 10) DEFAULT NULL', 'gwp_pfcs');
CALL ce_add_factor_cache_column('gwp_nf3', 'DECIMAL(28, 10) DEFAULT NULL', 'gwp_sf6');
CALL ce_add_factor_cache_column('factor_gwp', 'DECIMAL(28, 10) DEFAULT NULL', 'gwp_nf3');
CALL ce_add_factor_cache_column('version_province_code', 'VARCHAR(128) DEFAULT NULL', 'factor_gwp');
CALL ce_add_factor_cache_column('factor_version', 'VARCHAR(64) DEFAULT NULL', 'version_province_code');
CALL ce_add_factor_cache_column('division_code', 'VARCHAR(64) DEFAULT NULL', 'factor_version');
CALL ce_add_factor_cache_column('division_name', 'VARCHAR(128) DEFAULT NULL', 'division_code');
CALL ce_add_factor_cache_column('region_name', 'VARCHAR(128) DEFAULT NULL', 'division_name');
CALL ce_add_factor_cache_column('province_factor', 'DECIMAL(28, 10) DEFAULT NULL', 'region_name');
CALL ce_add_factor_cache_column('region_factor', 'DECIMAL(28, 10) DEFAULT NULL', 'province_factor');
CALL ce_add_factor_cache_column('national_factor', 'DECIMAL(28, 10) DEFAULT NULL', 'region_factor');
CALL ce_add_factor_cache_column('non_fossil_excluded_factor', 'DECIMAL(28, 10) DEFAULT NULL', 'national_factor');
CALL ce_add_factor_cache_column('national_fossil_power_factor', 'DECIMAL(28, 10) DEFAULT NULL', 'non_fossil_excluded_factor');
CALL ce_add_factor_cache_column('row_no', 'INT DEFAULT NULL', 'national_fossil_power_factor');
CALL ce_add_factor_cache_column('fuel_level1', 'VARCHAR(255) DEFAULT NULL', 'row_no');
CALL ce_add_factor_cache_column('fuel_level2', 'VARCHAR(255) DEFAULT NULL', 'fuel_level1');
CALL ce_add_factor_cache_column('fuel_level3', 'VARCHAR(255) DEFAULT NULL', 'fuel_level2');
CALL ce_add_factor_cache_column('fuel_level4', 'VARCHAR(255) DEFAULT NULL', 'fuel_level3');
CALL ce_add_factor_cache_column('lower_heat_value', 'DECIMAL(28, 10) DEFAULT NULL', 'fuel_level4');
CALL ce_add_factor_cache_column('lower_heat_value_cv', 'DECIMAL(28, 10) DEFAULT NULL', 'lower_heat_value');
CALL ce_add_factor_cache_column('co2_factor', 'DECIMAL(28, 10) DEFAULT NULL', 'lower_heat_value_cv');
CALL ce_add_factor_cache_column('co2_factor_cv', 'DECIMAL(28, 10) DEFAULT NULL', 'co2_factor');
CALL ce_add_factor_cache_column('gwp_value', 'DECIMAL(28, 10) DEFAULT NULL', 'co2_factor_cv');
CALL ce_add_factor_cache_column('converted_factor', 'DECIMAL(28, 10) DEFAULT NULL', 'gwp_value');

CALL ce_create_index_if_missing('idx_ce_factor_cache_record_version', '(cache_version_id)');
CALL ce_drop_index_if_exists('uk_ce_factor_cache_record');
ALTER TABLE ce_factor_cache_record
    ADD CONSTRAINT uk_ce_factor_cache_record UNIQUE (cache_version_id, factor_table_code, factor_code);
CALL ce_create_index_if_missing('idx_ce_factor_cache_record_table', '(factor_table_code)');

DROP PROCEDURE IF EXISTS ce_add_factor_cache_column;
DROP PROCEDURE IF EXISTS ce_drop_index_if_exists;
DROP PROCEDURE IF EXISTS ce_create_index_if_missing;
