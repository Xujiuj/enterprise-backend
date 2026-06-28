Enterprise SQL Server scripts
=============================

SQL Server is the only supported enterprise database target.

The enterprise backend keeps customer database connection details outside YAML
configuration. Operators provide the JDBC URL, username, password, and driver by
environment or deployment-specific configuration so enterprise database
credentials are not disclosed to the vendor side.

Runtime schema hardening that must be present on SQL Server is enforced by:

- `ruoyi-modules/carbon-enterprise/src/main/java/org/dromara/carbon/enterprise/shared/config/CeSchemaMigrationRunner.java`

Do not add MySQL-compatible enterprise scripts back into this delivery.
