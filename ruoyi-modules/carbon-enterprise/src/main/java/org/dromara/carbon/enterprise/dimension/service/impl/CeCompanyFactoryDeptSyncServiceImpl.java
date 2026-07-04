package org.dromara.carbon.enterprise.dimension.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.shared.service.ICeCompanyFactoryDeptSyncService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Keeps company/factory business rows aligned with the department tree.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CeCompanyFactoryDeptSyncServiceImpl implements ICeCompanyFactoryDeptSyncService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void syncCompanyFactoriesToSysDept() {
        jdbcTemplate.update("""
            IF OBJECT_ID(N'dbo.sys_dept', N'U') IS NOT NULL
               AND OBJECT_ID(N'dbo.ce_company_factory', N'U') IS NOT NULL
            BEGIN
                ;WITH parent_dept AS (
                    SELECT
                        CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id,
                        CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN N'0,100' ELSE N'0' END AS ancestors
                ),
                company_rows AS (
                    SELECT
                        NULLIF(LTRIM(RTRIM(company_code)), N'') AS company_code,
                        COALESCE(
                            MAX(NULLIF(LTRIM(RTRIM(company_name)), N'')),
                            NULLIF(LTRIM(RTRIM(company_code)), N'')
                        ) AS dept_name,
                        MIN(CASE WHEN ISNULL(is_active, N'Y') = N'Y' THEN N'0' ELSE N'1' END) AS dept_status
                    FROM dbo.ce_company_factory
                    WHERE NULLIF(LTRIM(RTRIM(company_code)), N'') IS NOT NULL
                    GROUP BY NULLIF(LTRIM(RTRIM(company_code)), N'')
                )
                UPDATE d
                   SET d.dept_name = company_rows.dept_name,
                       d.status = company_rows.dept_status,
                       d.update_time = SYSDATETIME()
                  FROM dbo.sys_dept d
                  JOIN parent_dept ON d.parent_id = parent_dept.parent_id
                  JOIN company_rows
                    ON d.dept_name = company_rows.dept_name
                   AND ISNULL(d.dept_category, N'') = company_rows.company_code
                 WHERE d.del_flag = N'0'
                   AND (d.dept_name <> company_rows.dept_name OR ISNULL(d.status, N'') <> company_rows.dept_status);

                ;WITH parent_dept AS (
                    SELECT
                        CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id,
                        CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN N'0,100' ELSE N'0' END AS ancestors
                ),
                company_rows AS (
                    SELECT
                        NULLIF(LTRIM(RTRIM(company_code)), N'') AS company_code,
                        COALESCE(
                            MAX(NULLIF(LTRIM(RTRIM(company_name)), N'')),
                            NULLIF(LTRIM(RTRIM(company_code)), N'')
                        ) AS dept_name,
                        MIN(CASE WHEN ISNULL(is_active, N'Y') = N'Y' THEN N'0' ELSE N'1' END) AS dept_status
                    FROM dbo.ce_company_factory
                    WHERE NULLIF(LTRIM(RTRIM(company_code)), N'') IS NOT NULL
                    GROUP BY NULLIF(LTRIM(RTRIM(company_code)), N'')
                ),
                candidates AS (
                    SELECT company_rows.company_code, company_rows.dept_name, company_rows.dept_status
                    FROM company_rows
                    CROSS JOIN parent_dept
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM dbo.sys_dept d
                        WHERE d.del_flag = N'0'
                          AND ISNULL(d.dept_category, N'') = company_rows.company_code
                          AND d.dept_name = company_rows.dept_name
                          AND d.parent_id = parent_dept.parent_id
                    )
                ),
                numbered AS (
                    SELECT
                        candidates.company_code,
                        candidates.dept_name,
                        candidates.dept_status,
                        ROW_NUMBER() OVER (ORDER BY candidates.company_code) AS rn
                    FROM candidates
                ),
                id_base AS (
                    SELECT CASE WHEN ISNULL(MAX(dept_id), 0) < 100000 THEN 100000 ELSE MAX(dept_id) END AS max_dept_id
                    FROM dbo.sys_dept
                ),
                tenant_value AS (
                    SELECT COALESCE((SELECT TOP 1 tenant_id FROM dbo.sys_dept WHERE tenant_id IS NOT NULL ORDER BY dept_id), N'000000') AS tenant_id
                )
                INSERT INTO dbo.sys_dept (
                    dept_id, tenant_id, parent_id, ancestors, dept_name, dept_category,
                    order_num, status, del_flag, create_dept, create_by, create_time
                )
                SELECT
                    id_base.max_dept_id + numbered.rn,
                    tenant_value.tenant_id,
                    parent_dept.parent_id,
                    parent_dept.ancestors,
                    numbered.dept_name,
                    numbered.company_code,
                    10 + numbered.rn,
                    numbered.dept_status,
                    N'0',
                    parent_dept.parent_id,
                    1,
                    SYSDATETIME()
                FROM numbered
                CROSS JOIN id_base
                CROSS JOIN parent_dept
                CROSS JOIN tenant_value;

                ;WITH parent_dept AS (
                    SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id
                ),
                factory_rows AS (
                    SELECT DISTINCT
                        NULLIF(LTRIM(RTRIM(company_code)), N'') AS company_code,
                        COALESCE(
                            MAX(NULLIF(LTRIM(RTRIM(company_name)), N'')),
                            NULLIF(LTRIM(RTRIM(company_code)), N'')
                        ) AS company_dept_name,
                        COALESCE(NULLIF(LTRIM(RTRIM(factory_name)), N''), NULLIF(LTRIM(RTRIM(factory_code)), N'')) AS dept_name,
                        MIN(CASE WHEN ISNULL(is_active, N'Y') = N'Y' THEN N'0' ELSE N'1' END) AS dept_status
                    FROM dbo.ce_company_factory
                    WHERE NULLIF(LTRIM(RTRIM(company_code)), N'') IS NOT NULL
                      AND (
                          NULLIF(LTRIM(RTRIM(factory_name)), N'') IS NOT NULL
                          OR NULLIF(LTRIM(RTRIM(factory_code)), N'') IS NOT NULL
                      )
                    GROUP BY
                        NULLIF(LTRIM(RTRIM(company_code)), N''),
                        COALESCE(NULLIF(LTRIM(RTRIM(factory_name)), N''), NULLIF(LTRIM(RTRIM(factory_code)), N''))
                )
                UPDATE d
                   SET d.parent_id = company_dept.dept_id,
                       d.ancestors = CONCAT(company_dept.ancestors, N',', company_dept.dept_id),
                       d.status = factory_rows.dept_status,
                       d.update_time = SYSDATETIME()
                  FROM dbo.sys_dept d
                  JOIN factory_rows
                    ON d.dept_name = factory_rows.dept_name
                   AND ISNULL(d.dept_category, N'') = factory_rows.company_code
                  JOIN parent_dept ON 1 = 1
                  JOIN dbo.sys_dept company_dept
                    ON company_dept.del_flag = N'0'
                   AND company_dept.parent_id = parent_dept.parent_id
                   AND company_dept.dept_name = factory_rows.company_dept_name
                   AND ISNULL(company_dept.dept_category, N'') = factory_rows.company_code
                 WHERE d.del_flag = N'0'
                   AND d.dept_id <> company_dept.dept_id
                   AND (
                       d.parent_id <> company_dept.dept_id
                       OR ISNULL(d.status, N'') <> factory_rows.dept_status
                       OR ISNULL(d.ancestors, N'') <> CONCAT(company_dept.ancestors, N',', company_dept.dept_id)
                   );

                ;WITH parent_dept AS (
                    SELECT
                        CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id,
                        CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN N'0,100' ELSE N'0' END AS ancestors
                ),
                factory_rows AS (
                    SELECT DISTINCT
                        NULLIF(LTRIM(RTRIM(company_code)), N'') AS company_code,
                        COALESCE(
                            MAX(NULLIF(LTRIM(RTRIM(company_name)), N'')),
                            NULLIF(LTRIM(RTRIM(company_code)), N'')
                        ) AS company_dept_name,
                        COALESCE(NULLIF(LTRIM(RTRIM(factory_name)), N''), NULLIF(LTRIM(RTRIM(factory_code)), N'')) AS dept_name,
                        MIN(CASE WHEN ISNULL(is_active, N'Y') = N'Y' THEN N'0' ELSE N'1' END) AS dept_status
                    FROM dbo.ce_company_factory
                    WHERE NULLIF(LTRIM(RTRIM(company_code)), N'') IS NOT NULL
                      AND (
                          NULLIF(LTRIM(RTRIM(factory_name)), N'') IS NOT NULL
                          OR NULLIF(LTRIM(RTRIM(factory_code)), N'') IS NOT NULL
                      )
                    GROUP BY
                        NULLIF(LTRIM(RTRIM(company_code)), N''),
                        COALESCE(NULLIF(LTRIM(RTRIM(factory_name)), N''), NULLIF(LTRIM(RTRIM(factory_code)), N''))
                ),
                candidates AS (
                    SELECT
                        factory_rows.dept_name,
                        factory_rows.company_code,
                        factory_rows.dept_status,
                        company_dept.dept_id AS parent_id,
                        CONCAT(company_dept.ancestors, N',', company_dept.dept_id) AS ancestors
                    FROM factory_rows
                    CROSS JOIN parent_dept
                    JOIN dbo.sys_dept company_dept
                      ON company_dept.del_flag = N'0'
                     AND company_dept.parent_id = parent_dept.parent_id
                     AND company_dept.dept_name = factory_rows.company_dept_name
                     AND ISNULL(company_dept.dept_category, N'') = factory_rows.company_code
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM dbo.sys_dept d
                        WHERE d.del_flag = N'0'
                          AND d.dept_name = factory_rows.dept_name
                          AND ISNULL(d.dept_category, N'') = factory_rows.company_code
                          AND d.parent_id = company_dept.dept_id
                    )
                ),
                numbered AS (
                    SELECT
                        candidates.dept_name,
                        candidates.company_code,
                        candidates.dept_status,
                        candidates.parent_id,
                        candidates.ancestors,
                        ROW_NUMBER() OVER (ORDER BY candidates.company_code, candidates.dept_name) AS rn
                    FROM candidates
                ),
                id_base AS (
                    SELECT CASE WHEN ISNULL(MAX(dept_id), 0) < 100000 THEN 100000 ELSE MAX(dept_id) END AS max_dept_id
                    FROM dbo.sys_dept
                ),
                tenant_value AS (
                    SELECT COALESCE((SELECT TOP 1 tenant_id FROM dbo.sys_dept WHERE tenant_id IS NOT NULL ORDER BY dept_id), N'000000') AS tenant_id
                )
                INSERT INTO dbo.sys_dept (
                    dept_id, tenant_id, parent_id, ancestors, dept_name, dept_category,
                    order_num, status, del_flag, create_dept, create_by, create_time
                )
                SELECT
                    id_base.max_dept_id + numbered.rn,
                    tenant_value.tenant_id,
                    numbered.parent_id,
                    numbered.ancestors,
                    numbered.dept_name,
                    numbered.company_code,
                    10 + numbered.rn,
                    numbered.dept_status,
                    N'0',
                    numbered.parent_id,
                    1,
                    SYSDATETIME()
                FROM numbered
                CROSS JOIN id_base
                CROSS JOIN tenant_value;

                DECLARE @moved_direct_depts TABLE (
                    dept_id BIGINT PRIMARY KEY,
                    old_ancestors NVARCHAR(500),
                    new_ancestors NVARCHAR(500),
                    source_level NVARCHAR(20)
                );

                ;WITH parent_dept AS (
                    SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id
                ),
                factory_rows AS (
                    SELECT
                        NULLIF(LTRIM(RTRIM(company_code)), N'') AS company_code,
                        COALESCE(
                            MAX(NULLIF(LTRIM(RTRIM(company_name)), N'')),
                            NULLIF(LTRIM(RTRIM(company_code)), N'')
                        ) AS company_dept_name,
                        COALESCE(NULLIF(LTRIM(RTRIM(factory_name)), N''), NULLIF(LTRIM(RTRIM(factory_code)), N'')) AS dept_name
                    FROM dbo.ce_company_factory
                    WHERE NULLIF(LTRIM(RTRIM(company_code)), N'') IS NOT NULL
                      AND ISNULL(is_active, N'Y') = N'Y'
                      AND (
                          NULLIF(LTRIM(RTRIM(factory_name)), N'') IS NOT NULL
                          OR NULLIF(LTRIM(RTRIM(factory_code)), N'') IS NOT NULL
                      )
                    GROUP BY
                        NULLIF(LTRIM(RTRIM(company_code)), N''),
                        COALESCE(NULLIF(LTRIM(RTRIM(factory_name)), N''), NULLIF(LTRIM(RTRIM(factory_code)), N''))
                ),
                ranked_factories AS (
                    SELECT
                        factory_rows.company_code,
                        factory_rows.company_dept_name,
                        factory_rows.dept_name,
                        ROW_NUMBER() OVER (
                            PARTITION BY factory_rows.company_code
                            ORDER BY
                                CASE
                                    WHEN factory_rows.dept_name = N'集团总部' THEN 0
                                    WHEN factory_rows.dept_name LIKE N'%总部%' THEN 1
                                    ELSE 2
                                END,
                                factory_rows.dept_name
                        ) AS rn
                    FROM factory_rows
                ),
                target_factories AS (
                    SELECT
                        parent_dept.parent_id AS root_parent_id,
                        company_dept.dept_id AS company_dept_id,
                        factory_dept.dept_id AS target_dept_id,
                        factory_dept.ancestors AS target_ancestors,
                        ranked_factories.company_code
                    FROM ranked_factories
                    CROSS JOIN parent_dept
                    JOIN dbo.sys_dept company_dept
                      ON company_dept.del_flag = N'0'
                     AND company_dept.parent_id = parent_dept.parent_id
                     AND company_dept.dept_name = ranked_factories.company_dept_name
                     AND ISNULL(company_dept.dept_category, N'') = ranked_factories.company_code
                    JOIN dbo.sys_dept factory_dept
                      ON factory_dept.del_flag = N'0'
                     AND factory_dept.parent_id = company_dept.dept_id
                     AND factory_dept.dept_name = ranked_factories.dept_name
                     AND ISNULL(factory_dept.dept_category, N'') = ranked_factories.company_code
                    WHERE ranked_factories.rn = 1
                ),
                movable_depts AS (
                    SELECT
                        d.dept_id,
                        target_factories.target_dept_id,
                        target_factories.target_ancestors,
                        N'company' AS source_level
                    FROM dbo.sys_dept d
                    JOIN target_factories
                      ON target_factories.company_dept_id = d.parent_id
                     AND target_factories.company_code = ISNULL(d.dept_category, N'')
                   WHERE d.del_flag = N'0'
                     AND NOT EXISTS (
                         SELECT 1
                           FROM factory_rows
                          WHERE factory_rows.company_code = ISNULL(d.dept_category, N'')
                            AND factory_rows.dept_name = d.dept_name
                     )
                    UNION ALL
                    SELECT
                        d.dept_id,
                        target_factories.target_dept_id,
                        target_factories.target_ancestors,
                        N'root' AS source_level
                    FROM dbo.sys_dept d
                    JOIN target_factories
                      ON target_factories.root_parent_id = d.parent_id
                     AND target_factories.company_code = ISNULL(d.dept_category, N'')
                   WHERE d.del_flag = N'0'
                     AND d.dept_id <> target_factories.company_dept_id
                     AND NOT EXISTS (
                         SELECT 1
                           FROM factory_rows
                          WHERE factory_rows.company_code = ISNULL(d.dept_category, N'')
                            AND factory_rows.dept_name = d.dept_name
                     )
                )
                UPDATE d
                   SET d.parent_id = movable_depts.target_dept_id,
                       d.ancestors = CONCAT(movable_depts.target_ancestors, N',', movable_depts.target_dept_id),
                       d.create_dept = movable_depts.target_dept_id,
                       d.update_time = SYSDATETIME()
                OUTPUT inserted.dept_id, deleted.ancestors, inserted.ancestors, movable_depts.source_level
                  INTO @moved_direct_depts (dept_id, old_ancestors, new_ancestors, source_level)
                  FROM dbo.sys_dept d
                  JOIN movable_depts ON movable_depts.dept_id = d.dept_id;

                UPDATE child
                   SET child.ancestors = CONCAT(
                           CONCAT(moved.new_ancestors, N',', moved.dept_id),
                           SUBSTRING(
                               child.ancestors,
                               LEN(CONCAT(moved.old_ancestors, N',', moved.dept_id)) + 1,
                               LEN(child.ancestors)
                           )
                       ),
                       child.update_time = SYSDATETIME()
                  FROM dbo.sys_dept child
                  JOIN @moved_direct_depts moved
                    ON child.dept_id <> moved.dept_id
                   AND (
                       child.ancestors = CONCAT(moved.old_ancestors, N',', moved.dept_id)
                       OR child.ancestors LIKE CONCAT(moved.old_ancestors, N',', moved.dept_id, N',%')
                   )
                 WHERE child.del_flag = N'0';

                ;WITH duplicate_roots AS (
                    SELECT child.dept_id
                      FROM dbo.sys_dept child
                      JOIN @moved_direct_depts moved
                        ON moved.source_level = N'root'
                       AND child.parent_id = moved.dept_id
                     WHERE child.del_flag = N'0'
                       AND NOT EXISTS (SELECT 1 FROM dbo.sys_user u WHERE u.dept_id = child.dept_id)
                ),
                duplicate_tree AS (
                    SELECT dept_id FROM duplicate_roots
                    UNION ALL
                    SELECT child.dept_id
                      FROM dbo.sys_dept child
                      JOIN duplicate_tree parent_tree ON parent_tree.dept_id = child.parent_id
                     WHERE child.del_flag = N'0'
                       AND NOT EXISTS (SELECT 1 FROM dbo.sys_user u WHERE u.dept_id = child.dept_id)
                )
                UPDATE d
                   SET d.del_flag = N'1',
                       d.status = N'1',
                       d.update_time = SYSDATETIME()
                  FROM dbo.sys_dept d
                  JOIN duplicate_tree ON duplicate_tree.dept_id = d.dept_id
                OPTION (MAXRECURSION 32767);
            END
            """);
    }

    @Override
    public void syncCompanyFactoryChange(String previousCompanyCode, String previousFactoryName,
                                         String currentCompanyCode, String currentFactoryName, String activeFlag) {
        boolean hasPreviousFactory = StringUtils.isNotBlank(previousCompanyCode) && StringUtils.isNotBlank(previousFactoryName);
        boolean hasCurrentFactory = StringUtils.isNotBlank(currentCompanyCode) && StringUtils.isNotBlank(currentFactoryName);
        boolean sameCompany = StringUtils.equals(previousCompanyCode, currentCompanyCode);
        boolean sameFactory = StringUtils.equals(previousFactoryName, currentFactoryName);

        if (hasPreviousFactory && hasCurrentFactory && sameCompany && !sameFactory) {
            renameCompanyFactoryDept(previousCompanyCode, previousFactoryName, currentFactoryName, activeFlag);
        } else if (hasPreviousFactory && (!sameCompany || !sameFactory)) {
            disableCompanyFactoryDept(previousCompanyCode, previousFactoryName);
        }
        syncCompanyFactoriesToSysDept();
    }

    @Override
    public void disableCompanyFactoryDept(String companyCode, String factoryName) {
        if (StringUtils.isBlank(companyCode) || StringUtils.isBlank(factoryName)) {
            return;
        }
        jdbcTemplate.update("""
            IF OBJECT_ID(N'dbo.sys_dept', N'U') IS NOT NULL
            BEGIN
                ;WITH parent_dept AS (
                    SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id
                )
                UPDATE d
                   SET d.status = N'1',
                       d.update_time = SYSDATETIME()
                  FROM dbo.sys_dept d
                  CROSS JOIN parent_dept
                  JOIN dbo.sys_dept company_dept
                    ON company_dept.del_flag = N'0'
                   AND company_dept.parent_id = parent_dept.parent_id
                   AND ISNULL(company_dept.dept_category, N'') = ?
                 WHERE d.del_flag = N'0'
                   AND ISNULL(d.dept_category, N'') = ?
                   AND d.dept_name = ?
                   AND d.parent_id = company_dept.dept_id
            END
            """, companyCode, companyCode, factoryName);
    }

    private void renameCompanyFactoryDept(String companyCode, String previousFactoryName,
                                          String currentFactoryName, String activeFlag) {
        String status = "N".equals(activeFlag) ? "1" : "0";
        try {
            jdbcTemplate.update("""
                IF OBJECT_ID(N'dbo.sys_dept', N'U') IS NOT NULL
                BEGIN
                    ;WITH parent_dept AS (
                        SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.sys_dept WHERE dept_id = 100) THEN 100 ELSE 0 END AS parent_id
                    )
                    UPDATE d
                       SET d.dept_name = ?,
                           d.status = ?,
                           d.update_time = SYSDATETIME()
                      FROM dbo.sys_dept d
                      CROSS JOIN parent_dept
                      JOIN dbo.sys_dept company_dept
                        ON company_dept.del_flag = N'0'
                       AND company_dept.parent_id = parent_dept.parent_id
                       AND ISNULL(company_dept.dept_category, N'') = ?
                     WHERE d.del_flag = N'0'
                       AND ISNULL(d.dept_category, N'') = ?
                       AND d.dept_name = ?
                       AND d.parent_id = company_dept.dept_id
                       AND NOT EXISTS (
                           SELECT 1
                             FROM dbo.sys_dept existing
                            WHERE existing.del_flag = N'0'
                              AND existing.parent_id = company_dept.dept_id
                              AND ISNULL(existing.dept_category, N'') = ?
                              AND existing.dept_name = ?
                       )
                END
                """, currentFactoryName, status, companyCode, companyCode, previousFactoryName, companyCode, currentFactoryName);
        } catch (Exception e) {
            log.warn("[CompanyFactoryDeptSync] factory department rename skipped: {}", e.getMessage());
        }
    }
}
