Enterprise backend MySQL development scripts
============================================

Use this directory for enterprise-side local development DDL and seed data.
Business data in this project must remain local to the enterprise backend and
must not be mirrored into the vendor backend.

Current status:
- The copied RuoYi base scripts remain in `script/sql/` for framework setup.
- `carbon_enterprise_schema_v1.sql` is the first development DDL slice. It
  records Excel template versions, sheets, original fields, local capture
  batches/rows/cells, extension-field metadata, local license runtime state,
  and factor cache versions.
- The schema is metadata-first so the full Excel field inventory can be loaded
  before generated CRUD tables are finalized.
- Keep SQL Server migration compatibility in mind when adding MySQL types,
  indexes, views, or pagination assumptions.
