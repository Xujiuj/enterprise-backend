-- Repair browser-acceptance menu drift after local enterprise DB resets.
-- Scope: enterprise database only.

update sys_menu
set parent_id = 900160,
    path = 'report-template-download',
    component = 'enterprise/reportTemplateFile/index',
    perms = 'enterprise:reportTemplateFile:list'
where menu_id = 900163;

update sys_menu
set parent_id = 900120,
    path = 'factor-cache-record',
    component = 'enterprise/factorCacheRecord/index',
    perms = 'enterprise:factorCacheRecord:list'
where menu_id = 900127;

select menu_id, menu_name, parent_id, path, component, perms
from sys_menu
where menu_id in (900127, 900163)
order by menu_id;
