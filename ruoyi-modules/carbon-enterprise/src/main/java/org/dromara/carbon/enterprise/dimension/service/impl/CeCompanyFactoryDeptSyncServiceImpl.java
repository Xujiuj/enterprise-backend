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
