# Source(A) Enterprise Constraint Redesign

## Evidence

- Customer workbook `source(A) ALL/1 排放源识别表.xlsx` defines factory rows in `102公司表` with `BK_工厂编号`, and emission-source rows in `104排放源识别` with factory code pointing to that key.
- Customer activity workbooks define monthly facts with emission-source identification code, year, month, and activity value.
- Activity facts repeat by emission source across months, so uniqueness belongs on source plus activity period, not source alone.
- Factor keys may legally point to dynamic electricity-factor references outside the static EF table, so factor columns remain indexed but are not strict foreign keys.

## Target Relationships

- `ce_company_factory.factory_code` is the enterprise-side factory business key.
- `ce_emission_source.source_identification_code` is the stable emission-source business key.
- `ce_emission_source.company_code` currently stores the Source(A) factory code; new code should prefer explicit factory semantics.
- `ce_activity_data.emission_source_id` links to `ce_emission_source.id`; `source_identification_code` remains for imports and filters.
- `ce_activity_data` is unique by `(source_identification_code, activity_year, activity_month)`.
- `ce_green_power_certificate` is unique by `(factory_code, activity_year, activity_month, certificate_code)`.
- `ce_intensity_denominator_fact` is unique by `(factory_code, fact_year, fact_month, denominator_metric_name)`.

## Execution Order

1. Run current SQL Server diagnostics under `script/sql/sqlserver/` against `enterprise`.
2. Clean duplicate or orphan rows until diagnostics return no blocking rows.
3. Apply the current SQL Server schema migration.
4. Re-run diagnostics and Source(A) import validation.

## RuoYi Generator Note

The affected module already exists and includes handwritten import and validation services around generated CRUD. Schema changes are therefore implemented as migrations plus minimal service alignment instead of regenerating all enterprise carbon tables.
