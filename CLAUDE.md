# 企业碳数据管理平台 — 后端（RuoYi-Vue-Plus）开发约定

> 本文件是**企业端后端项目级**开发约定，Claude Code 在 `D:\project\fx\enterprise-backend` 工作时自动加载。
> 基座 **RuoYi-Vue-Plus v5.6.1**（dromara，单体增强版）。
> 全局总纲见上级 `../CLAUDE.md`；需求与设计依据见 `../docs/`（01 项目约束、02 开源选型与需求改动）；UI/交互基准见 `../html-prototype/`。

## 一、回复语言

始终使用**中文**回复。

## 二、全局差异化约束（写代码前必读，详见 ../docs/01、../docs/02）

1. **数据库统一 SQL Server（2016+）**，非 MySQL。dynamic-datasource 已内置 sqlserver 数据源块（`application-dev.yml` 中注释状态），驱动 `com.microsoft.sqlserver.jdbc.SQLServerDriver`；中文列用 `NVARCHAR`；建表脚本/分页/自增需按 SQL Server 方言。
2. **双端同入口**：厂商端 vendor + 企业端 enterprise 共享同一套代码与登录页，按角色 `portal` 渲染不同菜单，避免同名动作串台。
3. **License 体系**：RSA 私钥签发 `.lic`、公钥内置验签、设备绑定 `installId`、防时间回拨、写 `LicenseState` 表；登录/操作前置校验；外部 API 权限由 License `features` 驱动，企业端零改动。
4. **私有化单体部署**：一个 jar + 一个本地 SQL Server 库，排除微服务版（RuoYi-Cloud）。
5. **自定义扩展字段仅限三模块**（活动数据 / 绿电绿证 / 强度分母），采用 JSON 扩展列 + 元数据表，其余模块不可加字段。
6. **Power BI 为外部软件**：平台只导出 `.pbix` 模板并提供 rpt 受控视图连接参数，不内嵌 PBI 运行时。
7. **维度字段不可自由输入**：能下拉的下拉、能带出的带出（关联字段联动）。
8. 全站**非衬线字体**，优先 OPPOSans。

## 三、技术栈（以 pom.xml 为准，勿擅自升降级）

| 领域 | 选型 | 版本 |
|---|---|---|
| 框架 | Spring Boot | 3.5.14 |
| JDK | Java | 17 |
| ORM | MyBatis-Plus | 3.5.16 |
| 多数据源 | dynamic-datasource | 4.3.1 |
| 鉴权 | Sa-Token | 1.45.0 |
| 工具库 | Hutool | 5.8.43 |
| 对象转换 | MapStruct-Plus | 1.5.0 |
| Excel | FastExcel | 1.3.0 |
| 缓存/分布式锁 | Redis + Redisson | 3.52.0 |
| 工作流 | Warm-Flow | 1.8.5 |
| API 文档 | SpringDoc | 2.8.17 |
| 代码生成 | Velocity | 2.3 |

## 四、模块结构

```
enterprise-backend/
├── ruoyi-admin/          启动模块（含 application*.yml、web 登录控制器）
├── ruoyi-common/         公共能力（按子模块拆分，见下）
├── ruoyi-modules/        业务模块：system / generator / job / workflow / demo
└── ruoyi-extend/         扩展（监控等）
```

`ruoyi-common` 子模块按能力划分（core / mybatis / satoken / excel / log / redis / tenant / web / oss / sms / mail / sse / websocket / idempotent / ratelimiter / encrypt / sensitive / translation / social / job / doc / json / security）。**复用既有公共模块，勿重复造轮子。**

本项目新增业务（5 大碳数据域、License、因子库等）放入 `ruoyi-modules/` 下新建模块，包名沿用 `org.dromara.{module}`。

## 五、分层与命名（严格遵循基座范式）

包结构：`org.dromara.{module}.{controller|service|service.impl|mapper|domain|domain.bo|domain.vo}`

| 层 | 约定 |
|---|---|
| Controller | `XxxController` extends `BaseController`；`@RestController` + `@RequestMapping("/模块/资源")` |
| Service 接口 | `IXxxService`（`I` 前缀） |
| Service 实现 | `XxxServiceImpl implements IXxxService`，位于 `service.impl` |
| Mapper | `XxxMapper`，继承 `BaseMapperPlus` |
| 实体 | `Xxx` extends `TenantEntity`（多租户）或 `BaseEntity`；`@TableName("表名")` |
| 入参对象 | `XxxBo`（business object），位于 `domain.bo`，带校验注解 |
| 出参对象 | `XxxVo`（view object），位于 `domain.vo` |

## 六、编码风格（与基座现有代码保持一致）

- **缩进 4 空格**；UTF-8；类/方法/实体字段**必须有 JavaDoc**，类注释带 `@author`。
- **依赖注入用构造注入**：类加 `@RequiredArgsConstructor`，字段 `private final XxxService`，**禁止 `@Autowired` 字段注入**。Mapper 注入字段统一命名 `baseMapper`。
- **实体**：`@Data` + `@EqualsAndHashCode(callSuper = true)` + `@TableName`；主键 `@TableId`；每个字段加注释，状态字段注明取值（如 `0正常 1停用`）。
- **Controller 方法**按需叠加注解：`@SaCheckPermission("模块:资源:动作")` 权限、`@Log(title, businessType)` 操作日志、`@RepeatSubmit` 防重复提交、`@Validated`。
- **返回值**：单对象/操作用 `R<T>`（`R.ok()` / `R.fail()` / `toAjax()`）；分页列表用 `TableDataInfo<T>`（`TableDataInfo.build(page)`）。
- **查询**：用 `LambdaQueryWrapper` + `StringUtils.isNotBlank(...)` 条件式拼接；分页用 `PageQuery.build()`。
- **对象转换**：用 `MapstructUtils.convert(...)` 在 Bo/Entity/Vo 间转换，勿手写 getter/setter 搬运。
- **Excel 导入导出**：用 `ExcelUtil.exportExcel(...)`，VO 字段配 FastExcel 注解。
- **权限标识**冒号分隔三段：`{模块}:{资源}:{动作}`，与前端 `v-hasPermi` 对齐。

## 七、查找代码（CodeGraph）

本仓库已建立**独立 CodeGraph 索引**（仓库内 `.codegraph/`）。结构性问题（谁调谁、定义在哪、签名、改动影响面）优先用 `codegraph_*` 工具；字面文本/注释/日志检索才用 grep。详见全局 `~/.claude/CLAUDE.md` 的 CodeGraph 段。
