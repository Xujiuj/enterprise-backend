-- Enterprise SQL Server runtime repair for vendor-sync field parity, report content, and RuoYi system menus.
-- This script is idempotent and targets the enterprise database only.

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.sys_menu', N'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.sys_menu WHERE menu_id = 900161)
    BEGIN
        UPDATE dbo.sys_menu
           SET menu_name = N'Content',
               parent_id = 900160,
               order_num = 1,
               path = N'content',
               component = N'enterprise/reports/index',
               perms = N'enterprise:reports:view',
               icon = N'chart',
               remark = N'Content',
               update_time = SYSDATETIME()
         WHERE menu_id = 900161;
    END;

    IF EXISTS (SELECT 1 FROM dbo.sys_menu WHERE menu_id = 900164)
    BEGIN
        UPDATE dbo.sys_menu
           SET menu_name = N'温室气体核算报表',
               parent_id = 900160,
               order_num = 2,
               path = N'powerbi-report',
               component = N'enterprise/reports/powerbi',
               perms = N'enterprise:reports:view',
               icon = N'chart',
               remark = N'Power BI温室气体核算报表',
               update_time = SYSDATETIME()
         WHERE menu_id = 900164;
    END
    ELSE IF EXISTS (SELECT 1 FROM dbo.sys_menu WHERE menu_id = 900160)
    BEGIN
        INSERT INTO dbo.sys_menu
            (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, remark, create_time)
        VALUES
            (900164, N'温室气体核算报表', 900160, 2, N'powerbi-report', N'enterprise/reports/powerbi', N'', 1, 0, N'C', N'0', N'0', N'enterprise:reports:view', N'chart', N'Power BI温室气体核算报表', SYSDATETIME());
    END;

    UPDATE dbo.sys_menu
       SET order_num = 3,
           update_time = SYSDATETIME()
     WHERE menu_id = 900162;

    UPDATE dbo.sys_menu
       SET order_num = 4,
           update_time = SYSDATETIME()
     WHERE menu_id = 900163;
END;
GO

IF OBJECT_ID(N'dbo.sys_role_menu', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.sys_role', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.sys_menu', N'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.sys_role_menu (role_id, menu_id)
    SELECT role.role_id, 900164
      FROM dbo.sys_role role
     WHERE role.role_id IN (1, 900001, 900002, 900005)
       AND EXISTS (SELECT 1 FROM dbo.sys_menu WHERE menu_id = 900164)
       AND NOT EXISTS (
           SELECT 1
             FROM dbo.sys_role_menu existing
            WHERE existing.role_id = role.role_id
              AND existing.menu_id = 900164
       );
END;
GO

IF OBJECT_ID(N'dbo.ce_report_content', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ce_report_content (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        directory_no INT NULL,
        directory_name NVARCHAR(255) NULL,
        subdirectory_no INT NULL,
        subdirectory_name NVARCHAR(255) NULL,
        chart_names NVARCHAR(MAX) NULL,
        display_order INT NOT NULL CONSTRAINT df_ce_report_content_display_order DEFAULT 0,
        create_time DATETIME2 NULL CONSTRAINT df_ce_report_content_create_time DEFAULT SYSDATETIME(),
        update_time DATETIME2 NULL CONSTRAINT df_ce_report_content_update_time DEFAULT SYSDATETIME(),
        remark NVARCHAR(500) NULL
    );
    CREATE INDEX idx_ce_report_content_order ON dbo.ce_report_content (display_order);
    CREATE INDEX idx_ce_report_content_directory ON dbo.ce_report_content (directory_no, subdirectory_no);
END;
GO

IF COL_LENGTH('dbo.ce_admin_division', 'parent_code') IS NULL ALTER TABLE dbo.ce_admin_division ADD parent_code NVARCHAR(64) NULL;
IF COL_LENGTH('dbo.ce_admin_division', 'level_type') IS NULL ALTER TABLE dbo.ce_admin_division ADD level_type NVARCHAR(64) NULL;
IF COL_LENGTH('dbo.ce_admin_division', 'sort_order') IS NULL ALTER TABLE dbo.ce_admin_division ADD sort_order INT NULL;
IF COL_LENGTH('dbo.ce_admin_division', 'status') IS NULL ALTER TABLE dbo.ce_admin_division ADD status NVARCHAR(16) NULL;

IF COL_LENGTH('dbo.ce_emission_source_category', 'parent_code') IS NULL ALTER TABLE dbo.ce_emission_source_category ADD parent_code NVARCHAR(64) NULL;
IF COL_LENGTH('dbo.ce_emission_source_category', 'category_name_en') IS NULL ALTER TABLE dbo.ce_emission_source_category ADD category_name_en NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.ce_emission_source_category', 'sort_order') IS NULL ALTER TABLE dbo.ce_emission_source_category ADD sort_order INT NULL;
IF COL_LENGTH('dbo.ce_emission_source_category', 'status') IS NULL ALTER TABLE dbo.ce_emission_source_category ADD status NVARCHAR(16) NULL;

IF COL_LENGTH('dbo.ce_base_year', 'base_year_key') IS NULL ALTER TABLE dbo.ce_base_year ADD base_year_key NVARCHAR(64) NULL;
IF COL_LENGTH('dbo.ce_base_year', 'description') IS NULL ALTER TABLE dbo.ce_base_year ADD description NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.ce_base_year', 'is_current') IS NULL ALTER TABLE dbo.ce_base_year ADD is_current INT NULL;
IF COL_LENGTH('dbo.ce_base_year', 'sort_order') IS NULL ALTER TABLE dbo.ce_base_year ADD sort_order INT NULL;
IF COL_LENGTH('dbo.ce_base_year', 'status') IS NULL ALTER TABLE dbo.ce_base_year ADD status NVARCHAR(16) NULL;

IF COL_LENGTH('dbo.ce_electricity_factor', 'sort_order') IS NULL ALTER TABLE dbo.ce_electricity_factor ADD sort_order INT NULL;
IF COL_LENGTH('dbo.ce_electricity_factor', 'status') IS NULL ALTER TABLE dbo.ce_electricity_factor ADD status NVARCHAR(16) NULL;

IF COL_LENGTH('dbo.ce_electricity_factor_version_map', 'sort_order') IS NULL ALTER TABLE dbo.ce_electricity_factor_version_map ADD sort_order INT NULL;
IF COL_LENGTH('dbo.ce_electricity_factor_version_map', 'status') IS NULL ALTER TABLE dbo.ce_electricity_factor_version_map ADD status NVARCHAR(16) NULL;

IF COL_LENGTH('dbo.ce_electricity_factor_scope', 'sort_order') IS NULL ALTER TABLE dbo.ce_electricity_factor_scope ADD sort_order INT NULL;
IF COL_LENGTH('dbo.ce_electricity_factor_scope', 'status') IS NULL ALTER TABLE dbo.ce_electricity_factor_scope ADD status NVARCHAR(16) NULL;

IF COL_LENGTH('dbo.ce_greenhouse_gas', 'gwp_value') IS NULL ALTER TABLE dbo.ce_greenhouse_gas ADD gwp_value DECIMAL(28, 10) NULL;
IF COL_LENGTH('dbo.ce_greenhouse_gas', 'gwp_version') IS NULL ALTER TABLE dbo.ce_greenhouse_gas ADD gwp_version NVARCHAR(64) NULL;
IF COL_LENGTH('dbo.ce_greenhouse_gas', 'chemical_formula') IS NULL ALTER TABLE dbo.ce_greenhouse_gas ADD chemical_formula NVARCHAR(64) NULL;
IF COL_LENGTH('dbo.ce_greenhouse_gas', 'sort_order') IS NULL ALTER TABLE dbo.ce_greenhouse_gas ADD sort_order INT NULL;
IF COL_LENGTH('dbo.ce_greenhouse_gas', 'status') IS NULL ALTER TABLE dbo.ce_greenhouse_gas ADD status NVARCHAR(16) NULL;

IF COL_LENGTH('dbo.ce_template_field', 'business_field_code') IS NULL ALTER TABLE dbo.ce_template_field ADD business_field_code NVARCHAR(64) NULL;
GO

IF COL_LENGTH('dbo.ce_template_field', 'target_column_code') IS NOT NULL
BEGIN
    EXEC(N'
        UPDATE dbo.ce_template_field
           SET business_field_code = COALESCE(NULLIF(target_column_code, ''''), NULLIF(original_field_name, ''''))
         WHERE business_field_code IS NULL OR business_field_code = ''''
    ');
END
ELSE
BEGIN
    UPDATE dbo.ce_template_field
       SET business_field_code = original_field_name
     WHERE (business_field_code IS NULL OR business_field_code = N'')
       AND original_field_name IS NOT NULL
       AND original_field_name <> N'';
END;

IF NOT EXISTS (
       SELECT 1
         FROM sys.key_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.ce_template_field')
          AND name = N'uk_ce_template_field_business_code'
          AND type = N'UQ'
   )
   AND NOT EXISTS (
       SELECT 1
         FROM dbo.ce_template_field
        WHERE business_field_code IS NULL
           OR business_field_code = N''
   )
   AND NOT EXISTS (
       SELECT 1
         FROM dbo.ce_template_field
        GROUP BY sheet_id, business_field_code
       HAVING COUNT(*) > 1
   )
BEGIN
    ALTER TABLE dbo.ce_template_field ADD CONSTRAINT uk_ce_template_field_business_code UNIQUE (sheet_id, business_field_code);
END;
GO

DECLARE @now DATETIME2 = SYSDATETIME();
DECLARE @systemManagement NVARCHAR(32) = NCHAR(31995)+NCHAR(32479)+NCHAR(31649)+NCHAR(29702);
DECLARE @userManagement NVARCHAR(32) = NCHAR(29992)+NCHAR(25143)+NCHAR(31649)+NCHAR(29702);
DECLARE @roleManagement NVARCHAR(32) = NCHAR(35282)+NCHAR(33394)+NCHAR(31649)+NCHAR(29702);
DECLARE @menuManagement NVARCHAR(32) = NCHAR(33756)+NCHAR(21333)+NCHAR(31649)+NCHAR(29702);
DECLARE @deptManagement NVARCHAR(32) = NCHAR(37096)+NCHAR(38376)+NCHAR(31649)+NCHAR(29702);
DECLARE @postManagement NVARCHAR(32) = NCHAR(23703)+NCHAR(20301)+NCHAR(31649)+NCHAR(29702);

IF OBJECT_ID(N'dbo.sys_menu', N'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.sys_menu WHERE menu_id = 1)
        UPDATE dbo.sys_menu
           SET menu_name = @systemManagement,
               parent_id = 0,
               order_num = 8,
               path = N'system',
               component = N'Layout',
               menu_type = N'M',
               visible = N'0',
               status = N'0',
               update_time = @now
         WHERE menu_id = 1;
    ELSE
        INSERT INTO dbo.sys_menu
            (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
        VALUES
            (1, @systemManagement, 0, 8, N'system', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'system', 103, 1, @now, @systemManagement);

    UPDATE dbo.sys_menu
       SET menu_name = CASE menu_id WHEN 100 THEN @userManagement WHEN 101 THEN @roleManagement WHEN 102 THEN @menuManagement ELSE menu_name END,
           parent_id = 1,
           order_num = CASE menu_id WHEN 100 THEN 1 WHEN 101 THEN 2 WHEN 102 THEN 3 ELSE order_num END,
           path = CASE menu_id WHEN 100 THEN N'user' WHEN 101 THEN N'role' WHEN 102 THEN N'menu' ELSE path END,
           component = CASE menu_id WHEN 100 THEN N'system/user/index' WHEN 101 THEN N'system/role/index' WHEN 102 THEN N'system/menu/index' ELSE component END,
           menu_type = N'C',
           visible = N'0',
           status = N'0',
           update_time = @now
     WHERE menu_id IN (100, 101, 102);

    IF NOT EXISTS (SELECT 1 FROM dbo.sys_menu WHERE menu_id = 103)
        INSERT INTO dbo.sys_menu
            (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
        VALUES
            (103, @deptManagement, 1, 4, N'dept', N'system/dept/index', N'', 1, 0, N'C', N'0', N'0', N'system:dept:list', N'tree', 103, 1, @now, @deptManagement);
    ELSE
        UPDATE dbo.sys_menu
           SET menu_name = @deptManagement,
               parent_id = 1,
               order_num = 4,
               path = N'dept',
               component = N'system/dept/index',
               menu_type = N'C',
               visible = N'0',
               status = N'0',
               perms = N'system:dept:list',
               icon = N'tree',
               update_time = @now
         WHERE menu_id = 103;

    IF NOT EXISTS (SELECT 1 FROM dbo.sys_menu WHERE menu_id = 104)
        INSERT INTO dbo.sys_menu
            (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
        VALUES
            (104, @postManagement, 1, 5, N'post', N'system/post/index', N'', 1, 0, N'C', N'0', N'0', N'system:post:list', N'post', 103, 1, @now, @postManagement);
    ELSE
        UPDATE dbo.sys_menu
           SET menu_name = @postManagement,
               parent_id = 1,
               order_num = 5,
               path = N'post',
               component = N'system/post/index',
               menu_type = N'C',
               visible = N'0',
               status = N'0',
               perms = N'system:post:list',
               icon = N'post',
               update_time = @now
         WHERE menu_id = 104;

    MERGE dbo.sys_menu AS target
    USING (VALUES
        (1051, NCHAR(37096)+NCHAR(38376)+NCHAR(26597)+NCHAR(35810), 103, 1, N'system:dept:query'),
        (1052, NCHAR(37096)+NCHAR(38376)+NCHAR(26032)+NCHAR(22686), 103, 2, N'system:dept:add'),
        (1053, NCHAR(37096)+NCHAR(38376)+NCHAR(20462)+NCHAR(25913), 103, 3, N'system:dept:edit'),
        (1054, NCHAR(37096)+NCHAR(38376)+NCHAR(21024)+NCHAR(38500), 103, 4, N'system:dept:remove'),
        (1061, NCHAR(23703)+NCHAR(20301)+NCHAR(26597)+NCHAR(35810), 104, 1, N'system:post:query'),
        (1062, NCHAR(23703)+NCHAR(20301)+NCHAR(26032)+NCHAR(22686), 104, 2, N'system:post:add'),
        (1063, NCHAR(23703)+NCHAR(20301)+NCHAR(20462)+NCHAR(25913), 104, 3, N'system:post:edit'),
        (1064, NCHAR(23703)+NCHAR(20301)+NCHAR(21024)+NCHAR(38500), 104, 4, N'system:post:remove'),
        (1065, NCHAR(23703)+NCHAR(20301)+NCHAR(23548)+NCHAR(20986), 104, 5, N'system:post:export')
    ) AS source(menu_id, menu_name, parent_id, order_num, perms)
    ON target.menu_id = source.menu_id
    WHEN MATCHED THEN
        UPDATE SET menu_name = source.menu_name,
                   parent_id = source.parent_id,
                   order_num = source.order_num,
                   path = N'#',
                   component = N'',
                   query_param = N'',
                   is_frame = 1,
                   is_cache = 0,
                   menu_type = N'F',
                   visible = N'1',
                   status = N'0',
                   perms = source.perms,
                   icon = N'#',
                   update_time = @now
    WHEN NOT MATCHED THEN
        INSERT (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
        VALUES (source.menu_id, source.menu_name, source.parent_id, source.order_num, N'#', N'', N'', 1, 0, N'F', N'1', N'0', source.perms, N'#', 103, 1, @now, source.menu_name);
END;
GO

IF OBJECT_ID(N'dbo.sys_role_menu', N'U') IS NOT NULL AND OBJECT_ID(N'dbo.sys_role', N'U') IS NOT NULL AND OBJECT_ID(N'dbo.sys_menu', N'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.sys_role_menu (role_id, menu_id)
    SELECT role.role_id, menu.menu_id
      FROM dbo.sys_role role
      CROSS JOIN dbo.sys_menu menu
     WHERE role.status = N'0'
       AND role.del_flag = N'0'
       AND menu.menu_id IN (1, 100, 101, 102, 103, 104, 1051, 1052, 1053, 1054, 1061, 1062, 1063, 1064, 1065)
       AND NOT EXISTS (
           SELECT 1
             FROM dbo.sys_role_menu existing
            WHERE existing.role_id = role.role_id
              AND existing.menu_id = menu.menu_id
       );
END;
GO

IF OBJECT_ID(N'dbo.sys_menu', N'U') IS NOT NULL
BEGIN
    UPDATE dbo.sys_menu
       SET component = N'enterprise/dimension/index',
           query_param = N'{"code":"intensity-target"}',
           perms = N'enterprise:dimension:list',
           update_time = SYSDATETIME()
     WHERE menu_id = 900152
       AND path = N'intensity-target';
END;
GO

SELECT N'enterprise_runtime_repair_20260628_ok' AS result;
GO
