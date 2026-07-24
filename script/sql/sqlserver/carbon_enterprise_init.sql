-- Enterprise SQL Server initialization.
-- Target: the connected enterprise database only.
-- Prerequisite: RuoYi base system tables have already been created.

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.sys_menu', N'U') IS NULL
    THROW 51000, 'Missing dbo.sys_menu. Run the RuoYi base SQL before carbon_enterprise_init.sql.', 1;
IF OBJECT_ID(N'dbo.sys_role', N'U') IS NULL
    THROW 51000, 'Missing dbo.sys_role. Run the RuoYi base SQL before carbon_enterprise_init.sql.', 1;
IF OBJECT_ID(N'dbo.sys_role_menu', N'U') IS NULL
    THROW 51000, 'Missing dbo.sys_role_menu. Run the RuoYi base SQL before carbon_enterprise_init.sql.', 1;
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

IF OBJECT_ID(N'dbo.ce_report_setting', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ce_report_setting (
        setting_key NVARCHAR(100) NOT NULL CONSTRAINT pk_ce_report_setting PRIMARY KEY,
        setting_value NVARCHAR(MAX) NOT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.ce_report_setting WHERE setting_key = N'powerbi.embedUrl')
    INSERT INTO dbo.ce_report_setting(setting_key, setting_value, update_by, update_time)
    VALUES (
        N'powerbi.embedUrl',
        N'https://app.powerbi.com/view?r=eyJrIjoiYjQzODVjYmEtYzFiMy00NDQ0LWIwZTAtMjM2YmVjOWNlZDAyIiwidCI6ImU2NDExZmRiLTZkNzctNGZmZC1iMDE1LTYxOWM3NWIxMzc2OCIsImMiOjEwfQ%3D%3D',
        NULL,
        SYSDATETIME()
    );

IF OBJECT_ID(N'dbo.ce_emission_source_category', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.ce_emission_source_category')
          AND name = N'ix_ce_emission_source_category_version'
   )
    CREATE INDEX ix_ce_emission_source_category_version
        ON dbo.ce_emission_source_category(version_no, category_sk, is_current);
GO

IF OBJECT_ID(N'dbo.ce_extension_field', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ce_extension_field (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_ce_extension_field PRIMARY KEY,
        template_version_id BIGINT NOT NULL CONSTRAINT df_ce_extension_field_template_version DEFAULT (1),
        module_code NVARCHAR(80) NOT NULL,
        sheet_id BIGINT NOT NULL CONSTRAINT df_ce_extension_field_sheet DEFAULT (1),
        field_code NVARCHAR(80) NOT NULL,
        field_name NVARCHAR(120) NOT NULL,
        value_type NVARCHAR(30) NOT NULL CONSTRAINT df_ce_extension_field_value_type DEFAULT (N'text'),
        enabled_flag BIT NOT NULL CONSTRAINT df_ce_extension_field_enabled DEFAULT (1),
        create_time DATETIME2 NULL
    );
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.ce_extension_field')
      AND name = N'ux_ce_extension_field_module_code'
)
    CREATE UNIQUE INDEX ux_ce_extension_field_module_code
        ON dbo.ce_extension_field(module_code, field_code);

IF OBJECT_ID(N'dbo.ce_extension_field_value', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ce_extension_field_value (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_ce_extension_field_value PRIMARY KEY,
        owner_table_code NVARCHAR(128) NOT NULL,
        owner_record_id BIGINT NOT NULL,
        extension_field_id BIGINT NOT NULL,
        text_value NVARCHAR(2000) NULL,
        decimal_value DECIMAL(30,10) NULL,
        date_value DATETIME2 NULL,
        boolean_value BIT NULL,
        create_time DATETIME2 NULL,
        update_time DATETIME2 NULL
    );
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.ce_extension_field_value')
      AND name = N'ux_ce_extension_field_value_owner_field'
)
    CREATE UNIQUE INDEX ux_ce_extension_field_value_owner_field
        ON dbo.ce_extension_field_value(owner_table_code, owner_record_id, extension_field_id);

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'dbo.ce_extension_field_value')
      AND name = N'fk_ce_extension_field_value_field'
)
    ALTER TABLE dbo.ce_extension_field_value
        ADD CONSTRAINT fk_ce_extension_field_value_field
        FOREIGN KEY (extension_field_id) REFERENCES dbo.ce_extension_field(id);
GO

DECLARE @now DATETIME2 = SYSDATETIME();
DECLARE @tenantId NVARCHAR(20) = N'000000';
DECLARE @createDept BIGINT = 100;
DECLARE @createBy BIGINT = 1;
DECLARE @adminPassword NVARCHAR(100) = N'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2';

IF OBJECT_ID(N'dbo.sys_tenant', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_tenant AS target
    USING (VALUES
        (1, @tenantId, N'企业管理员', N'15888888888', N'企业碳数据管理平台', N'企业端私有化部署租户', -1, N'0', N'0')
    ) AS source(id, tenant_id, contact_user_name, contact_phone, company_name, intro, create_dept, status, del_flag)
    ON target.tenant_id = source.tenant_id
    WHEN MATCHED THEN
        UPDATE SET contact_user_name = source.contact_user_name,
                   contact_phone = source.contact_phone,
                   company_name = source.company_name,
                   intro = source.intro,
                   license_number = NULL,
                   package_id = NULL,
                   expire_time = NULL,
                   account_count = -1,
                   status = source.status,
                   del_flag = source.del_flag,
                   update_by = @createBy,
                   update_time = @now
    WHEN NOT MATCHED THEN
        INSERT (id, tenant_id, contact_user_name, contact_phone, company_name, address, intro, domain, package_id,
                expire_time, account_count, license_number, create_dept, status, del_flag, create_by, update_by,
                create_time, update_time, remark)
        VALUES (source.id, source.tenant_id, source.contact_user_name, source.contact_phone, source.company_name,
                NULL, source.intro, NULL, NULL, NULL, -1, NULL, source.create_dept, source.status,
                source.del_flag, @createBy, NULL, @now, NULL, NULL);
END;

-- Fresh enterprise initialization must start without any authorization state.
-- Operators import a valid license after deployment through the license import flow.
IF OBJECT_ID(N'dbo.ce_license_state', N'U') IS NOT NULL
BEGIN
    DELETE FROM dbo.ce_license_state;
END;

IF OBJECT_ID(N'dbo.sys_dept', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_dept AS target
    USING (VALUES
        (100, @tenantId, 0, N'0', N'企业总部', 0, N'0', N'0', 100)
    ) AS source(dept_id, tenant_id, parent_id, ancestors, dept_name, order_num, status, del_flag, create_dept)
    ON target.dept_id = source.dept_id
    WHEN MATCHED THEN
        UPDATE SET tenant_id = source.tenant_id,
                   parent_id = source.parent_id,
                   ancestors = source.ancestors,
                   dept_name = source.dept_name,
                   order_num = source.order_num,
                   status = source.status,
                   del_flag = source.del_flag,
                   update_by = @createBy,
                   update_time = @now
    WHEN NOT MATCHED THEN
        INSERT (dept_id, tenant_id, parent_id, ancestors, dept_name, dept_category, order_num, leader, phone,
                email, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
        VALUES (source.dept_id, source.tenant_id, source.parent_id, source.ancestors, source.dept_name, NULL,
                source.order_num, N'1', N'15888888888', N'admin@example.com', source.status, source.del_flag,
                source.create_dept, @createBy, @now, NULL, NULL);
END;

IF OBJECT_ID(N'dbo.sys_post', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_post AS target
    USING (VALUES
        (1, @tenantId, @createDept, N'carbon_admin', N'碳管理负责人', 1, N'0')
    ) AS source(post_id, tenant_id, dept_id, post_code, post_name, post_sort, status)
    ON target.post_id = source.post_id
    WHEN MATCHED THEN
        UPDATE SET tenant_id = source.tenant_id,
                   post_code = source.post_code,
                   dept_id = source.dept_id,
                   post_name = source.post_name,
                   post_sort = source.post_sort,
                   status = source.status,
                   update_by = @createBy,
                   update_time = @now
    WHEN NOT MATCHED THEN
        INSERT (post_id, tenant_id, dept_id, post_code, post_category, post_name, post_sort, status, create_dept,
                create_by, create_time, update_by, update_time, remark)
        VALUES (source.post_id, source.tenant_id, source.dept_id, source.post_code, NULL, source.post_name, source.post_sort,
                source.status, @createDept, @createBy, @now, NULL, NULL, N'');
END;

IF OBJECT_ID(N'dbo.sys_client', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_client AS target
    USING (VALUES
        (1, N'e5cd7e4891bf95d1d19206ce24a7b32e', N'pc', N'pc123', N'password,social', N'pc', 3600, 3600, N'0', N'0'),
        (2, N'428a8310cd442757ae699df5d894f051', N'app', N'app123', N'password,sms,social', N'android', 3600, 3600, N'0', N'0')
    ) AS source(id, client_id, client_key, client_secret, grant_type, device_type, active_timeout, timeout, status, del_flag)
    ON target.client_id = source.client_id
    WHEN MATCHED THEN
        UPDATE SET client_key = source.client_key,
                   client_secret = source.client_secret,
                   grant_type = source.grant_type,
                   device_type = source.device_type,
                   active_timeout = source.active_timeout,
                   timeout = source.timeout,
                   status = source.status,
                   del_flag = source.del_flag,
                   update_by = @createBy,
                   update_time = @now
    WHEN NOT MATCHED THEN
        INSERT (id, client_id, client_key, client_secret, grant_type, device_type, active_timeout, timeout,
                status, del_flag, create_dept, create_by, create_time, update_by, update_time)
        VALUES (source.id, source.client_id, source.client_key, source.client_secret, source.grant_type,
                source.device_type, source.active_timeout, source.timeout, source.status, source.del_flag,
                @createDept, @createBy, @now, NULL, NULL);
END;

IF OBJECT_ID(N'dbo.sys_config', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_config AS target
    USING (VALUES
        (1, @tenantId, N'主框架页-默认皮肤样式名称', N'sys.index.skinName', N'skin-blue', N'Y', N'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'),
        (2, @tenantId, N'用户管理-账号初始密码', N'sys.user.initPassword', N'123456', N'Y', N'初始化密码 123456'),
        (3, @tenantId, N'主框架页-侧边栏主题', N'sys.index.sideTheme', N'theme-dark', N'Y', N'深色主题theme-dark，浅色主题theme-light'),
        (5, @tenantId, N'账号自助-是否开启用户注册功能', N'sys.account.registerUser', N'false', N'Y', N'是否开启注册用户功能（true开启，false关闭）'),
        (11, @tenantId, N'OSS预览列表资源开关', N'sys.oss.previewListResource', N'true', N'Y', N'true:开启, false:关闭'),
        (20, @tenantId, N'企业端授权验签公钥', N'carbon.license.public-key-pem',
         N'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8//eDDZSHROkF3uJdVTnjuV5ZSeQ2gpgLJVGSAt7jKb9YPgX5gENaFftwOEapBwZ6a45olIVfk4V+lsElpx7mAaXn/n7/q0Du1hevMcI3f4xVBP4b8JsTHnTbbl3CJ2Sgz7kYlllwvubbBZuI/K7/BU8ZwMW7a2xSo0tlEJgEhRZ0Psd0j696OOGnrbcWWX80wPRP7L5QUdwawC67DSmA1YEOZj7ksF8KMavJVSHTFIt5um4SLbdsNHSA6R4ebyFfwkrp3bcDsgM780Y/NaImCqLKR5OUdDKVL0e+cA5qqASLYnUw/dYPLprEmCSG+QzXoOwSMXRul3QJISlGZpTXwIDAQAB',
         N'Y', N'厂商端 local-key-2026-06-08 对应 RSA 公钥，仅用于企业端验签 .lic')
    ) AS source(config_id, tenant_id, config_name, config_key, config_value, config_type, remark)
    ON target.config_id = source.config_id
    WHEN MATCHED THEN
        UPDATE SET tenant_id = source.tenant_id,
                   config_name = source.config_name,
                   config_key = source.config_key,
                   config_value = source.config_value,
                   config_type = source.config_type,
                   update_by = @createBy,
                   update_time = @now,
                   remark = source.remark
    WHEN NOT MATCHED THEN
        INSERT (config_id, tenant_id, config_name, config_key, config_value, config_type,
                create_dept, create_by, create_time, update_by, update_time, remark)
        VALUES (source.config_id, source.tenant_id, source.config_name, source.config_key,
                source.config_value, source.config_type, @createDept, @createBy, @now, NULL, NULL, source.remark);
END;

IF OBJECT_ID(N'dbo.sys_oss_config', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_oss_config AS target
    USING (VALUES
        (1, @tenantId, N'minio', N'ruoyi', N'ruoyi123', N'ruoyi', N'', N'127.0.0.1:9000', N'', N'N', N'', N'1', N'0', N'', NULL),
        (2, @tenantId, N'qiniu', N'XXXXXXXXXXXXXXXX', N'XXXXXXXXXXXXXXX', N'ruoyi', N'', N's3-cn-north-1.qiniucs.com', N'', N'N', N'', N'1', N'1', N'', NULL),
        (3, @tenantId, N'aliyun', N'XXXXXXXXXXXXXXX', N'XXXXXXXXXXXXXXX', N'ruoyi', N'', N'oss-cn-beijing.aliyuncs.com', N'', N'N', N'', N'1', N'1', N'', NULL),
        (4, @tenantId, N'qcloud', N'XXXXXXXXXXXXXXX', N'XXXXXXXXXXXXXXX', N'ruoyi-1250000000', N'', N'cos.ap-beijing.myqcloud.com', N'', N'N', N'ap-beijing', N'1', N'1', N'', NULL),
        (5, @tenantId, N'image', N'ruoyi', N'ruoyi123', N'ruoyi', N'image', N'127.0.0.1:9000', N'', N'N', N'', N'1', N'1', N'', NULL)
    ) AS source(oss_config_id, tenant_id, config_key, access_key, secret_key, bucket_name, prefix, endpoint, domain,
                is_https, region, access_policy, status, ext1, remark)
    ON target.oss_config_id = source.oss_config_id
    WHEN MATCHED THEN
        UPDATE SET tenant_id = source.tenant_id,
                   config_key = source.config_key,
                   access_key = source.access_key,
                   secret_key = source.secret_key,
                   bucket_name = source.bucket_name,
                   prefix = source.prefix,
                   endpoint = source.endpoint,
                   domain = source.domain,
                   is_https = source.is_https,
                   region = source.region,
                   access_policy = source.access_policy,
                   status = source.status,
                   ext1 = source.ext1,
                   update_by = @createBy,
                   update_time = @now,
                   remark = source.remark
    WHEN NOT MATCHED THEN
        INSERT (oss_config_id, tenant_id, config_key, access_key, secret_key, bucket_name, prefix, endpoint,
                domain, is_https, region, access_policy, status, ext1, create_dept, create_by, create_time,
                update_by, update_time, remark)
        VALUES (source.oss_config_id, source.tenant_id, source.config_key, source.access_key, source.secret_key,
                source.bucket_name, source.prefix, source.endpoint, source.domain, source.is_https, source.region,
                source.access_policy, source.status, source.ext1, @createDept, @createBy, @now, NULL, NULL, source.remark);
END;

IF OBJECT_ID(N'dbo.sys_dict_type', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_dict_type AS target
    USING (VALUES
        (1, @tenantId, N'用户性别', N'sys_user_sex', N'用户性别列表'),
        (2, @tenantId, N'菜单状态', N'sys_show_hide', N'菜单状态列表'),
        (3, @tenantId, N'系统开关', N'sys_normal_disable', N'系统开关列表'),
        (4, @tenantId, N'任务状态', N'sys_job_status', N'任务状态列表'),
        (5, @tenantId, N'任务分组', N'sys_job_group', N'任务分组列表'),
        (6, @tenantId, N'系统是否', N'sys_yes_no', N'系统是否列表'),
        (7, @tenantId, N'通知类型', N'sys_notice_type', N'通知类型列表'),
        (8, @tenantId, N'通知状态', N'sys_notice_status', N'通知状态列表'),
        (9, @tenantId, N'操作类型', N'sys_oper_type', N'操作类型列表'),
        (10, @tenantId, N'系统状态', N'sys_common_status', N'登录状态列表'),
        (11, @tenantId, N'授权类型', N'sys_grant_type', N'认证授权类型'),
        (12, @tenantId, N'设备类型', N'sys_device_type', N'客户端设备类型')
    ) AS source(dict_id, tenant_id, dict_name, dict_type, remark)
    ON target.dict_id = source.dict_id
    WHEN MATCHED THEN
        UPDATE SET tenant_id = source.tenant_id,
                   dict_name = source.dict_name,
                   dict_type = source.dict_type,
                   update_by = @createBy,
                   update_time = @now,
                   remark = source.remark
    WHEN NOT MATCHED THEN
        INSERT (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
        VALUES (source.dict_id, source.tenant_id, source.dict_name, source.dict_type, @createDept, @createBy, @now, NULL, NULL, source.remark);
END;

IF OBJECT_ID(N'dbo.sys_dict_data', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_dict_data AS target
    USING (VALUES
        (1, @tenantId, 1, N'男', N'0', N'sys_user_sex', N'', N'', N'Y', N'性别男'),
        (2, @tenantId, 2, N'女', N'1', N'sys_user_sex', N'', N'', N'N', N'性别女'),
        (3, @tenantId, 3, N'未知', N'2', N'sys_user_sex', N'', N'', N'N', N'性别未知'),
        (4, @tenantId, 1, N'显示', N'0', N'sys_show_hide', N'', N'primary', N'Y', N'显示菜单'),
        (5, @tenantId, 2, N'隐藏', N'1', N'sys_show_hide', N'', N'danger', N'N', N'隐藏菜单'),
        (6, @tenantId, 1, N'正常', N'0', N'sys_normal_disable', N'', N'primary', N'Y', N'正常状态'),
        (7, @tenantId, 2, N'停用', N'1', N'sys_normal_disable', N'', N'danger', N'N', N'停用状态'),
        (8, @tenantId, 1, N'正常', N'0', N'sys_job_status', N'', N'primary', N'Y', N'正常状态'),
        (9, @tenantId, 2, N'暂停', N'1', N'sys_job_status', N'', N'danger', N'N', N'停用状态'),
        (10, @tenantId, 1, N'默认', N'DEFAULT', N'sys_job_group', N'', N'', N'Y', N'默认分组'),
        (11, @tenantId, 2, N'系统', N'SYSTEM', N'sys_job_group', N'', N'', N'N', N'系统分组'),
        (12, @tenantId, 1, N'是', N'Y', N'sys_yes_no', N'', N'primary', N'Y', N'系统默认是'),
        (13, @tenantId, 2, N'否', N'N', N'sys_yes_no', N'', N'danger', N'N', N'系统默认否'),
        (14, @tenantId, 1, N'通知', N'1', N'sys_notice_type', N'', N'warning', N'Y', N'通知'),
        (15, @tenantId, 2, N'公告', N'2', N'sys_notice_type', N'', N'success', N'N', N'公告'),
        (16, @tenantId, 1, N'正常', N'0', N'sys_notice_status', N'', N'primary', N'Y', N'正常状态'),
        (17, @tenantId, 2, N'关闭', N'1', N'sys_notice_status', N'', N'danger', N'N', N'关闭状态'),
        (18, @tenantId, 1, N'新增', N'1', N'sys_oper_type', N'', N'info', N'N', N'新增操作'),
        (19, @tenantId, 2, N'修改', N'2', N'sys_oper_type', N'', N'info', N'N', N'修改操作'),
        (20, @tenantId, 3, N'删除', N'3', N'sys_oper_type', N'', N'danger', N'N', N'删除操作'),
        (21, @tenantId, 4, N'授权', N'4', N'sys_oper_type', N'', N'primary', N'N', N'授权操作'),
        (22, @tenantId, 5, N'导出', N'5', N'sys_oper_type', N'', N'warning', N'N', N'导出操作'),
        (23, @tenantId, 6, N'导入', N'6', N'sys_oper_type', N'', N'warning', N'N', N'导入操作'),
        (24, @tenantId, 7, N'强退', N'7', N'sys_oper_type', N'', N'danger', N'N', N'强退操作'),
        (25, @tenantId, 8, N'生成代码', N'8', N'sys_oper_type', N'', N'warning', N'N', N'生成操作'),
        (26, @tenantId, 9, N'清空数据', N'9', N'sys_oper_type', N'', N'danger', N'N', N'清空操作'),
        (27, @tenantId, 1, N'成功', N'0', N'sys_common_status', N'', N'primary', N'N', N'正常状态'),
        (28, @tenantId, 2, N'失败', N'1', N'sys_common_status', N'', N'danger', N'N', N'停用状态'),
        (29, @tenantId, 99, N'其他', N'0', N'sys_oper_type', N'', N'info', N'N', N'其他操作'),
        (30, @tenantId, 0, N'密码认证', N'password', N'sys_grant_type', N'', N'default', N'N', N'密码认证'),
        (31, @tenantId, 0, N'短信认证', N'sms', N'sys_grant_type', N'', N'default', N'N', N'短信认证'),
        (32, @tenantId, 0, N'邮件认证', N'email', N'sys_grant_type', N'', N'default', N'N', N'邮件认证'),
        (33, @tenantId, 0, N'小程序认证', N'xcx', N'sys_grant_type', N'', N'default', N'N', N'小程序认证'),
        (34, @tenantId, 0, N'三方登录认证', N'social', N'sys_grant_type', N'', N'default', N'N', N'三方登录认证'),
        (35, @tenantId, 0, N'PC', N'pc', N'sys_device_type', N'', N'default', N'N', N'PC'),
        (36, @tenantId, 0, N'安卓', N'android', N'sys_device_type', N'', N'default', N'N', N'安卓'),
        (37, @tenantId, 0, N'iOS', N'ios', N'sys_device_type', N'', N'default', N'N', N'iOS'),
        (38, @tenantId, 0, N'小程序', N'xcx', N'sys_device_type', N'', N'default', N'N', N'小程序')
    ) AS source(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, remark)
    ON target.dict_code = source.dict_code
    WHEN MATCHED THEN
        UPDATE SET tenant_id = source.tenant_id,
                   dict_sort = source.dict_sort,
                   dict_label = source.dict_label,
                   dict_value = source.dict_value,
                   dict_type = source.dict_type,
                   css_class = source.css_class,
                   list_class = source.list_class,
                   is_default = source.is_default,
                   update_by = @createBy,
                   update_time = @now,
                   remark = source.remark
    WHEN NOT MATCHED THEN
        INSERT (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                is_default, create_dept, create_by, create_time, update_by, update_time, remark)
        VALUES (source.dict_code, source.tenant_id, source.dict_sort, source.dict_label, source.dict_value,
                source.dict_type, source.css_class, source.list_class, source.is_default, @createDept,
                @createBy, @now, NULL, NULL, source.remark);
END;

IF OBJECT_ID(N'dbo.sys_user', N'U') IS NOT NULL
BEGIN
    MERGE dbo.sys_user AS target
    USING (VALUES
        (1, @tenantId, @createDept, N'admin', N'企业管理员', N'sys_user', N'admin@example.com', N'15888888888', N'1',
         @adminPassword, N'0', N'0', N'初始化管理员，密码 admin123')
    ) AS source(user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, password,
                status, del_flag, remark)
    ON target.tenant_id = source.tenant_id AND target.user_name = source.user_name
    WHEN MATCHED THEN
        UPDATE SET dept_id = source.dept_id,
                   nick_name = source.nick_name,
                   user_type = source.user_type,
                   email = source.email,
                   phonenumber = source.phonenumber,
                   sex = source.sex,
                   password = source.password,
                   status = source.status,
                   del_flag = source.del_flag,
                   update_by = @createBy,
                   update_time = @now,
                   remark = source.remark
    WHEN NOT MATCHED THEN
        INSERT (user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
                password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time,
                update_by, update_time, remark)
        VALUES (source.user_id, source.tenant_id, source.dept_id, source.user_name, source.nick_name,
                source.user_type, source.email, source.phonenumber, source.sex, NULL, source.password,
                source.status, source.del_flag, N'127.0.0.1', @now, @createDept, @createBy, @now, NULL,
                NULL, source.remark);
END;

DECLARE @menus TABLE (
    menu_id BIGINT NOT NULL PRIMARY KEY,
    menu_name NVARCHAR(100) NOT NULL,
    parent_id BIGINT NOT NULL,
    order_num INT NOT NULL,
    path NVARCHAR(200) NOT NULL,
    component NVARCHAR(255) NOT NULL,
    query_param NVARCHAR(500) NOT NULL,
    is_frame INT NOT NULL,
    is_cache INT NOT NULL,
    menu_type NVARCHAR(1) NOT NULL,
    visible NVARCHAR(1) NOT NULL,
    status NVARCHAR(1) NOT NULL,
    perms NVARCHAR(200) NOT NULL,
    icon NVARCHAR(100) NOT NULL,
    remark NVARCHAR(500) NOT NULL
);

INSERT INTO @menus
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type,
     visible, status, perms, icon, remark)
VALUES
    (1, N'系统管理', 0, 8, N'system', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'system', N'系统管理目录'),
    (100, N'用户管理', 1, 1, N'user', N'system/user/index', N'', 1, 0, N'C', N'0', N'0', N'system:user:list', N'user', N'用户管理'),
    (101, N'角色管理', 1, 2, N'role', N'system/role/index', N'', 1, 0, N'C', N'0', N'0', N'system:role:list', N'peoples', N'角色管理'),
    (102, N'菜单管理', 1, 3, N'menu', N'system/menu/index', N'', 1, 0, N'C', N'0', N'0', N'system:menu:list', N'tree-table', N'菜单管理'),
    (900107, N'扩展字段配置', 1, 5, N'extension-field', N'enterprise/extensionField/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:extensionField:list', N'form', N'扩展字段配置'),
    (900280, N'动态数据管理', 0, 7, N'dynamic-data', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'table', N'Excel动态页面目录'),
    (900281, N'页面生成管理', 900280, 1, N'module-generator', N'enterprise/dynamicModule/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:dynamicModule:list', N'upload', N'上传Excel生成管理页面'),
    (900282, N'Excel预览', 900281, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dynamicModule:preview', N'#', N'Excel页面结构预览权限'),
    (900283, N'生成页面', 900281, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dynamicModule:generate', N'#', N'生成动态页面权限'),
    (900284, N'Power BI链接配置', 900164, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:reports:powerbi:edit', N'#', N'配置企业Power BI嵌入链接'),

    (900100, N'系统授权', 0, 1, N'system-auth', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'link', N'系统授权目录'),
    (900102, N'授权管理', 900100, 1, N'license-import', N'enterprise/licenseImport/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:licenseImport:import', N'link', N'授权管理'),
    (900103, N'授权导入接口', 900100, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:licenseImport:import', N'#', N'授权导入接口权限'),
    (900104, N'授权状态查询', 900100, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:licenseState:query', N'#', N'授权状态查询权限'),
    (900105, N'授权状态列表', 900100, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:licenseState:list', N'#', N'授权状态列表权限'),
    (900106, N'工作台总览', 900100, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:workbench:overview', N'#', N'工作台总览接口权限'),

    (900110, N'1 配置排放源', 0, 2, N'emission-source-config', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'list', N'配置排放源目录'),
    (900111, N'101 行政区划', 900110, 1, N'admin-division', N'enterprise/dimension/index', N'{"code":"admin-division"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'link', N'行政区划'),
    (900112, N'102 公司表', 900110, 2, N'company', N'enterprise/dimension/index', N'{"code":"company"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'form', N'公司表'),
    (900113, N'103 排放源分类', 900110, 3, N'emission-source-category', N'enterprise/dimension/index', N'{"code":"emission-source-category"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'link', N'排放源分类'),
    (900114, N'104 排放源识别', 900110, 4, N'emission-source', N'enterprise/emissionSource/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:emissionSource:list', N'form', N'排放源识别'),
    (900115, N'106 基准年维度表', 900110, 5, N'base-year', N'enterprise/dimension/index', N'{"code":"base-year"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'link', N'基准年维度表'),

    (900120, N'2 确认排放因子', 0, 3, N'factor-confirm', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'list', N'确认排放因子目录'),
    (900121, N'201 EF排放因子维度表', 900120, 1, N'ef-factor', N'enterprise/dimension/index', N'{"code":"ef-factor"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'form', N'EF排放因子维度表'),
    (900122, N'202 EF电力因子维度表', 900120, 2, N'ef-electricity-factor', N'enterprise/dimension/index', N'{"code":"ef-electricity-factor"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'link', N'EF电力因子维度表'),
    (900123, N'203 EF电力因子版本对应', 900120, 3, N'ef-electricity-version', N'enterprise/dimension/index', N'{"code":"ef-electricity-version"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'link', N'EF电力因子版本对应'),
    (900124, N'205 EF电力因子口径维度', 900120, 4, N'ef-electricity-scope', N'enterprise/dimension/index', N'{"code":"ef-electricity-scope"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'link', N'EF电力因子口径维度'),
    (900125, N'206 温室气体维度', 900120, 5, N'greenhouse-gas', N'enterprise/dimension/index', N'{"code":"greenhouse-gas"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'link', N'温室气体维度'),

    (900130, N'3 活动数据', 0, 4, N'activity-data', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'list', N'活动数据目录'),
    (900131, N'排放活动数据', 900130, 1, N'emission-activity-data', N'enterprise/activityData/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:activityData:list', N'form', N'排放活动数据'),

    (900140, N'4 绿电绿证', 0, 5, N'green-electricity', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'list', N'绿电绿证目录'),
    (900141, N'401 绿电绿证数据', 900140, 1, N'green-electricity-data', N'enterprise/greenPowerCertificate/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:greenPowerCertificate:list', N'international', N'绿电绿证数据'),

    (900150, N'5 强度管理', 0, 6, N'intensity', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'list', N'强度管理目录'),
    (900151, N'501 碳排放强度分母维度表', 900150, 1, N'intensity-denominator', N'enterprise/dimension/index', N'{"code":"intensity-denominator"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'form', N'碳排放强度分母维度表'),
    (900152, N'502 强度目标表', 900150, 2, N'intensity-target', N'enterprise/dimension/index', N'{"code":"intensity-target"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'form', N'强度目标表'),
    (900153, N'503 分母事实表', 900150, 3, N'denominator-fact', N'enterprise/dimension/index', N'{"code":"denominator-fact"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'form', N'分母事实表'),
    (900154, N'504 碳排放强度容忍率参数表', 900150, 4, N'intensity-tolerance', N'enterprise/dimension/index', N'{"code":"intensity-tolerance"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'form', N'碳排放强度容忍率参数表'),

    (900160, N'报表管理', 0, 7, N'report-management', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'chart', N'报表管理目录'),
    (900161, N'Content', 900160, 1, N'content', N'enterprise/reports/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:reports:view', N'link', N'Content'),
    (900164, N'温室气体核算报表', 900160, 2, N'powerbi-report', N'enterprise/reports/powerbi', N'', 1, 0, N'C', N'0', N'0', N'enterprise:reports:view', N'chart', N'Power BI温室气体核算报表'),
    (900162, N'数据验证', 900160, 3, N'data-validation', N'enterprise/dataValidation/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:dataValidation:view', N'validCode', N'数据验证'),
    (900163, N'报表模板下载', 900160, 4, N'report-template-download', N'enterprise/reportTemplateFile/index', N'', 1, 0, N'C', N'0', N'0', N'enterprise:reportTemplateFile:list', N'link', N'报表模板下载'),
    (900165, N'103版本记录', 900160, 5, N'emission-source-category-history', N'enterprise/dimension/index', N'{"code":"emission-source-category-history"}', 1, 0, N'C', N'0', N'0', N'enterprise:dimension:list', N'history', N'103排放源分类全部历史版本'),

    (900230, N'日志', 0, 9, N'log', N'Layout', N'', 1, 0, N'M', N'0', N'0', N'', N'log', N'日志目录'),
    (900231, N'操作日志', 900230, 1, N'operlog', N'monitor/operlog/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:operlog:list', N'form', N'操作日志'),
    (900232, N'登录日志', 900230, 2, N'logininfor', N'monitor/logininfor/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:logininfor:list', N'logininfor', N'登录日志'),

    (1001, N'用户查询', 100, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:user:query', N'#', N'用户查询权限'),
    (1002, N'用户新增', 100, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:user:add', N'#', N'用户新增权限'),
    (1003, N'用户修改', 100, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:user:edit', N'#', N'用户修改权限'),
    (1004, N'用户删除', 100, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:user:remove', N'#', N'用户删除权限'),
    (1005, N'用户导出', 100, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:user:export', N'#', N'用户导出权限'),
    (1006, N'用户导入', 100, 6, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:user:import', N'#', N'用户导入权限'),
    (1007, N'重置密码', 100, 7, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:user:resetPwd', N'#', N'重置密码权限'),
    (1008, N'角色查询', 101, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:role:query', N'#', N'角色查询权限'),
    (1009, N'角色新增', 101, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:role:add', N'#', N'角色新增权限'),
    (1010, N'角色修改', 101, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:role:edit', N'#', N'角色修改权限'),
    (1011, N'角色删除', 101, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:role:remove', N'#', N'角色删除权限'),
    (1012, N'角色导出', 101, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:role:export', N'#', N'角色导出权限'),
    (1013, N'菜单查询', 102, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:menu:query', N'#', N'菜单查询权限'),
    (1014, N'菜单新增', 102, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:menu:add', N'#', N'菜单新增权限'),
    (1015, N'菜单修改', 102, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:menu:edit', N'#', N'菜单修改权限'),
    (1016, N'菜单删除', 102, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'system:menu:remove', N'#', N'菜单删除权限'),

    (900170, N'维度列表查询', 900110, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dimension:list', N'#', N'维度列表查询权限'),
    (900171, N'维度详情查询', 900110, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dimension:query', N'#', N'维度详情查询权限'),
    (900172, N'维度新增', 900110, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dimension:add', N'#', N'维度新增权限'),
    (900173, N'维度修改', 900110, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dimension:edit', N'#', N'维度修改权限'),
    (900174, N'维度删除', 900110, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dimension:remove', N'#', N'维度删除权限'),
    (900175, N'维度同步', 900110, 6, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dimensionSync:refresh', N'#', N'维度同步权限'),
    (900176, N'维度同步状态', 900110, 7, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dimensionSync:status', N'#', N'维度同步状态权限'),
    (900180, N'排放源列表查询', 900110, 8, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:emissionSource:list', N'#', N'排放源列表查询权限'),
    (900181, N'排放源详情查询', 900110, 9, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:emissionSource:query', N'#', N'排放源详情查询权限'),
    (900182, N'排放源新增', 900110, 10, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:emissionSource:add', N'#', N'排放源新增权限'),
    (900183, N'排放源修改', 900110, 11, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:emissionSource:edit', N'#', N'排放源修改权限'),
    (900184, N'排放源删除', 900110, 12, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:emissionSource:remove', N'#', N'排放源删除权限'),

    (900185, N'活动数据列表查询', 900130, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activityData:list', N'#', N'活动数据列表查询权限'),
    (900186, N'活动数据详情查询', 900130, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activityData:query', N'#', N'活动数据详情查询权限'),
    (900187, N'活动数据新增', 900130, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activityDataRaw:add', N'#', N'活动数据新增权限'),
    (900188, N'活动数据修改', 900130, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activityDataRaw:edit', N'#', N'活动数据修改权限'),
    (900189, N'活动数据删除', 900130, 6, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activityDataRaw:remove', N'#', N'活动数据删除权限'),
    (900190, N'活动数据校验', 900130, 7, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activityImportValidation:validate', N'#', N'活动数据校验权限'),
    (900191, N'活动数据提交', 900130, 8, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activity:save', N'#', N'活动数据提交权限'),
    (900192, N'活动数据Excel导入', 900130, 9, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:activityImport:import', N'#', N'活动数据Excel导入权限'),
    (900193, N'Source(A)数据导入', 900130, 10, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:sourceA:import', N'#', N'Source(A)数据导入权限'),
    (900194, N'Source(A)数据校验', 900130, 11, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:sourceA:validate', N'#', N'Source(A)数据校验权限'),
    (900195, N'在线填报批次查询', 900130, 12, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:captureBatch:list', N'#', N'在线填报批次查询权限'),
    (900196, N'在线填报行查询', 900130, 13, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:captureRow:list', N'#', N'在线填报行查询权限'),
    (900197, N'在线填报单元格查询', 900130, 14, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:captureCell:list', N'#', N'在线填报单元格查询权限'),

    (900200, N'因子确认列表查询', 900120, 6, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorConfirmation:list', N'#', N'因子确认列表查询权限'),
    (900201, N'因子确认详情查询', 900120, 7, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorConfirmation:query', N'#', N'因子确认详情查询权限'),
    (900202, N'因子确认新增', 900120, 8, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorConfirmation:add', N'#', N'因子确认新增权限'),
    (900203, N'因子确认修改', 900120, 9, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorConfirmation:edit', N'#', N'因子确认修改权限'),
    (900204, N'因子确认删除', 900120, 10, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorConfirmation:remove', N'#', N'因子确认删除权限'),
    (900205, N'匹配因子库', 900120, 11, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorSync:run', N'#', N'匹配因子库权限'),
    (900206, N'因子缓存列表查询', 900120, 12, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorCacheRecord:list', N'#', N'因子缓存列表查询权限'),
    (900207, N'因子缓存详情查询', 900120, 13, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorCacheRecord:query', N'#', N'因子缓存详情查询权限'),
    (900208, N'因子缓存版本查询', 900120, 14, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorCacheVersion:list', N'#', N'因子缓存版本查询权限'),

    (900210, N'绿电绿证列表查询', 900140, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:greenPowerCertificate:list', N'#', N'绿电绿证列表查询权限'),
    (900211, N'绿电绿证详情查询', 900140, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:greenPowerCertificate:query', N'#', N'绿电绿证详情查询权限'),
    (900212, N'绿电绿证新增', 900140, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:greenPowerCertificate:add', N'#', N'绿电绿证新增权限'),
    (900213, N'绿电绿证修改', 900140, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:greenPowerCertificate:edit', N'#', N'绿电绿证修改权限'),
    (900214, N'绿电绿证删除', 900140, 6, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:greenPowerCertificate:remove', N'#', N'绿电绿证删除权限'),

    (900220, N'强度指标列表查询', 900150, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:intensityMetric:list', N'#', N'强度指标列表查询权限'),
    (900221, N'强度指标详情查询', 900150, 6, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:intensityMetric:query', N'#', N'强度指标详情查询权限'),
    (900222, N'强度指标新增', 900150, 7, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:intensityMetric:add', N'#', N'强度指标新增权限'),
    (900223, N'强度指标修改', 900150, 8, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:intensityMetric:edit', N'#', N'强度指标修改权限'),
    (900224, N'强度指标删除', 900150, 9, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:intensityMetric:remove', N'#', N'强度指标删除权限'),

    (900240, N'Content查看', 900160, 4, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:reports:view', N'#', N'Content查看与同步权限'),
    (900241, N'数据验证查看', 900160, 5, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:dataValidation:view', N'#', N'数据验证查看权限'),
    (900242, N'报表模板同步', 900160, 6, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:reportTemplateSync:run', N'#', N'报表模板同步权限'),
    (900243, N'报表模板列表查询', 900160, 7, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:reportTemplateFile:list', N'#', N'报表模板列表查询权限'),
    (900244, N'报表模板详情查询', 900160, 8, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:reportTemplateFile:query', N'#', N'报表模板详情查询权限'),
    (900245, N'报表模板下载', 900160, 9, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:reportTemplateFile:download', N'#', N'报表模板下载权限'),
    (900246, N'模板版本列表查询', 900160, 10, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:templateVersion:list', N'#', N'模板版本列表查询权限'),
    (900247, N'模板Sheet列表查询', 900160, 11, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:templateSheet:list', N'#', N'模板Sheet列表查询权限'),
    (900248, N'模板字段列表查询', 900160, 12, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:templateField:list', N'#', N'模板字段列表查询权限'),
    (900249, N'扩展字段列表查询', 900130, 15, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionField:list', N'#', N'扩展字段列表查询权限'),
    (900250, N'扩展字段值列表查询', 900130, 16, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionFieldValue:list', N'#', N'扩展字段值列表查询权限'),
    (900251, N'扩展字段值新增', 900130, 17, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionFieldValue:add', N'#', N'扩展字段值新增权限'),
    (900252, N'扩展字段值修改', 900130, 18, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionFieldValue:edit', N'#', N'扩展字段值修改权限'),
    (900253, N'扩展字段值删除', 900130, 19, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionFieldValue:remove', N'#', N'扩展字段值删除权限'),
    (900254, N'扩展字段新增', 900130, 25, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionField:add', N'#', N'扩展字段新增权限'),
    (900255, N'扩展字段修改', 900130, 26, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionField:edit', N'#', N'扩展字段修改权限'),
    (900256, N'扩展字段删除', 900130, 27, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionField:remove', N'#', N'扩展字段删除权限'),
    (900260, N'操作日志导出', 900231, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'monitor:operlog:export', N'#', N'操作日志导出权限'),
    (900261, N'操作日志删除', 900231, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'monitor:operlog:remove', N'#', N'操作日志删除权限'),
    (900262, N'登录日志导出', 900232, 1, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'monitor:logininfor:export', N'#', N'登录日志导出权限'),
    (900263, N'登录日志删除', 900232, 2, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'monitor:logininfor:remove', N'#', N'登录日志删除权限'),
    (900264, N'登录日志解锁', 900232, 3, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'monitor:logininfor:unlock', N'#', N'登录日志解锁权限'),
    (900265, N'在线填报批次详情', 900130, 20, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:captureBatch:query', N'#', N'在线填报批次详情权限'),
    (900266, N'在线填报行详情', 900130, 21, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:captureRow:query', N'#', N'在线填报行详情权限'),
    (900267, N'在线填报单元格详情', 900130, 22, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:captureCell:query', N'#', N'在线填报单元格详情权限'),
    (900268, N'因子缓存版本详情', 900120, 15, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:factorCacheVersion:query', N'#', N'因子缓存版本详情权限'),
    (900269, N'模板版本详情查询', 900160, 13, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:templateVersion:query', N'#', N'模板版本详情查询权限'),
    (900270, N'模板Sheet详情查询', 900160, 14, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:templateSheet:query', N'#', N'模板Sheet详情查询权限'),
    (900271, N'模板字段详情查询', 900160, 15, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:templateField:query', N'#', N'模板字段详情查询权限'),
    (900272, N'扩展字段详情查询', 900130, 23, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionField:query', N'#', N'扩展字段详情查询权限'),
    (900273, N'扩展字段值详情查询', 900130, 24, N'#', N'', N'', 1, 0, N'F', N'1', N'0', N'enterprise:extensionFieldValue:query', N'#', N'扩展字段值详情查询权限');

MERGE dbo.sys_menu AS target
USING @menus AS source
ON target.menu_id = source.menu_id
WHEN MATCHED THEN
    UPDATE SET menu_name = source.menu_name,
               parent_id = source.parent_id,
               order_num = source.order_num,
               path = source.path,
               component = source.component,
               query_param = source.query_param,
               is_frame = source.is_frame,
               is_cache = source.is_cache,
               menu_type = source.menu_type,
               visible = source.visible,
               status = source.status,
               perms = source.perms,
               icon = source.icon,
               update_by = @createBy,
               update_time = @now,
               remark = source.remark
WHEN NOT MATCHED THEN
    INSERT (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
            menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
    VALUES (source.menu_id, source.menu_name, source.parent_id, source.order_num, source.path, source.component,
            source.query_param, source.is_frame, source.is_cache, source.menu_type, source.visible, source.status,
            source.perms, source.icon, @createDept, @createBy, @now, source.remark);

DECLARE @roles TABLE (
    role_id BIGINT NOT NULL PRIMARY KEY,
    role_name NVARCHAR(100) NOT NULL,
    role_key NVARCHAR(100) NOT NULL,
    role_sort INT NOT NULL,
    data_scope NVARCHAR(1) NOT NULL,
    remark NVARCHAR(500) NOT NULL
);

INSERT INTO @roles (role_id, role_name, role_key, role_sort, data_scope, remark)
VALUES
    (1, N'超级管理员', N'superadmin', 1, N'1', N'系统超级管理员'),
    (900001, N'企业系统管理员', N'enterprise_admin', 10, N'1', N'企业端预置角色：系统、授权、填报、同步、报表、日志全权限'),
    (900002, N'企业运营管理员', N'enterprise_operator', 20, N'1', N'企业端预置角色：授权、同步、数据验证、报表和日志运营'),
    (900003, N'企业数据填报员', N'enterprise_reporter', 30, N'5', N'企业端预置角色：排放源、活动数据、绿电绿证、强度数据填报'),
    (900004, N'企业数据审核员', N'enterprise_reviewer', 40, N'5', N'企业端预置角色：数据验证、活动数据查看、因子确认'),
    (900005, N'企业报表查看员', N'enterprise_report_viewer', 50, N'5', N'企业端预置角色：Content、数据验证、报表模板下载');

MERGE dbo.sys_role AS target
USING @roles AS source
ON target.role_id = source.role_id
WHEN MATCHED THEN
    UPDATE SET tenant_id = @tenantId,
               role_name = source.role_name,
               role_key = source.role_key,
               role_sort = source.role_sort,
               data_scope = source.data_scope,
               menu_check_strictly = 1,
               dept_check_strictly = 1,
               status = N'0',
               del_flag = N'0',
               update_by = @createBy,
               update_time = @now,
               remark = source.remark
WHEN NOT MATCHED THEN
    INSERT (role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
            dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by,
            update_time, remark)
    VALUES (source.role_id, @tenantId, source.role_name, source.role_key, source.role_sort, source.data_scope,
            1, 1, N'0', N'0', @createDept, @createBy, @now, NULL, NULL, source.remark);

IF OBJECT_ID(N'dbo.sys_user_role', N'U') IS NOT NULL
BEGIN
    DECLARE @adminUserId BIGINT = (
        SELECT TOP (1) user_id
          FROM dbo.sys_user
         WHERE tenant_id = @tenantId
           AND user_name = N'admin'
           AND del_flag = N'0'
    );

    IF @adminUserId IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM dbo.sys_user_role WHERE user_id = @adminUserId AND role_id = 1)
            INSERT INTO dbo.sys_user_role (user_id, role_id) VALUES (@adminUserId, 1);
        IF NOT EXISTS (SELECT 1 FROM dbo.sys_user_role WHERE user_id = @adminUserId AND role_id = 900001)
            INSERT INTO dbo.sys_user_role (user_id, role_id) VALUES (@adminUserId, 900001);
    END;
END;

IF OBJECT_ID(N'dbo.sys_user_post', N'U') IS NOT NULL
BEGIN
    DECLARE @postUserId BIGINT = (
        SELECT TOP (1) user_id
          FROM dbo.sys_user
         WHERE tenant_id = @tenantId
           AND user_name = N'admin'
           AND del_flag = N'0'
    );

    IF @postUserId IS NOT NULL
       AND EXISTS (SELECT 1 FROM dbo.sys_post WHERE post_id = 1)
       AND NOT EXISTS (SELECT 1 FROM dbo.sys_user_post WHERE user_id = @postUserId AND post_id = 1)
        INSERT INTO dbo.sys_user_post (user_id, post_id) VALUES (@postUserId, 1);
END;

DECLARE @roleMenu TABLE (role_id BIGINT NOT NULL, menu_id BIGINT NOT NULL);

INSERT INTO @roleMenu (role_id, menu_id)
SELECT role_id, menu_id
  FROM @roles
 CROSS JOIN @menus
 WHERE role_id IN (1, 900001);

INSERT INTO @roleMenu (role_id, menu_id)
SELECT 900002, menu_id
  FROM @menus
 WHERE menu_id IN (
    900100, 900102, 900103, 900104, 900105, 900106,
    900107, 900249, 900250, 900254, 900255, 900256, 900272,
    900110, 900175, 900176,
    900120, 900121, 900122, 900123, 900124, 900125, 900200, 900201, 900205, 900206, 900207, 900208,
    900160, 900161, 900164, 900162, 900163, 900165, 900240, 900241, 900242, 900243, 900244, 900245, 900246, 900247, 900248,
    900268, 900269, 900270, 900271,
    900230, 900231, 900232
 );

INSERT INTO @roleMenu (role_id, menu_id)
VALUES
    (900001, 103), (900001, 1051), (900001, 1052), (900001, 1053), (900001, 1054),
    (900002, 103), (900002, 1051), (900002, 1052), (900002, 1053), (900002, 1054);

INSERT INTO @roleMenu (role_id, menu_id)
SELECT 900003, menu_id
  FROM @menus
 WHERE menu_id IN (
    900106,
    900110, 900111, 900112, 900113, 900114, 900115, 900170, 900171, 900172, 900173, 900174, 900180, 900181, 900182, 900183, 900184,
    900130, 900131, 900185, 900186, 900187, 900188, 900189, 900190, 900191, 900192, 900193, 900194, 900195, 900196, 900197,
    900265, 900266, 900267,
    900140, 900141, 900210, 900211, 900212, 900213, 900214,
    900150, 900151, 900152, 900153, 900154, 900220, 900221, 900222, 900223, 900224,
    900249, 900250, 900251, 900252, 900253, 900272, 900273
 );

INSERT INTO @roleMenu (role_id, menu_id)
SELECT 900004, menu_id
  FROM @menus
 WHERE menu_id IN (
    900106,
    900110, 900111, 900112, 900113, 900114, 900115, 900170, 900171, 900180, 900181,
    900120, 900121, 900122, 900123, 900124, 900125, 900200, 900201, 900202, 900203,
    900130, 900131, 900185, 900186, 900190, 900195, 900196, 900197, 900265, 900266, 900267,
    900140, 900141, 900210, 900211,
    900150, 900151, 900152, 900153, 900154, 900220, 900221,
    900160, 900162, 900241
 );

INSERT INTO @roleMenu (role_id, menu_id)
SELECT 900005, menu_id
  FROM @menus
 WHERE menu_id IN (
    900106,
    900160, 900161, 900164, 900162, 900163, 900165, 900240, 900241, 900243, 900244, 900245,
    900246, 900247, 900248, 900269, 900270, 900271
 );

DELETE target
  FROM dbo.sys_role_menu target
  JOIN @roles role_source ON role_source.role_id = target.role_id
  JOIN @menus menu_source ON menu_source.menu_id = target.menu_id;

INSERT INTO dbo.sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, menu_id
  FROM @roleMenu source
 WHERE EXISTS (SELECT 1 FROM dbo.sys_role role_target WHERE role_target.role_id = source.role_id)
   AND EXISTS (SELECT 1 FROM dbo.sys_menu menu_target WHERE menu_target.menu_id = source.menu_id)
   AND NOT EXISTS (
       SELECT 1
         FROM dbo.sys_role_menu existing
        WHERE existing.role_id = source.role_id
          AND existing.menu_id = source.menu_id
   );

SELECT N'carbon_enterprise_init_ok' AS result;
GO
