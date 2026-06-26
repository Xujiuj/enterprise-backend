-- Read-only diagnostics for enterprise Source(A) dirty data.
-- Run against database `enterprise` before applying constraint redesign.

SELECT 'table.ce_emission_source' AS item, COUNT(*) AS row_count FROM ce_emission_source
UNION ALL SELECT 'table.ce_activity_data', COUNT(*) FROM ce_activity_data
UNION ALL SELECT 'table.ce_green_power_certificate', COUNT(*) FROM ce_green_power_certificate
UNION ALL SELECT 'table.ce_intensity_denominator_fact', COUNT(*) FROM ce_intensity_denominator_fact;

SELECT 'emission_source.factory_source.duplicate.summary' AS check_code,
       COUNT(*) AS duplicate_groups,
       COALESCE(SUM(duplicate_count), 0) AS rows_in_duplicate_groups,
       COALESCE(SUM(duplicate_count - 1), 0) AS extra_rows
FROM (
    SELECT company_code, source_identification_code, COUNT(*) AS duplicate_count
    FROM ce_emission_source
    WHERE company_code IS NOT NULL
      AND source_identification_code IS NOT NULL
    GROUP BY company_code, source_identification_code
    HAVING COUNT(*) > 1
) d;

SELECT 'activity.factory_source_period.duplicate.summary' AS check_code,
       COUNT(*) AS duplicate_groups,
       COALESCE(SUM(duplicate_count), 0) AS rows_in_duplicate_groups,
       COALESCE(SUM(duplicate_count - 1), 0) AS extra_rows
FROM (
    SELECT company_code, source_identification_code, activity_year, activity_month, COUNT(*) AS duplicate_count
    FROM ce_activity_data
    WHERE company_code IS NOT NULL
      AND source_identification_code IS NOT NULL
      AND activity_year IS NOT NULL
      AND activity_month IS NOT NULL
    GROUP BY company_code, source_identification_code, activity_year, activity_month
    HAVING COUNT(*) > 1
) d;

SELECT 'green_power.period_certificate.duplicate.summary' AS check_code,
       COUNT(*) AS duplicate_groups,
       COALESCE(SUM(duplicate_count), 0) AS rows_in_duplicate_groups,
       COALESCE(SUM(duplicate_count - 1), 0) AS extra_rows
FROM (
    SELECT factory_code, activity_year, activity_month, certificate_code, COUNT(*) AS duplicate_count
    FROM ce_green_power_certificate
    WHERE factory_code IS NOT NULL
      AND activity_year IS NOT NULL
      AND activity_month IS NOT NULL
      AND certificate_code IS NOT NULL
      AND certificate_code <> ''
    GROUP BY factory_code, activity_year, activity_month, certificate_code
    HAVING COUNT(*) > 1
) d;

SELECT 'denominator_fact.period_metric.duplicate.summary' AS check_code,
       COUNT(*) AS duplicate_groups,
       COALESCE(SUM(duplicate_count), 0) AS rows_in_duplicate_groups,
       COALESCE(SUM(duplicate_count - 1), 0) AS extra_rows
FROM (
    SELECT factory_code, fact_year, fact_month, denominator_metric_name, COUNT(*) AS duplicate_count
    FROM ce_intensity_denominator_fact
    WHERE factory_code IS NOT NULL
      AND fact_year IS NOT NULL
      AND fact_month IS NOT NULL
      AND denominator_metric_name IS NOT NULL
      AND denominator_metric_name <> ''
    GROUP BY factory_code, fact_year, fact_month, denominator_metric_name
    HAVING COUNT(*) > 1
) d;

SELECT 'emission_source.factory_source.duplicate' AS check_code,
       company_code,
       source_identification_code,
       COUNT(*) AS duplicate_count
FROM ce_emission_source
WHERE company_code IS NOT NULL
  AND source_identification_code IS NOT NULL
GROUP BY company_code, source_identification_code
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, company_code, source_identification_code
LIMIT 100;

SELECT 'activity.factory_source_period.duplicate' AS check_code,
       company_code,
       source_identification_code,
       activity_year,
       activity_month,
       COUNT(*) AS duplicate_count
FROM ce_activity_data
WHERE company_code IS NOT NULL
  AND source_identification_code IS NOT NULL
  AND activity_year IS NOT NULL
  AND activity_month IS NOT NULL
GROUP BY company_code, source_identification_code, activity_year, activity_month
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, company_code, source_identification_code, activity_year, activity_month
LIMIT 100;

SELECT 'green_power.period_certificate.duplicate' AS check_code,
       factory_code,
       activity_year,
       activity_month,
       certificate_code,
       COUNT(*) AS duplicate_count
FROM ce_green_power_certificate
WHERE factory_code IS NOT NULL
  AND activity_year IS NOT NULL
  AND activity_month IS NOT NULL
  AND certificate_code IS NOT NULL
  AND certificate_code <> ''
GROUP BY factory_code, activity_year, activity_month, certificate_code
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, factory_code, activity_year, activity_month
LIMIT 100;

SELECT 'denominator_fact.period_metric.duplicate' AS check_code,
       factory_code,
       fact_year,
       fact_month,
       denominator_metric_name,
       COUNT(*) AS duplicate_count
FROM ce_intensity_denominator_fact
WHERE factory_code IS NOT NULL
  AND fact_year IS NOT NULL
  AND fact_month IS NOT NULL
  AND denominator_metric_name IS NOT NULL
  AND denominator_metric_name <> ''
GROUP BY factory_code, fact_year, fact_month, denominator_metric_name
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, factory_code, fact_year, fact_month
LIMIT 100;

SELECT 'emission_source.company.orphan' AS check_code,
       es.company_code,
       COUNT(*) AS orphan_count
FROM ce_emission_source es
LEFT JOIN ce_company_factory cf ON cf.factory_code = es.company_code
WHERE es.company_code IS NOT NULL
  AND cf.id IS NULL
GROUP BY es.company_code
ORDER BY orphan_count DESC, es.company_code
LIMIT 100;

SELECT 'activity.factory_source.orphan' AS check_code,
       ad.company_code,
       ad.source_identification_code,
       COUNT(*) AS orphan_count
FROM ce_activity_data ad
LEFT JOIN ce_emission_source es
       ON es.company_code = ad.company_code
      AND es.source_identification_code = ad.source_identification_code
WHERE ad.source_identification_code IS NOT NULL
  AND ad.company_code IS NOT NULL
  AND es.id IS NULL
GROUP BY ad.company_code, ad.source_identification_code
ORDER BY orphan_count DESC, ad.company_code, ad.source_identification_code
LIMIT 100;

SELECT 'activity.company.orphan' AS check_code,
       ad.company_code,
       COUNT(*) AS orphan_count
FROM ce_activity_data ad
LEFT JOIN ce_company_factory cf ON cf.factory_code = ad.company_code
WHERE ad.company_code IS NOT NULL
  AND cf.id IS NULL
GROUP BY ad.company_code
ORDER BY orphan_count DESC, ad.company_code
LIMIT 100;

SELECT 'green_power.factory.orphan' AS check_code,
       gp.factory_code,
       COUNT(*) AS orphan_count
FROM ce_green_power_certificate gp
LEFT JOIN ce_company_factory cf ON cf.factory_code = gp.factory_code
WHERE gp.factory_code IS NOT NULL
  AND cf.id IS NULL
GROUP BY gp.factory_code
ORDER BY orphan_count DESC, gp.factory_code
LIMIT 100;

SELECT 'denominator_fact.factory.orphan' AS check_code,
       df.factory_code,
       COUNT(*) AS orphan_count
FROM ce_intensity_denominator_fact df
LEFT JOIN ce_company_factory cf ON cf.factory_code = df.factory_code
WHERE df.factory_code IS NOT NULL
  AND cf.id IS NULL
GROUP BY df.factory_code
ORDER BY orphan_count DESC, df.factory_code
LIMIT 100;
