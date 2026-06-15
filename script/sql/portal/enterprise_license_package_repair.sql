-- Repair enterprise-local license state package metadata.
-- This script only writes the enterprise database.

set @schema_name = database();

set @sql = if(
    exists(select 1 from information_schema.columns where table_schema = @schema_name and table_name = 'ce_license_state' and column_name = 'package_id'),
    'select 1',
    'alter table ce_license_state add column package_id bigint default null after customer_id'
);
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

set @sql = if(
    exists(select 1 from information_schema.columns where table_schema = @schema_name and table_name = 'ce_license_state' and column_name = 'package_name'),
    'select 1',
    'alter table ce_license_state add column package_name varchar(64) default null after package_id'
);
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

update ce_license_state
set package_id = case
        when current_summary like '%packageId=1001%' then 1001
        when current_summary like '%packageId=1002%' then 1002
        when current_summary like '%packageId=1003%' then 1003
        when current_summary like '%edition=standard%' then 1001
        when current_summary like '%edition=professional%' then 1002
        when current_summary like '%edition=pro%' then 1002
        when current_summary like '%edition=enterprise%' then 1003
        when current_summary like '%edition=group%' then 1003
        else package_id
    end,
    package_name = case
        when current_summary like '%packageName=标准版%' or current_summary like '%packageId=1001%' or current_summary like '%edition=standard%' then '标准版'
        when current_summary like '%packageName=专业版%' or current_summary like '%packageId=1002%' or current_summary like '%edition=professional%' or current_summary like '%edition=pro%' then '专业版'
        when current_summary like '%packageName=集团版%' or current_summary like '%packageId=1003%' or current_summary like '%edition=enterprise%' or current_summary like '%edition=group%' then '集团版'
        else package_name
    end
where package_id is null
   or package_name is null;
