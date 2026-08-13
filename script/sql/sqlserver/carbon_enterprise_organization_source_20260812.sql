-- Department management is the only organization source.
-- Company and factory rows are maintained as a compatibility projection.
SET NOCOUNT ON;
GO

IF COL_LENGTH(N'dbo.sys_dept', N'factory_code') IS NULL
BEGIN
    ALTER TABLE dbo.sys_dept ADD factory_code NVARCHAR(64) NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_sys_dept_organization_projection' AND object_id = OBJECT_ID(N'dbo.sys_dept'))
BEGIN
    CREATE INDEX ix_sys_dept_organization_projection
        ON dbo.sys_dept (dept_category, parent_id, factory_code, status, del_flag);
END;
GO
