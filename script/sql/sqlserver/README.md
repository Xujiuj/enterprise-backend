Enterprise SQL Server scripts
=============================

SQL Server initialization scripts have been removed from the enterprise backend.
The supported initialization path is now the local MySQL `enterprise` database
through:

- `../enterprise_init.sql`
- `../enterprise_test_data.sql`

Do not recreate SQL Server or vendor-database scripts here unless the target
database support policy changes.
