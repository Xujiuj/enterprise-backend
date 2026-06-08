-- Enterprise portal business menus are returned by /system/menu/getRouters.
-- Frontend router files must not hand-code these business menu entries.

delete from sys_menu where menu_id between 900100 and 900108;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900100, '企业本地业务', 0, 1, 'enterprise', 'Layout', '', 1, 0, 'M', '0', '0', '', 'company', 103, 1, sysdate(), '企业端 portal 根菜单');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900101, 'License 授权', 900100, 1, 'license-import', 'enterprise/licenseImport/index', '', 1, 0, 'C', '0', '0', 'enterprise:license:import', 'lock', 103, 1, sysdate(), '企业本地导入和验签授权');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900102, '01 配置排放源', 900100, 2, 'emission-source', 'system/emissionSource/index', '', 1, 0, 'C', '0', '0', 'enterprise:emissionSource:list', 'tree', 103, 1, sysdate(), '企业本地配置排放源');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900103, '02 确认排放因子', 900100, 3, 'factor-confirm', 'system/factorConfirm/index', '', 1, 0, 'C', '0', '0', 'enterprise:factorConfirm:list', 'validCode', 103, 1, sysdate(), '企业本地确认排放因子');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900104, '03 活动数据', 900100, 4, 'activity-data', 'system/activityData/index', '', 1, 0, 'C', '0', '0', 'enterprise:activityData:list', 'form', 103, 1, sysdate(), '企业本地活动数据录入');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900105, '04 绿电绿证', 900100, 5, 'green-electricity', 'system/greenElectricity/index', '', 1, 0, 'C', '0', '0', 'enterprise:greenElectricity:list', 'international', 103, 1, sysdate(), '企业本地绿电绿证管理');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900106, '05 强度管理', 900100, 6, 'intensity', 'system/intensity/index', '', 1, 0, 'C', '0', '0', 'enterprise:intensity:list', 'chart', 103, 1, sysdate(), '企业本地强度管理');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900107, '因子查询', 900100, 7, 'factor-library', 'system/factorLibrary/index', '', 1, 0, 'C', '0', '0', 'enterprise:factor:query', 'search', 103, 1, sysdate(), '企业端只读因子查询');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
values
(900108, '报表模板下载', 900100, 8, 'report-template-download', 'system/reportTemplate/index', '', 1, 0, 'C', '0', '0', 'enterprise:reportTemplate:download', 'download', 103, 1, sysdate(), '企业端只读下载报表模板');
