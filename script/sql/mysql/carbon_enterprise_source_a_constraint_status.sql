-- Read-only constraint/index status for enterprise Source(A) tables.

SHOW CREATE TABLE ce_emission_source;
SHOW CREATE TABLE ce_activity_data;

SELECT table_name,
       constraint_name,
       constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name IN (
      'ce_emission_source',
      'ce_activity_data',
      'ce_green_power_certificate',
      'ce_intensity_denominator_fact'
  )
ORDER BY table_name, constraint_type, constraint_name;

SELECT table_name,
       index_name,
       non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN (
      'ce_emission_source',
      'ce_activity_data',
      'ce_green_power_certificate',
      'ce_intensity_denominator_fact'
  )
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;
