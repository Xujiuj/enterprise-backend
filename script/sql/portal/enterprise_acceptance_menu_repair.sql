-- Repair browser-acceptance menu drift after local enterprise DB resets.
-- Scope: enterprise database only.

update sys_menu
set parent_id = 900160,
    path = 'report-template-download',
    component = 'enterprise/reportTemplateFile/index',
    perms = 'enterprise:reportTemplateFile:list'
where menu_id = 900163;

delete from sys_role_menu
where menu_id in (900126, 900127, 900132);

delete from sys_menu
where menu_id in (900126, 900127, 900132);

update sys_menu
set menu_name = case menu_id
    when 900110 then '01 配置排放源'
    when 900120 then '02 确认排放因子'
    when 900130 then '03 活动数据'
    when 900140 then '04 绿电绿证'
    when 900150 then '05 强度管理'
    else menu_name
  end,
    update_time = sysdate()
where menu_id in (900110, 900120, 900130, 900140, 900150);

update sys_menu
set menu_name = '日志',
    parent_id = 0,
    component = 'Layout',
    update_time = sysdate()
where menu_id = 108;

update sys_menu
set order_num = case menu_id
    when 900100 then 1
    when 900110 then 2
    when 900120 then 3
    when 900130 then 4
    when 900140 then 5
    when 900150 then 6
    when 900160 then 7
    when 1 then 8
    when 108 then 9
    else order_num
  end,
    update_time = sysdate()
where menu_id in (900100, 900110, 900120, 900130, 900140, 900150, 900160, 1, 108);

update sys_menu
set order_num = case menu_id
    when 900111 then 1
    when 900112 then 2
    when 900113 then 3
    when 900114 then 4
    when 900115 then 5
    when 900121 then 1
    when 900122 then 2
    when 900123 then 3
    when 900124 then 4
    when 900125 then 5
    when 900131 then 1
    when 900141 then 1
    when 900151 then 1
    when 900152 then 2
    when 900153 then 3
    when 900154 then 4
    when 900161 then 1
    when 900162 then 2
    when 900163 then 3
    when 100 then 1
    when 101 then 2
    when 102 then 3
    when 500 then 1
    when 501 then 2
    else order_num
  end,
    update_time = sysdate()
where menu_id in (
  900111, 900112, 900113, 900114, 900115,
  900121, 900122, 900123, 900124, 900125,
  900131, 900141,
  900151, 900152, 900153, 900154,
  900161, 900162, 900163,
  100, 101, 102, 500, 501
);

-- Production portal menu policy:
-- - Hide development/admin configuration utilities from final navigation.
-- - Keep only the system management entries listed in 意见反馈20260602.md visible.
-- RuoYi uses visible='0' for shown routes and visible='1' for hidden routes.
update sys_menu
set visible = '1',
    status = '1',
    update_time = sysdate()
where menu_id in (
  103, 104, 105, 106, 115, 116, 132,
  107, 109, 113, 117, 118, 120, 121, 122, 123,
  2, 3, 4, 5, 6, 900106,
  1500, 1506, 11616, 11618, 11619, 11620, 11621, 11622,
  11629, 11630, 11631, 11632, 11633, 11638, 11801,
  1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1025,
  1026, 1027, 1028, 1029, 1030,
  1031, 1032, 1033, 1034, 1035,
  1055, 1056, 1057, 1058, 1059, 1060
);

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
    1, 100, 101, 102,
    108, 500, 501
  );

update sys_menu
set visible = '0',
    status = '0',
    update_time = sysdate()
where menu_id in (102, 108, 500, 501, 1013, 1014, 1015, 1016);

delete from sys_role_menu
where menu_id in (
  103, 104, 105, 106, 115, 116, 132,
  107, 109, 113, 117, 118, 120, 121, 122, 123,
  2, 3, 4, 5, 6, 900106,
  1500, 1506, 11616, 11618, 11619, 11620, 11621, 11622,
  11629, 11630, 11631, 11632, 11633, 11638, 11801,
  1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1025,
  1026, 1027, 1028, 1029, 1030,
  1031, 1032, 1033, 1034, 1035,
  1055, 1056, 1057, 1058, 1059, 1060
)
and role_id <> 1;

insert ignore into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
cross join sys_menu m
where r.status = '0'
  and r.role_id <> 1
  and m.menu_id in (102, 1013, 1014, 1015, 1016);

select menu_id, menu_name, parent_id, path, component, perms
from sys_menu
where menu_id in (102, 103, 104, 105, 106, 115, 116, 132, 900163)
order by menu_id;
