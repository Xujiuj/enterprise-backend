package org.dromara.carbon.enterprise.dimension.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.shared.service.ICeCompanyFactoryDeptSyncService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Maintains the legacy company/factory projection from the organization tree.
 */
@RequiredArgsConstructor
@Service
public class CeCompanyFactoryDeptSyncServiceImpl implements ICeCompanyFactoryDeptSyncService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void syncSysDeptToCompanyFactories() {
        jdbcTemplate.update("""
            IF OBJECT_ID(N'dbo.sys_dept', N'U') IS NOT NULL
               AND OBJECT_ID(N'dbo.ce_company_factory', N'U') IS NOT NULL
            BEGIN
                ;WITH company_nodes AS (
                    SELECT c.dept_id, LTRIM(RTRIM(c.dept_category)) AS company_code,
                           LTRIM(RTRIM(c.dept_name)) AS company_name,
                           c.status
                      FROM dbo.sys_dept c
                     WHERE c.del_flag = N'0'
                       AND NULLIF(LTRIM(RTRIM(c.dept_category)), N'') IS NOT NULL
                       AND NOT EXISTS (
                           SELECT 1 FROM dbo.sys_dept parent
                            WHERE parent.dept_id = c.parent_id
                              AND parent.del_flag = N'0'
                              AND NULLIF(LTRIM(RTRIM(parent.dept_category)), N'') IS NOT NULL
                       )
                ), factory_nodes AS (
                    SELECT company.company_code, company.company_name, company.status AS company_status,
                           LTRIM(RTRIM(factory.factory_code)) AS factory_code,
                           LTRIM(RTRIM(factory.dept_name)) AS factory_name,
                           factory.status AS factory_status
                      FROM dbo.sys_dept factory
                      JOIN company_nodes company ON company.dept_id = factory.parent_id
                     WHERE factory.del_flag = N'0'
                       AND factory.dept_category = company.company_code
                       AND NULLIF(LTRIM(RTRIM(factory.factory_code)), N'') IS NOT NULL
                )
                MERGE dbo.ce_company_factory AS target
                USING factory_nodes AS source
                   ON target.company_code = source.company_code
                  AND target.factory_code = source.factory_code
                WHEN MATCHED THEN UPDATE SET
                    company_name = source.company_name,
                    factory_name = source.factory_name,
                    company_sk = CONCAT(N'SK_', source.company_code, N'_', source.factory_code),
                    is_active = CASE WHEN source.company_status = N'0' AND source.factory_status = N'0' THEN N'Y' ELSE N'N' END,
                    update_time = SYSDATETIME()
                WHEN NOT MATCHED THEN INSERT (
                    company_sk, company_code, factory_code, company_name, factory_name, is_active, create_time, update_time
                ) VALUES (
                    CONCAT(N'SK_', source.company_code, N'_', source.factory_code), source.company_code, source.factory_code,
                    source.company_name, source.factory_name,
                    CASE WHEN source.company_status = N'0' AND source.factory_status = N'0' THEN N'Y' ELSE N'N' END,
                    SYSDATETIME(), SYSDATETIME()
                );

                ;WITH company_nodes AS (
                    SELECT c.dept_id, LTRIM(RTRIM(c.dept_category)) AS company_code
                      FROM dbo.sys_dept c
                     WHERE c.del_flag = N'0'
                       AND NULLIF(LTRIM(RTRIM(c.dept_category)), N'') IS NOT NULL
                       AND NOT EXISTS (
                           SELECT 1 FROM dbo.sys_dept parent
                            WHERE parent.dept_id = c.parent_id
                              AND parent.del_flag = N'0'
                              AND NULLIF(LTRIM(RTRIM(parent.dept_category)), N'') IS NOT NULL
                       )
                ), active_factories AS (
                    SELECT company.company_code, LTRIM(RTRIM(factory.factory_code)) AS factory_code
                      FROM dbo.sys_dept factory
                      JOIN company_nodes company ON company.dept_id = factory.parent_id
                     WHERE factory.del_flag = N'0'
                       AND factory.dept_category = company.company_code
                       AND NULLIF(LTRIM(RTRIM(factory.factory_code)), N'') IS NOT NULL
                )
                UPDATE target
                   SET is_active = N'N', update_time = SYSDATETIME()
                  FROM dbo.ce_company_factory target
                 WHERE ISNULL(target.is_active, N'Y') <> N'N'
                   AND NOT EXISTS (
                       SELECT 1 FROM active_factories source
                        WHERE source.company_code = target.company_code
                          AND source.factory_code = target.factory_code
                   );
            END
            """);
    }
}
