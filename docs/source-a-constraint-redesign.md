# Source(A) enterprise constraint redesign

## Evidence

- Customer workbook `source（A）/ALL/1 排放源识别表.xlsx` defines factory rows in `102公司表` with `BK_工厂编号`, and emission-source rows in `104排放源识别*` with `FK_公司编号` pointing to that factory code.
- Customer activity workbooks define monthly facts with `PK_排放源识别编号`, `年度`, `月份`, and `活动数据`.
- Local analysis in `tools/source_a_schema_analysis.txt` found 8323 activity rows. `PK_排放源识别编号 + 年度 + 月份` is unique; `PK_排放源识别编号` alone repeats monthly.
- `factor_key` may legally point to dynamic electricity-factor references outside `ce_ef_factor`, so factor columns remain indexed but not strict foreign keys.

## Target relationships

- `ce_company_factory.factory_code` is the enterprise-side factory business key.
- `ce_emission_source.source_identification_code` remains the stable emission-source business key.
- `ce_emission_source.company_code` is retained for existing API compatibility, but semantically stores Source(A) `FK_公司编号`/factory code. `factory_code` is added as an explicit alias for future code.
- `ce_activity_data.emission_source_id` links to `ce_emission_source.id`; `source_identification_code` is retained for imports, filters, and compatibility.
- `ce_activity_data` is unique by `(source_identification_code, activity_year, activity_month)`.
- `ce_green_power_certificate` is unique by `(factory_code, activity_year, activity_month, certificate_code)`.
- `ce_intensity_denominator_fact` is unique by `(factory_code, fact_year, fact_month, denominator_metric_name)`.

## Execution order

1. Run `script/sql/mysql/carbon_enterprise_source_a_dirty_data_diagnostics.sql` against `enterprise`.
2. Clean duplicate/orphan rows until diagnostics return no blocking rows.
3. Run `script/sql/mysql/carbon_enterprise_source_a_constraint_redesign.sql`.
4. Re-run diagnostics and Source(A) import validation.

## RuoYi generator note

The affected module already exists and includes hand-written import/validation services around generated CRUD. The schema change is therefore implemented as a migration and minimal service alignment instead of regenerating all enterprise carbon tables.
