-- Excel-driven dynamic enterprise management pages.
-- Target: enterprise database only.
SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.ce_dynamic_module', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ce_dynamic_module (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_ce_dynamic_module PRIMARY KEY,
        module_code NVARCHAR(50) NOT NULL,
        module_name NVARCHAR(100) NOT NULL,
        table_name NVARCHAR(64) NOT NULL,
        sheet_name NVARCHAR(100) NULL,
        permission_prefix NVARCHAR(100) NOT NULL,
        menu_id BIGINT NULL,
        status NCHAR(1) NOT NULL CONSTRAINT df_ce_dynamic_module_status DEFAULT (N'0'),
        create_by BIGINT NULL,
        create_time DATETIME2 NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NULL
    );
    CREATE UNIQUE INDEX ux_ce_dynamic_module_code ON dbo.ce_dynamic_module(module_code);
    CREATE UNIQUE INDEX ux_ce_dynamic_module_table ON dbo.ce_dynamic_module(table_name);
END;
GO

IF OBJECT_ID(N'dbo.ce_dynamic_field', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ce_dynamic_field (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_ce_dynamic_field PRIMARY KEY,
        module_id BIGINT NOT NULL,
        field_code NVARCHAR(64) NOT NULL,
        field_name NVARCHAR(120) NOT NULL,
        db_column NVARCHAR(64) NOT NULL,
        value_type NVARCHAR(20) NOT NULL,
        ui_type NVARCHAR(20) NOT NULL,
        required_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_required DEFAULT (0),
        searchable_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_searchable DEFAULT (0),
        list_visible_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_list DEFAULT (1),
        form_visible_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_form DEFAULT (1),
        sort_order INT NOT NULL,
        max_length INT NULL,
        numeric_precision INT NULL,
        numeric_scale INT NULL,
        CONSTRAINT fk_ce_dynamic_field_module FOREIGN KEY (module_id) REFERENCES dbo.ce_dynamic_module(id)
    );
    CREATE UNIQUE INDEX ux_ce_dynamic_field_code ON dbo.ce_dynamic_field(module_id, field_code);
END;
GO

DECLARE @now DATETIME2 = SYSDATETIME();
DECLARE @menus TABLE (
    menu_id BIGINT PRIMARY KEY, menu_name NVARCHAR(100), parent_id BIGINT, order_num INT,
    path NVARCHAR(200), component NVARCHAR(255), query_param NVARCHAR(500), is_frame INT,
    is_cache INT, menu_type NVARCHAR(1), visible NVARCHAR(1), status NVARCHAR(1),
    perms NVARCHAR(200), icon NVARCHAR(100), remark NVARCHAR(500)
);

INSERT INTO @menus VALUES
    (900280, N'动态数据管理', 0, 7, N'dynamic-data', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'table', N'Excel动态页面目录'),
    (900281, N'页面生成管理', 900280, 1, N'module-generator', N'enterprise/dynamicModule/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:dynamicModule:list', N'upload', N'上传Excel生成管理页面'),
    (900282, N'Excel预览', 900281, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dynamicModule:preview', N'#', N'Excel页面结构预览权限'),
    (900283, N'生成页面', 900281, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dynamicModule:generate', N'#', N'生成动态页面权限');

MERGE dbo.sys_menu AS target
USING @menus AS source
ON target.menu_id = source.menu_id
WHEN MATCHED THEN UPDATE SET
    menu_name = source.menu_name, parent_id = source.parent_id, order_num = source.order_num,
    path = source.path, component = source.component, query_param = source.query_param,
    is_frame = source.is_frame, is_cache = source.is_cache, menu_type = source.menu_type,
    visible = source.visible, status = source.status, perms = source.perms, icon = source.icon,
    update_by = 1, update_time = @now, remark = source.remark
WHEN NOT MATCHED THEN INSERT
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
    (source.menu_id, source.menu_name, source.parent_id, source.order_num, source.path, source.component,
     source.query_param, source.is_frame, source.is_cache, source.menu_type, source.visible, source.status,
     source.perms, source.icon, 100, 1, @now, source.remark);

INSERT INTO dbo.sys_role_menu(role_id, menu_id)
SELECT 900001, source.menu_id
FROM @menus source
WHERE EXISTS (SELECT 1 FROM dbo.sys_role WHERE role_id = 900001)
  AND NOT EXISTS (
      SELECT 1 FROM dbo.sys_role_menu target
      WHERE target.role_id = 900001 AND target.menu_id = source.menu_id
  );
GO

SELECT N'carbon_enterprise_dynamic_module_ok' AS result;
