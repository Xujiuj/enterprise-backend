-- Enterprise SQL Server reporting permission scaffold.
-- Run after carbon_enterprise_schema_v1.sql and create/login-map pbi_user
-- according to the deployment environment's credential policy.

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'pbi_user')
    CREATE USER pbi_user WITHOUT LOGIN;
GO

GRANT SELECT ON SCHEMA::rpt TO pbi_user;
GO

DENY SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo TO pbi_user;
GO
