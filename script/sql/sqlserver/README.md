Enterprise SQL Server migration scripts
=======================================

Use this directory for enterprise-side SQL Server migration and acceptance
scripts.

Current status:
- `carbon_enterprise_schema_v1.sql` mirrors the MySQL development foundation
  and creates the initial `rpt` schema views.
- `carbon_enterprise_permissions_v1.sql` reserves the read-only `pbi_user`
  permission shape for Power BI access.
- Enterprise business data remains local to this backend. Vendor access is not
  granted to capture rows, cells, green-power proof details, or intensity facts.
