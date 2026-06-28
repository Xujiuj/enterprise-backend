# Enterprise Backend

企业端本地后端服务，基于 RuoYi-Vue-Plus 保留认证、权限、审计和通用工程能力，承载企业本地碳数据填报、Source(A) 数据导入、License 状态、因子同步、报表视图与本地接口。

## Delivery Boundary

- 运行形态：企业本地单体 Spring Boot/Maven 后端。
- 数据库：统一使用 SQL Server `enterprise`，不保留其他数据库兼容路径。
- 配置：企业数据库连接信息通过环境变量或外部配置提供，不写入源码，不向厂商端泄露。
- 数据边界：只写企业端本地业务表，不写厂商端业务库。
- 报表边界：Power BI 通过受控 SQL Server 视图读取企业本地数据，不直连厂商端数据。

## Modules

```text
enterprise-backend/
  ruoyi-admin/             # application entry and HTTP controllers
  ruoyi-common/            # shared framework capabilities retained by enterprise delivery
  ruoyi-modules/
    carbon-enterprise/     # enterprise carbon domain
    ruoyi-system/          # retained system/RBAC/audit domain
  script/sql/sqlserver/    # current SQL Server scripts
  deploy/                  # enterprise-side external configuration templates
```

## Verification

```bash
rtk mvn -pl ruoyi-modules/carbon-enterprise -am -DskipTests=false test
rtk mvn -pl ruoyi-admin -am package -DskipTests
```
