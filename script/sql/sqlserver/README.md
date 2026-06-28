Enterprise SQL Server scripts
=============================

SQL Server is the only supported enterprise database target.

The enterprise backend keeps customer database connection details outside YAML
configuration. Operators provide the JDBC URL, username, password, and driver by
environment or deployment-specific configuration so enterprise database
credentials are not disclosed to the vendor side.

Runtime schema hardening that must be present on SQL Server is enforced by:

- `ruoyi-modules/carbon-enterprise/src/main/java/org/dromara/carbon/enterprise/shared/config/CeSchemaMigrationRunner.java`

Initial enterprise data and RBAC can be initialized with:

- `carbon_enterprise_init.sql`

Run it against the enterprise database after the RuoYi base SQL has created the
system tables:

```bash
sqlcmd -S <sqlserver-host> -d <enterprise-db> -U <enterprise-db-user> -P <password> -i carbon_enterprise_init.sql
```

The script does not contain database connection details. It seeds the private
enterprise tenant, base system configuration, dictionary data, OSS/client
configuration, the default admin account, enterprise portal menus, button
permissions, and preset roles (`enterprise_admin`, `enterprise_operator`,
`enterprise_reporter`, `enterprise_reviewer`, `enterprise_report_viewer`).

The initialized enterprise database has no active authorization state:
`ce_license_state` is emptied and tenant license fields are reset. Operators
must import a valid License after deployment through the enterprise license
import flow.

Do not add legacy-compatible enterprise scripts back into this delivery.
