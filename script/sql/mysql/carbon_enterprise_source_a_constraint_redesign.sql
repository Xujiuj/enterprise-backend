-- Enterprise Source(A) constraint redesign for customer relationship alignment.
-- Database boundary: enterprise only. Run diagnostics first; clean duplicates
-- before adding unique keys or foreign keys to a dirty database.

DELIMITER //

DROP PROCEDURE IF EXISTS ce_add_column_if_missing//
CREATE PROCEDURE ce_add_column_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_column_name VARCHAR(128),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_add_index_if_missing//
CREATE PROCEDURE ce_add_index_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_sql TEXT
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
    IN p_column_name VARCHAR(128),
    IN p_constraint_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND CONSTRAINT_NAME = p_constraint_name
    ) AND NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
          AND REFERENCED_TABLE_NAME IS NOT NULL
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
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' DROP FOREIGN KEY ', p_constraint_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_drop_index_if_exists//
CREATE PROCEDURE ce_drop_index_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' DROP INDEX ', p_index_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ce_assert_zero//
CREATE PROCEDURE ce_assert_zero(
    IN p_check_code VARCHAR(128),
    IN p_problem_count BIGINT
)
BEGIN
    IF p_problem_count > 0 THEN
        SET @message_text = CONCAT('Source(A) constraint redesign blocked by dirty data: ', p_check_code, '=', p_problem_count);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @message_text;
    END IF;
END//

DELIMITER ;

CALL ce_add_column_if_missing('ce_emission_source', 'factory_code', 'VARCHAR(64) DEFAULT NULL AFTER company_name');
CALL ce_add_column_if_missing('ce_activity_data', 'emission_source_id', 'BIGINT DEFAULT NULL AFTER batch_id');
CALL ce_add_column_if_missing('ce_activity_data', 'activity_period', 'VARCHAR(32) DEFAULT NULL AFTER emission_source_id');
CALL ce_add_column_if_missing('ce_activity_data', 'factory_code', 'VARCHAR(64) DEFAULT NULL AFTER company_name');

CALL ce_drop_fk_if_exists('ce_activity_data', 'fk_ce_activity_data_source');
CALL ce_drop_index_if_exists('ce_activity_data', 'uk_ce_activity_data_source_period');
CALL ce_drop_index_if_exists('ce_emission_source', 'uk_ce_emission_source_code');

UPDATE ce_emission_source
   SET factory_code = company_code
 WHERE (factory_code IS NULL OR factory_code = '')
   AND company_code IS NOT NULL
   AND company_code <> '';

UPDATE ce_activity_data ad
LEFT JOIN ce_emission_source es
       ON es.source_identification_code = ad.source_identification_code
      AND (es.company_code = ad.company_code OR ad.company_code IS NULL OR ad.company_code = '')
   SET ad.emission_source_id = es.id,
       ad.activity_period = COALESCE(NULLIF(ad.activity_period, ''), CONCAT(ad.activity_year, '-', LPAD(ad.activity_month, 2, '0'))),
       ad.factory_code = COALESCE(NULLIF(ad.factory_code, ''), NULLIF(ad.company_code, ''), es.factory_code)
 WHERE (ad.emission_source_id IS NULL OR ad.emission_source_id = 0 OR ad.activity_period IS NULL OR ad.activity_period = '' OR ad.factory_code IS NULL OR ad.factory_code = '')
   AND es.id IS NOT NULL;

-- Diagnostic result sets. All duplicate/orphan counts must be zero before
-- this script can safely add the strict constraints below.
SELECT 'activity.factory_source_period.duplicate' AS check_code,
       COUNT(*) AS problem_count
FROM (
    SELECT company_code, source_identification_code, activity_year, activity_month, COUNT(*) AS cnt
    FROM ce_activity_data
    WHERE company_code IS NOT NULL
      AND source_identification_code IS NOT NULL
      AND activity_year IS NOT NULL
      AND activity_month IS NOT NULL
    GROUP BY company_code, source_identification_code, activity_year, activity_month
    HAVING COUNT(*) > 1
) d;

SELECT 'emission_source.factory_source.duplicate' AS check_code,
       COUNT(*) AS problem_count
FROM (
    SELECT company_code, source_identification_code, COUNT(*) AS cnt
    FROM ce_emission_source
    WHERE company_code IS NOT NULL
      AND source_identification_code IS NOT NULL
    GROUP BY company_code, source_identification_code
    HAVING COUNT(*) > 1
) d;

SELECT 'green_power.period_certificate.duplicate' AS check_code,
       COUNT(*) AS problem_count
FROM (
    SELECT factory_code, activity_year, activity_month, certificate_code, COUNT(*) AS cnt
    FROM ce_green_power_certificate
    WHERE factory_code IS NOT NULL
      AND activity_year IS NOT NULL
      AND activity_month IS NOT NULL
      AND certificate_code IS NOT NULL
    GROUP BY factory_code, activity_year, activity_month, certificate_code
    HAVING COUNT(*) > 1
) d;

SELECT 'denominator_fact.period_metric.duplicate' AS check_code,
       COUNT(*) AS problem_count
FROM (
    SELECT factory_code, fact_year, fact_month, denominator_metric_name, COUNT(*) AS cnt
    FROM ce_intensity_denominator_fact
    WHERE factory_code IS NOT NULL
      AND fact_year IS NOT NULL
      AND fact_month IS NOT NULL
      AND denominator_metric_name IS NOT NULL
    GROUP BY factory_code, fact_year, fact_month, denominator_metric_name
    HAVING COUNT(*) > 1
) d;

SELECT 'activity.source_id.orphan' AS check_code,
       COUNT(*) AS problem_count
FROM ce_activity_data ad
LEFT JOIN ce_emission_source es ON es.id = ad.emission_source_id
WHERE ad.emission_source_id IS NOT NULL
  AND es.id IS NULL;

SELECT 'activity.factory_source.orphan' AS check_code,
       COUNT(*) AS problem_count
FROM ce_activity_data ad
LEFT JOIN ce_emission_source es
       ON es.company_code = ad.company_code
      AND es.source_identification_code = ad.source_identification_code
WHERE ad.source_identification_code IS NOT NULL
  AND ad.company_code IS NOT NULL
  AND es.id IS NULL;

SELECT 'emission_source.factory_code.missing' AS check_code,
       COUNT(*) AS problem_count
FROM ce_emission_source es
LEFT JOIN ce_company_factory cf ON cf.factory_code = es.company_code
WHERE es.company_code IS NOT NULL
  AND cf.id IS NULL;

CALL ce_assert_zero('activity.factory_source_period.duplicate', (
    SELECT COUNT(*)
    FROM (
        SELECT company_code, source_identification_code, activity_year, activity_month
        FROM ce_activity_data
        WHERE company_code IS NOT NULL
          AND source_identification_code IS NOT NULL
          AND activity_year IS NOT NULL
          AND activity_month IS NOT NULL
        GROUP BY company_code, source_identification_code, activity_year, activity_month
        HAVING COUNT(*) > 1
    ) d
));
CALL ce_assert_zero('emission_source.factory_source.duplicate', (
    SELECT COUNT(*)
    FROM (
        SELECT company_code, source_identification_code
        FROM ce_emission_source
        WHERE company_code IS NOT NULL
          AND source_identification_code IS NOT NULL
        GROUP BY company_code, source_identification_code
        HAVING COUNT(*) > 1
    ) d
));
CALL ce_assert_zero('green_power.period_certificate.duplicate', (
    SELECT COUNT(*)
    FROM (
        SELECT factory_code, activity_year, activity_month, certificate_code
        FROM ce_green_power_certificate
        WHERE factory_code IS NOT NULL
          AND activity_year IS NOT NULL
          AND activity_month IS NOT NULL
          AND certificate_code IS NOT NULL
        GROUP BY factory_code, activity_year, activity_month, certificate_code
        HAVING COUNT(*) > 1
    ) d
));
CALL ce_assert_zero('denominator_fact.period_metric.duplicate', (
    SELECT COUNT(*)
    FROM (
        SELECT factory_code, fact_year, fact_month, denominator_metric_name
        FROM ce_intensity_denominator_fact
        WHERE factory_code IS NOT NULL
          AND fact_year IS NOT NULL
          AND fact_month IS NOT NULL
          AND denominator_metric_name IS NOT NULL
        GROUP BY factory_code, fact_year, fact_month, denominator_metric_name
        HAVING COUNT(*) > 1
    ) d
));
CALL ce_assert_zero('activity.source_id.orphan', (
    SELECT COUNT(*)
    FROM ce_activity_data ad
    LEFT JOIN ce_emission_source es ON es.id = ad.emission_source_id
    WHERE ad.emission_source_id IS NOT NULL
      AND es.id IS NULL
));
CALL ce_assert_zero('activity.factory_source.orphan', (
    SELECT COUNT(*)
    FROM ce_activity_data ad
    LEFT JOIN ce_emission_source es
           ON es.company_code = ad.company_code
          AND es.source_identification_code = ad.source_identification_code
    WHERE ad.source_identification_code IS NOT NULL
      AND ad.company_code IS NOT NULL
      AND es.id IS NULL
));
CALL ce_assert_zero('emission_source.factory_code.missing', (
    SELECT COUNT(*)
    FROM ce_emission_source es
    LEFT JOIN ce_company_factory cf ON cf.factory_code = es.company_code
    WHERE es.company_code IS NOT NULL
      AND cf.id IS NULL
));

CALL ce_add_index_if_missing('ce_emission_source', 'uk_ce_emission_source_factory_code',
    'UNIQUE KEY uk_ce_emission_source_factory_code (company_code, source_identification_code)');
CALL ce_add_index_if_missing('ce_emission_source', 'idx_ce_emission_source_factory',
    'KEY idx_ce_emission_source_factory (factory_code)');
CALL ce_add_index_if_missing('ce_activity_data', 'uk_ce_activity_data_source_period',
    'UNIQUE KEY uk_ce_activity_data_source_period (company_code, source_identification_code, activity_year, activity_month)');
CALL ce_add_index_if_missing('ce_activity_data', 'idx_ce_activity_data_emission_source_id',
    'KEY idx_ce_activity_data_emission_source_id (emission_source_id)');
CALL ce_add_index_if_missing('ce_activity_data', 'idx_ce_activity_data_factory',
    'KEY idx_ce_activity_data_factory (factory_code)');
CALL ce_add_index_if_missing('ce_green_power_certificate', 'uk_ce_green_power_certificate_period',
    'UNIQUE KEY uk_ce_green_power_certificate_period (factory_code, activity_year, activity_month, certificate_code)');
CALL ce_add_index_if_missing('ce_intensity_denominator_fact', 'uk_ce_denominator_fact_period_metric',
    'UNIQUE KEY uk_ce_denominator_fact_period_metric (factory_code, fact_year, fact_month, denominator_metric_name)');

CALL ce_add_fk_if_missing('ce_activity_data', 'fk_ce_activity_data_source_id', 'emission_source_id',
    'FOREIGN KEY (emission_source_id) REFERENCES ce_emission_source (id)');

DROP PROCEDURE IF EXISTS ce_add_column_if_missing;
DROP PROCEDURE IF EXISTS ce_add_index_if_missing;
DROP PROCEDURE IF EXISTS ce_add_fk_if_missing;
DROP PROCEDURE IF EXISTS ce_drop_fk_if_exists;
DROP PROCEDURE IF EXISTS ce_drop_index_if_exists;
DROP PROCEDURE IF EXISTS ce_assert_zero;
