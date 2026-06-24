-- Enterprise Source(A) relationship and safe read-view alignment.
-- Apply only to the enterprise database. Do not run against the vendor database.

DELIMITER //

DROP PROCEDURE IF EXISTS ce_add_index_if_missing//
CREATE PROCEDURE ce_add_index_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_sql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD ', p_index_sql);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_add_fk_if_missing//
CREATE PROCEDURE ce_add_fk_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_constraint_name VARCHAR(128),
    IN p_constraint_sql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND CONSTRAINT_NAME = p_constraint_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD CONSTRAINT ', p_constraint_name, ' ', p_constraint_sql);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_drop_fk_if_exists//
CREATE PROCEDURE ce_drop_fk_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_constraint_name VARCHAR(128)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND CONSTRAINT_NAME = p_constraint_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' DROP FOREIGN KEY ', p_constraint_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL ce_drop_fk_if_exists('ce_emission_source', 'fk_ce_emission_source_factor');
CALL ce_drop_fk_if_exists('ce_activity_data', 'fk_ce_activity_data_factor');
CALL ce_drop_fk_if_exists('ce_green_power_certificate', 'fk_ce_green_power_factor');

CALL ce_add_index_if_missing('ce_admin_division', 'uk_ce_admin_division_code', 'UNIQUE KEY uk_ce_admin_division_code (division_code)');
CALL ce_add_index_if_missing('ce_company_factory', 'uk_ce_company_factory_factory', 'UNIQUE KEY uk_ce_company_factory_factory (factory_code)');
CALL ce_add_index_if_missing('ce_company_factory', 'idx_ce_company_factory_code_name', 'KEY idx_ce_company_factory_code_name (factory_code, factory_name)');
CALL ce_add_index_if_missing('ce_emission_source_category', 'uk_ce_emission_source_category_sk', 'UNIQUE KEY uk_ce_emission_source_category_sk (category_sk)');
CALL ce_add_index_if_missing('ce_ef_factor', 'uk_ce_ef_factor_sk', 'UNIQUE KEY uk_ce_ef_factor_sk (factor_sk)');
CALL ce_add_index_if_missing('ce_emission_source', 'uk_ce_emission_source_code', 'UNIQUE KEY uk_ce_emission_source_code (source_identification_code)');
CALL ce_add_index_if_missing('ce_emission_source', 'idx_ce_emission_source_factor', 'KEY idx_ce_emission_source_factor (factor_key)');
CALL ce_add_index_if_missing('ce_activity_data', 'idx_ce_activity_data_required_submitter', 'KEY idx_ce_activity_data_required_submitter (responsible_dept, activity_year, activity_month, data_status)');
CALL ce_add_index_if_missing('ce_activity_data', 'idx_ce_activity_data_factor', 'KEY idx_ce_activity_data_factor (factor_key)');
CALL ce_add_index_if_missing('ce_green_power_certificate', 'idx_ce_green_power_required_submitter', 'KEY idx_ce_green_power_required_submitter (data_source, activity_year, activity_month, proof_status)');
CALL ce_add_index_if_missing('ce_green_power_certificate', 'idx_ce_green_power_factor', 'KEY idx_ce_green_power_factor (factor_key)');
CALL ce_add_index_if_missing('ce_intensity_denominator_fact', 'idx_ce_denominator_fact_quality', 'KEY idx_ce_denominator_fact_quality (factory_code, fact_year, fact_month, denominator_type)');
CALL ce_add_index_if_missing('ce_intensity_target', 'idx_ce_intensity_target_lookup', 'KEY idx_ce_intensity_target_lookup (factory_type, target_year)');

CREATE OR REPLACE VIEW ce_v_emission_source_user AS
SELECT
    es.source_identification_code,
    es.source_identification_name,
    es.emission_source_name,
    cf.company_name,
    cf.factory_code,
    cf.factory_name,
    cf.factory_type,
    cf.province_name,
    cat.ghg_scope,
    cat.ghg_scope_category,
    cat.iso_category,
    cat.gb_scope_category,
    es.responsible_dept,
    es.data_source,
    factor.emission_source_name AS factor_name,
    factor.factor_unit,
    factor.factor_gwp,
    es.enabled_flag,
    es.remark
FROM ce_emission_source es
LEFT JOIN ce_company_factory cf
    ON cf.factory_code = es.company_code
LEFT JOIN ce_emission_source_category cat
    ON cat.category_sk = es.source_category_key
LEFT JOIN ce_ef_factor factor
    ON factor.factor_sk = es.factor_key;

CREATE OR REPLACE VIEW ce_v_activity_data_user AS
SELECT
    ad.source_identification_code,
    COALESCE(ad.source_identification_name, es.source_identification_name) AS source_identification_name,
    COALESCE(ad.emission_source_name, es.emission_source_name) AS emission_source_name,
    COALESCE(ad.company_name, cf.company_name) AS company_name,
    COALESCE(ad.factory_name, cf.factory_name) AS factory_name,
    cat.ghg_scope,
    cat.ghg_scope_category,
    ad.activity_unit,
    ad.activity_year,
    ad.activity_month,
    ad.activity_date,
    ad.activity_value,
    ad.responsible_dept,
    ad.data_source,
    factor.emission_source_name AS factor_name,
    factor.factor_unit,
    ad.calculated_emission,
    ad.data_status,
    ad.source_remark,
    ad.create_time,
    ad.update_time
FROM ce_activity_data ad
LEFT JOIN ce_emission_source es
    ON es.source_identification_code = ad.source_identification_code
LEFT JOIN ce_company_factory cf
    ON cf.factory_code = ad.company_code
LEFT JOIN ce_emission_source_category cat
    ON cat.category_sk = ad.source_category_key
LEFT JOIN ce_ef_factor factor
    ON factor.factor_sk = ad.factor_key;

CREATE OR REPLACE VIEW ce_v_green_power_user AS
SELECT
    gp.factory_code,
    COALESCE(gp.factory_name, cf.factory_name) AS factory_name,
    cf.company_name,
    cf.province_name,
    gp.activity_year,
    gp.activity_month,
    cat.ghg_scope,
    cat.ghg_scope_category,
    gp.electricity_type,
    gp.electricity_type_desc,
    gp.quantity_kwh,
    gp.certificate_code,
    gp.issuing_org,
    gp.purchase_date,
    gp.expiry_date,
    gp.power_grid_region,
    gp.offset_power_source,
    gp.data_source,
    factor.emission_source_name AS factor_name,
    factor.factor_unit,
    gp.proof_status,
    gp.source_remark,
    gp.create_time,
    gp.update_time
FROM ce_green_power_certificate gp
LEFT JOIN ce_company_factory cf
    ON cf.factory_code = gp.factory_code
LEFT JOIN ce_emission_source_category cat
    ON cat.category_sk = gp.source_category_key
LEFT JOIN ce_ef_factor factor
    ON factor.factor_sk = gp.factor_key;

DROP PROCEDURE IF EXISTS ce_add_index_if_missing;
DROP PROCEDURE IF EXISTS ce_add_fk_if_missing;
DROP PROCEDURE IF EXISTS ce_drop_fk_if_exists;
