Enterprise backend MySQL scripts
================================

Enterprise initialization SQL has been consolidated into the root `script/sql`
directory.

Current files:
- `../enterprise_init.sql`: creates the local MySQL `enterprise` database,
  recreates RuoYi core tables and enterprise `ce_*` business tables, and seeds
  enterprise menus, preset roles, role-menu permissions, config, client, OSS,
  and report metadata.
- `../enterprise_test_data.sql`: inserts repeatable local test users and
  enterprise business sample data. Run it only after `enterprise_init.sql`.

Do not write enterprise business data into the vendor database.
