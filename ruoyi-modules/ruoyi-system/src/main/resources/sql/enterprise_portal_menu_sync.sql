-- Enterprise portal navigation contract.
-- Source of truth: 意见反馈20260602.md.
-- This script is intentionally idempotent and runs during enterprise-backend startup
-- so GitHub CI/CD deployments reconcile the production sys_menu/sys_role_menu tables.

delete from sys_role_menu where menu_id between 900100 and 900230;
delete from sys_menu where menu_id between 900100 and 900230;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900100, '系统授权', 0, 1, 'system-auth', 'Layout', '', 1, 0, 'M', '0', '0', '', 'lock', 103, 1, sysdate(), '系统授权目录'),
(900102, '授权管理', 900100, 1, 'license-import', 'enterprise/licenseImport/index', '', 1, 0, 'C', '0', '0', 'enterprise:licenseImport:import', 'lock', 103, 1, sysdate(), '授权管理'),
(900103, '授权导入接口', 900100, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:licenseImport:import', '#', 103, 1, sysdate(), '授权导入接口权限'),
(900104, '授权状态查询', 900100, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:licenseState:query', '#', 103, 1, sysdate(), '授权状态查询权限'),
(900105, '工作台总览', 900100, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:workbench:overview', '#', 103, 1, sysdate(), '工作台总览接口权限');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900110, '01 配置排放源', 0, 2, 'emission-source-config', 'Layout', '', 1, 0, 'M', '0', '0', '', 'tree', 103, 1, sysdate(), '配置排放源目录'),
(900111, '行政区划', 900110, 1, 'admin-division', 'enterprise/dimension/index', '{"code":"admin-division"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'tree', 103, 1, sysdate(), '行政区划'),
(900112, '公司表', 900110, 2, 'company', 'enterprise/dimension/index', '{"code":"company"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'company', 103, 1, sysdate(), '公司表'),
(900113, '排放源分类', 900110, 3, 'emission-source-category', 'enterprise/dimension/index', '{"code":"emission-source-category"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'tree', 103, 1, sysdate(), '排放源分类'),
(900114, '排放源识别', 900110, 4, 'emission-source', 'enterprise/emissionSource/index', '', 1, 0, 'C', '0', '0', 'enterprise:emissionSource:list', 'form', 103, 1, sysdate(), '排放源识别'),
(900115, '基准年维度表', 900110, 5, 'base-year', 'enterprise/dimension/index', '{"code":"base-year"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'date-range', 103, 1, sysdate(), '基准年维度表');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900120, '02 确认排放因子', 0, 3, 'factor-confirm', 'Layout', '', 1, 0, 'M', '0', '0', '', 'validCode', 103, 1, sysdate(), '确认排放因子目录'),
(900121, 'EF排放因子维度表', 900120, 1, 'ef-factor', 'enterprise/dimension/index', '{"code":"ef-factor"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'search', 103, 1, sysdate(), 'EF排放因子维度表'),
(900122, 'EF电力因子维度表', 900120, 2, 'ef-electricity-factor', 'enterprise/dimension/index', '{"code":"ef-electricity-factor"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'search', 103, 1, sysdate(), 'EF电力因子维度表'),
(900123, 'EF电力因子版本对应', 900120, 3, 'ef-electricity-version', 'enterprise/dimension/index', '{"code":"ef-electricity-version"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'validCode', 103, 1, sysdate(), 'EF电力因子版本对应'),
(900124, 'EF电力因子口径维度', 900120, 4, 'ef-electricity-scope', 'enterprise/dimension/index', '{"code":"ef-electricity-scope"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'validCode', 103, 1, sysdate(), 'EF电力因子口径维度'),
(900125, '温室气体维度', 900120, 5, 'greenhouse-gas', 'enterprise/dimension/index', '{"code":"greenhouse-gas"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'search', 103, 1, sysdate(), '温室气体维度');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900130, '03 活动数据', 0, 4, 'activity-data', 'Layout', '', 1, 0, 'M', '0', '0', '', 'form', 103, 1, sysdate(), '活动数据目录'),
(900131, '排放活动数据', 900130, 1, 'emission-activity-data', 'enterprise/activityData/index', '', 1, 0, 'C', '0', '0', 'enterprise:activityData:list', 'form', 103, 1, sysdate(), '排放活动数据'),
(900140, '04 绿电绿证', 0, 5, 'green-electricity', 'Layout', '', 1, 0, 'M', '0', '0', '', 'international', 103, 1, sysdate(), '绿电绿证目录'),
(900141, '绿电绿证数据', 900140, 1, 'green-electricity-data', 'enterprise/greenPowerCertificate/index', '', 1, 0, 'C', '0', '0', 'enterprise:greenPowerCertificate:list', 'international', 103, 1, sysdate(), '绿电绿证数据');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900150, '05 强度管理', 0, 6, 'intensity', 'Layout', '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, sysdate(), '强度管理目录'),
(900151, '碳排放强度分母维度表', 900150, 1, 'intensity-denominator', 'enterprise/dimension/index', '{"code":"intensity-denominator"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'form', 103, 1, sysdate(), '碳排放强度分母维度表'),
(900152, '强度目标表', 900150, 2, 'intensity-target', 'enterprise/intensityMetric/index', '', 1, 0, 'C', '0', '0', 'enterprise:intensityMetric:list', 'chart', 103, 1, sysdate(), '强度目标表'),
(900153, '分母事实表', 900150, 3, 'denominator-fact', 'enterprise/dimension/index', '{"code":"denominator-fact"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'form', 103, 1, sysdate(), '分母事实表'),
(900154, '碳排放强度容忍率参数表', 900150, 4, 'intensity-tolerance', 'enterprise/dimension/index', '{"code":"intensity-tolerance"}', 1, 0, 'C', '0', '0', 'enterprise:dimension:view', 'chart', 103, 1, sysdate(), '碳排放强度容忍率参数表');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900160, '报表管理', 0, 7, 'report-management', 'Layout', '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, sysdate(), '报表管理目录'),
(900161, 'Content', 900160, 1, 'content', 'enterprise/reports/index', '', 1, 0, 'C', '0', '0', 'enterprise:reports:view', 'chart', 103, 1, sysdate(), 'Content'),
(900162, '数据验证', 900160, 2, 'data-validation', 'enterprise/dataValidation/index', '', 1, 0, 'C', '0', '0', 'enterprise:dataValidation:view', 'validCode', 103, 1, sysdate(), '数据验证'),
(900163, '报表模板下载', 900160, 3, 'report-template-download', 'enterprise/reportTemplateFile/index', '', 1, 0, 'C', '0', '0', 'enterprise:reportTemplateFile:list', 'download', 103, 1, sysdate(), '报表模板下载');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900170, '维度列表查询', 900110, 1, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:list', '#', 103, 1, sysdate(), '维度列表查询权限'),
(900171, '维度详情查询', 900110, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:query', '#', 103, 1, sysdate(), '维度详情查询权限'),
(900172, '维度新增', 900110, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:add', '#', 103, 1, sysdate(), '维度新增权限'),
(900173, '维度修改', 900110, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:edit', '#', 103, 1, sysdate(), '维度修改权限'),
(900174, '维度删除', 900110, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:dimension:remove', '#', 103, 1, sysdate(), '维度删除权限'),
(900180, '排放源列表查询', 900130, 1, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:list', '#', 103, 1, sysdate(), '排放源列表查询权限'),
(900181, '排放源详情查询', 900130, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:query', '#', 103, 1, sysdate(), '排放源详情查询权限'),
(900182, '排放源新增', 900130, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:add', '#', 103, 1, sysdate(), '排放源新增权限'),
(900183, '排放源修改', 900130, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:edit', '#', 103, 1, sysdate(), '排放源修改权限'),
(900184, '排放源删除', 900130, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:emissionSource:remove', '#', 103, 1, sysdate(), '排放源删除权限'),
(900185, '活动数据列表查询', 900130, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityData:list', '#', 103, 1, sysdate(), '活动数据列表查询权限'),
(900186, '活动数据详情查询', 900130, 7, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityData:query', '#', 103, 1, sysdate(), '活动数据详情查询权限'),
(900187, '活动数据校验', 900130, 8, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityImportValidation:validate', '#', 103, 1, sysdate(), '活动数据校验权限'),
(900188, '活动数据保存', 900130, 9, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activity:save', '#', 103, 1, sysdate(), '活动数据保存权限'),
(900189, '活动数据导入', 900130, 10, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:activityImport:import', '#', 103, 1, sysdate(), '活动数据导入权限');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900190, '因子确认列表查询', 900120, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:list', '#', 103, 1, sysdate(), '因子确认列表查询权限'),
(900191, '因子确认详情查询', 900120, 7, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:query', '#', 103, 1, sysdate(), '因子确认详情查询权限'),
(900192, '因子确认新增', 900120, 8, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:add', '#', 103, 1, sysdate(), '因子确认新增权限'),
(900193, '因子确认修改', 900120, 9, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:edit', '#', 103, 1, sysdate(), '因子确认修改权限'),
(900194, '因子确认删除', 900120, 10, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorConfirmation:remove', '#', 103, 1, sysdate(), '因子确认删除权限'),
(900195, '绿电绿证列表查询', 900140, 2, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:list', '#', 103, 1, sysdate(), '绿电绿证列表查询权限'),
(900196, '绿电绿证详情查询', 900140, 3, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:query', '#', 103, 1, sysdate(), '绿电绿证详情查询权限'),
(900197, '绿电绿证新增', 900140, 4, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:add', '#', 103, 1, sysdate(), '绿电绿证新增权限'),
(900198, '绿电绿证修改', 900140, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:edit', '#', 103, 1, sysdate(), '绿电绿证修改权限'),
(900199, '绿电绿证删除', 900140, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:greenPowerCertificate:remove', '#', 103, 1, sysdate(), '绿电绿证删除权限'),
(900200, '强度指标列表查询', 900150, 5, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:list', '#', 103, 1, sysdate(), '强度指标列表查询权限'),
(900201, '强度指标详情查询', 900150, 6, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:query', '#', 103, 1, sysdate(), '强度指标详情查询权限'),
(900202, '强度指标新增', 900150, 7, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:add', '#', 103, 1, sysdate(), '强度指标新增权限'),
(900203, '强度指标修改', 900150, 8, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:edit', '#', 103, 1, sysdate(), '强度指标修改权限'),
(900204, '强度指标删除', 900150, 9, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:intensityMetric:remove', '#', 103, 1, sysdate(), '强度指标删除权限'),
(900205, '报表模板列表查询', 900160, 10, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:list', '#', 103, 1, sysdate(), '报表模板列表查询权限'),
(900206, '报表模板详情查询', 900160, 11, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:query', '#', 103, 1, sysdate(), '报表模板详情查询权限'),
(900207, '报表模板新增', 900160, 12, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:add', '#', 103, 1, sysdate(), '报表模板新增权限'),
(900208, '报表模板修改', 900160, 13, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:edit', '#', 103, 1, sysdate(), '报表模板修改权限'),
(900209, '报表模板删除', 900160, 14, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:remove', '#', 103, 1, sysdate(), '报表模板删除权限'),
(900210, '报表模板下载', 900160, 15, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateFile:download', '#', 103, 1, sysdate(), '报表模板下载权限'),
(900211, '厂商因子同步', 900120, 16, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorSync:run', '#', 103, 1, sysdate(), '厂商因子同步权限'),
(900212, '厂商模板同步', 900160, 17, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:reportTemplateSync:run', '#', 103, 1, sysdate(), '厂商模板同步权限'),
(900213, '因子缓存列表查询', 900120, 18, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorCacheRecord:list', '#', 103, 1, sysdate(), '因子缓存列表查询权限'),
(900214, '因子缓存详情查询', 900120, 19, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:factorCacheRecord:query', '#', 103, 1, sysdate(), '因子缓存详情查询权限'),
(900215, '扩展字段元数据查询', 900130, 11, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionField:list', '#', 103, 1, sysdate(), '扩展字段元数据查询权限'),
(900216, '扩展字段值列表查询', 900130, 12, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionFieldValue:list', '#', 103, 1, sysdate(), '扩展字段值列表权限'),
(900217, '扩展字段值新增', 900130, 13, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionFieldValue:add', '#', 103, 1, sysdate(), '扩展字段值新增权限'),
(900218, '扩展字段值修改', 900130, 14, '#', '', '', 1, 0, 'F', '1', '0', 'enterprise:extensionFieldValue:edit', '#', 103, 1, sysdate(), '扩展字段值修改权限');

-- Hide old or framework menus from final navigation. Buttons stay normal when explicitly granted.
update sys_menu
set visible = '1',
    status = '1',
    update_time = sysdate()
where menu_type in ('M', 'C')
  and menu_id not in (
    900100, 900102,
    900110, 900111, 900112, 900113, 900114, 900115,
    900120, 900121, 900122, 900123, 900124, 900125,
    900130, 900131,
    900140, 900141,
    900150, 900151, 900152, 900153, 900154,
    900160, 900161, 900162, 900163,
    1, 100, 101, 102, 130, 131,
    108, 500, 501
  );

update sys_menu
set menu_name = '系统管理',
    parent_id = 0,
    order_num = 8,
    path = 'system',
    component = 'Layout',
    menu_type = 'M',
    visible = '0',
    status = '0',
    update_time = sysdate()
where menu_id = 1;

update sys_menu
set menu_name = case menu_id when 100 then '用户管理' when 101 then '角色管理' when 102 then '菜单管理' else menu_name end,
    parent_id = 1,
    path = case menu_id when 100 then 'user' when 101 then 'role' when 102 then 'menu' else path end,
    component = case menu_id when 100 then 'system/user/index' when 101 then 'system/role/index' when 102 then 'system/menu/index' else component end,
    visible = '0',
    status = '0',
    order_num = case menu_id when 100 then 1 when 101 then 2 when 102 then 3 else order_num end,
    update_time = sysdate()
where menu_id in (100, 101, 102);

update sys_menu
set visible = '1',
    status = '0',
    update_time = sysdate()
where menu_id in (130, 131);

update sys_menu
set menu_name = '日志',
    parent_id = 0,
    order_num = 9,
    path = 'log',
    component = 'Layout',
    menu_type = 'M',
    visible = '0',
    status = '0',
    update_time = sysdate()
where menu_id = 108;

update sys_menu
set menu_name = case menu_id when 500 then '操作日志' when 501 then '登录日志' else menu_name end,
    parent_id = 108,
    path = case menu_id when 500 then 'operlog' when 501 then 'logininfor' else path end,
    component = case menu_id when 500 then 'monitor/operlog/index' when 501 then 'monitor/logininfor/index' else component end,
    visible = '0',
    status = '0',
    order_num = case menu_id when 500 then 1 when 501 then 2 else order_num end,
    update_time = sysdate()
where menu_id in (500, 501);

-- Remove old menu and button links from ordinary roles so non-admin users do not inherit stale navigation or stale API permissions.
delete srm
from sys_role_menu srm
where srm.role_id <> 1
  and srm.menu_id not in (
    900100, 900102, 900103, 900104, 900105,
    900110, 900111, 900112, 900113, 900114, 900115,
    900120, 900121, 900122, 900123, 900124, 900125,
    900130, 900131,
    900140, 900141,
    900150, 900151, 900152, 900153, 900154,
    900160, 900161, 900162, 900163,
    900170, 900171, 900172, 900173, 900174,
    900180, 900181, 900182, 900183, 900184, 900185, 900186, 900187, 900188, 900189,
    900190, 900191, 900192, 900193, 900194, 900195, 900196, 900197, 900198, 900199,
    900200, 900201, 900202, 900203, 900204, 900205, 900206, 900207, 900208, 900209, 900210,
    900211, 900212, 900213, 900214, 900215, 900216, 900217, 900218,
    1, 100, 101, 102, 130, 131,
    108, 500, 501,
    1001, 1002, 1003, 1004, 1005, 1006,
    1007, 1008, 1009, 1010, 1011, 1012,
    1013, 1014, 1015, 1016,
    1040, 1041, 1042, 1043, 1044, 1045, 1050
  );

insert ignore into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
cross join sys_menu m
where r.status = '0'
  and r.del_flag = '0'
  and m.menu_id in (
    900100, 900102, 900103, 900104, 900105,
    900110, 900111, 900112, 900113, 900114, 900115,
    900120, 900121, 900122, 900123, 900124, 900125,
    900130, 900131,
    900140, 900141,
    900150, 900151, 900152, 900153, 900154,
    900160, 900161, 900162, 900163,
    900170, 900171, 900172, 900173, 900174,
    900180, 900181, 900182, 900183, 900184, 900185, 900186, 900187, 900188, 900189,
    900190, 900191, 900192, 900193, 900194, 900195, 900196, 900197, 900198, 900199,
    900200, 900201, 900202, 900203, 900204, 900205, 900206, 900207, 900208, 900209, 900210,
    900211, 900212, 900213, 900214, 900215, 900216, 900217, 900218,
    1, 100, 101, 102, 130, 131,
    108, 500, 501,
    1001, 1002, 1003, 1004, 1005, 1006,
    1007, 1008, 1009, 1010, 1011, 1012,
    1013, 1014, 1015, 1016,
    1040, 1041, 1042, 1043, 1044, 1045, 1050
  );

insert ignore into sys_role_menu (role_id, menu_id)
select 1, menu_id
from sys_menu;
