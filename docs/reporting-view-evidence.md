# EB-5 Reporting View Evidence

## Scope

Repository scope for this slice is `D:\project\fx\enterprise-backend` only.
No vendor project, package, or parent-workspace file was modified.

## Conclusion

No SQL change was required for EB-5.

The existing SQL Server reporting scripts already satisfy the requested first
`rpt` business reporting view acceptance:

- `rpt.v_CaptureRows` already exists under the `rpt` schema.
- `rpt.v_CaptureRows` is a business-facing view over capture batch/row data.
- The view already includes an explicit License gate with
  `WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate)`.
- The permission scaffold already grants `pbi_user` read access to `rpt` and
  denies `dbo` read/write access.

## Evidence

### Existing reporting views

From `script/sql/sqlserver/carbon_enterprise_schema_v1.sql`:

- `rpt.v_LicenseGate` is defined as the gate behavior view.
- `rpt.v_CaptureRows` is defined as the first business reporting view candidate.
- `rpt.v_CaptureRows` ends with
  `WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate);`

This means the business view is exposed through `rpt` and is explicitly gated
by License state without granting Power BI direct raw-table access.

### Reporting permissions

From `script/sql/sqlserver/carbon_enterprise_permissions_v1.sql`:

- `GRANT SELECT ON SCHEMA::rpt TO pbi_user;`
- `DENY SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo TO pbi_user;`

This satisfies the reporting boundary requirement that Power BI reads `rpt`
only and does not read raw `dbo`.

## Review Notes

- EB-5 acceptance is already met by the current SQL scripts.
- The smallest safe slice is documentation-only evidence.
- No broad schema change was justified because changing an already compliant
  view would add risk without improving acceptance coverage.

## Verification Run

- Parent boundary script: `rtk python ..\tools\verify_sql_boundaries.py`
  Result: `verify-ok enterprise_tables=9 vendor_tables=9 sqlserver=ready`
- Repo grep verification:
  `rtk rg -n "CREATE VIEW rpt\.|WHERE EXISTS \(SELECT 1 FROM rpt\.v_LicenseGate\)|GRANT SELECT ON SCHEMA::rpt|DENY SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo" script/sql/sqlserver docs/reporting-view-evidence.md`
  Result: matched `rpt.v_LicenseGate`, `rpt.v_CaptureRows`, the explicit
  `WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate)` gate, the `rpt` grant, and
  the `dbo` deny.
