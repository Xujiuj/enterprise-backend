-- Conservative Source(A) dedupe for the enterprise database.
-- Keeps the lowest id in each business-key group and backs up removed rows.
-- Each run creates timestamped backup tables to avoid deleting from stale backups.

SET @ce_dedupe_run_id = DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%f');
SET @ce_emission_backup_table = CONCAT('ce_emission_source_dedupe_backup_', @ce_dedupe_run_id);
SET @ce_activity_backup_table = CONCAT('ce_activity_data_dedupe_backup_', @ce_dedupe_run_id);

SET @sql = CONCAT(
    'CREATE TABLE ', @ce_emission_backup_table, ' AS ',
    'SELECT es.* ',
    'FROM ce_emission_source es ',
    'JOIN (',
    '    SELECT company_code, source_identification_code, MIN(id) AS keep_id ',
    '    FROM ce_emission_source ',
    '    WHERE company_code IS NOT NULL ',
    '      AND source_identification_code IS NOT NULL ',
    '    GROUP BY company_code, source_identification_code ',
    '    HAVING COUNT(*) > 1',
    ') k ON k.company_code = es.company_code ',
    '   AND k.source_identification_code = es.source_identification_code ',
    '   AND es.id <> k.keep_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = CONCAT(
    'CREATE TABLE ', @ce_activity_backup_table, ' AS ',
    'SELECT ad.* ',
    'FROM ce_activity_data ad ',
    'JOIN (',
    '    SELECT company_code, source_identification_code, activity_year, activity_month, MIN(id) AS keep_id ',
    '    FROM ce_activity_data ',
    '    WHERE company_code IS NOT NULL ',
    '      AND source_identification_code IS NOT NULL ',
    '      AND activity_year IS NOT NULL ',
    '      AND activity_month IS NOT NULL ',
    '    GROUP BY company_code, source_identification_code, activity_year, activity_month ',
    '    HAVING COUNT(*) > 1',
    ') k ON k.company_code = ad.company_code ',
    '   AND k.source_identification_code = ad.source_identification_code ',
    '   AND k.activity_year = ad.activity_year ',
    '   AND k.activity_month = ad.activity_month ',
    '   AND ad.id <> k.keep_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = CONCAT(
    'SELECT ''backup.', @ce_emission_backup_table, ''' AS item, COUNT(*) AS row_count FROM ', @ce_emission_backup_table,
    ' UNION ALL SELECT ''backup.', @ce_activity_backup_table, ''', COUNT(*) FROM ', @ce_activity_backup_table
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ce_activity_data ad
JOIN ce_emission_source duplicate_es ON duplicate_es.id = ad.emission_source_id
JOIN (
    SELECT company_code, source_identification_code, MIN(id) AS keep_id
    FROM ce_emission_source
    WHERE company_code IS NOT NULL
      AND source_identification_code IS NOT NULL
    GROUP BY company_code, source_identification_code
    HAVING COUNT(*) > 1
) keep_es ON keep_es.company_code = duplicate_es.company_code
          AND keep_es.source_identification_code = duplicate_es.source_identification_code
   SET ad.emission_source_id = keep_es.keep_id,
       ad.factory_code = COALESCE(NULLIF(ad.factory_code, ''), NULLIF(ad.company_code, ''), duplicate_es.factory_code);

SET @sql = CONCAT(
    'DELETE ad FROM ce_activity_data ad JOIN ', @ce_activity_backup_table, ' b ON b.id = ad.id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = CONCAT(
    'DELETE es FROM ce_emission_source es JOIN ', @ce_emission_backup_table, ' b ON b.id = es.id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'remaining.ce_emission_source' AS item, COUNT(*) AS row_count FROM ce_emission_source
UNION ALL
SELECT 'remaining.ce_activity_data', COUNT(*) FROM ce_activity_data;
