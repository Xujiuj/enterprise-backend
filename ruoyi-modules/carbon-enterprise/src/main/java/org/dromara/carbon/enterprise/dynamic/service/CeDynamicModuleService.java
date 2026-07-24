package org.dromara.carbon.enterprise.dynamic.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.dynamic.domain.CeDynamicModels;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysMenu;
import org.dromara.system.mapper.SysMenuMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Creates and operates SQL Server tables backed by reviewed Excel metadata.
 */
@RequiredArgsConstructor
@Service
public class CeDynamicModuleService {

    private static final long DYNAMIC_ROOT_MENU_ID = 900280L;
    private static final long ENTERPRISE_ADMIN_ROLE_ID = 900001L;
    private static final int MAX_IMPORT_ROWS = 20_000;
    private static final Pattern IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]{1,48}$");
    private static final Pattern COLUMN_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Set<String> VALUE_TYPES = Set.of("text", "number", "date", "boolean");
    private static final Set<String> UI_TYPES = Set.of("input", "textarea", "number", "date", "switch");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("yyyy年M月d日")
    );

    private final JdbcTemplate jdbcTemplate;
    private final SysMenuMapper sysMenuMapper;
    private final CeDynamicExcelParser excelParser;

    public CeDynamicModels.WorkbookPreview preview(MultipartFile file) {
        return excelParser.preview(file);
    }

    @Transactional(rollbackFor = Exception.class)
    public CeDynamicModels.GenerateResult generate(MultipartFile file, CeDynamicModels.GenerateRequest request) {
        ensureMetadataTables();
        if (request == null || request.getSheets() == null || request.getSheets().isEmpty()) {
            throw new ServiceException("至少选择一个工作表生成页面");
        }
        List<CeDynamicModels.SheetDefinition> selected = request.getSheets().stream()
            .filter(sheet -> !Boolean.FALSE.equals(sheet.getSelected()))
            .toList();
        if (selected.isEmpty()) {
            throw new ServiceException("至少选择一个工作表生成页面");
        }
        List<CeDynamicModels.ParsedSheet> parsedSheets = excelParser.parse(file);
        Map<Integer, CeDynamicModels.ParsedSheet> parsedByNo = new HashMap<>();
        parsedSheets.forEach(sheet -> parsedByNo.put(sheet.sheetNo(), sheet));
        Set<String> requestModuleCodes = new HashSet<>();
        CeDynamicModels.GenerateResult result = new CeDynamicModels.GenerateResult();
        List<CeDynamicModels.ModuleSchema> modules = new ArrayList<>();
        for (CeDynamicModels.SheetDefinition definition : selected) {
            validateDefinition(definition, requestModuleCodes);
            CeDynamicModels.ParsedSheet parsedSheet = parsedByNo.get(definition.getSheetNo());
            if (parsedSheet == null) {
                throw new ServiceException("未找到工作表: " + definition.getSheetName());
            }
            if (parsedSheet.headers().size() != definition.getFields().size()) {
                throw new ServiceException("工作表字段数量已变化，请重新预览后生成: " + definition.getSheetName());
            }
            modules.add(createModule(definition, parsedSheet));
        }
        result.setModules(modules);
        return result;
    }

    public List<CeDynamicModels.ModuleSchema> listModules() {
        ensureMetadataTables();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, module_code, module_name, table_name, sheet_name, permission_prefix, menu_id, status
            FROM ce_dynamic_module
            ORDER BY create_time DESC, id DESC
            """);
        return rows.stream().map(this::toSchema).toList();
    }

    public CeDynamicModels.ModuleSchema getSchema(String moduleCode) {
        ensureMetadataTables();
        validateModuleCode(moduleCode);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, module_code, module_name, table_name, sheet_name, permission_prefix, menu_id, status
            FROM ce_dynamic_module
            WHERE module_code = ? AND status = '0'
            """, moduleCode);
        if (rows.isEmpty()) {
            throw new ServiceException("动态页面不存在或已停用: " + moduleCode);
        }
        CeDynamicModels.ModuleSchema schema = toSchema(rows.get(0));
        schema.setFields(loadFields(schema.getId()));
        StpUtil.checkPermission(schema.getPermissionPrefix() + ":list");
        return schema;
    }

    public TableDataInfo<Map<String, Object>> listRecords(String moduleCode, Map<String, String> params) {
        CeDynamicModels.ModuleSchema schema = authorizedSchema(moduleCode, "list");
        List<CeDynamicModels.FieldDefinition> fields = schema.getFields();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        for (CeDynamicModels.FieldDefinition field : fields) {
            if (!Boolean.TRUE.equals(field.getSearchable())) {
                continue;
            }
            String raw = params.get(field.getFieldCode());
            if (StringUtils.isBlank(raw)) {
                continue;
            }
            if ("text".equals(field.getValueType())) {
                where.append(" AND ").append(quote(field.getDbColumn())).append(" LIKE ?");
                args.add("%" + raw.trim() + "%");
            } else {
                where.append(" AND ").append(quote(field.getDbColumn())).append(" = ?");
                args.add(convertValue(field, raw, false));
            }
        }

        long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM " + quote(schema.getTableName()) + where,
            Long.class,
            args.toArray()
        );
        int pageNum = positiveInt(params.get("pageNum"), 1, 1, Integer.MAX_VALUE);
        int pageSize = positiveInt(params.get("pageSize"), 10, 1, 500);
        int offset = (pageNum - 1) * pageSize;
        String orderBy = resolveOrderBy(params, fields);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(offset);
        pageArgs.add(pageSize);
        String sql = "SELECT " + selectColumns(fields) + " FROM " + quote(schema.getTableName()) + where
            + " ORDER BY " + orderBy + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, pageArgs.toArray());
        return new TableDataInfo<>(rows, total);
    }

    public Map<String, Object> getRecord(String moduleCode, Long id) {
        CeDynamicModels.ModuleSchema schema = authorizedSchema(moduleCode, "query");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT " + selectColumns(schema.getFields()) + " FROM " + quote(schema.getTableName()) + " WHERE id = ?",
            id
        );
        if (rows.isEmpty()) {
            throw new ServiceException("数据不存在");
        }
        return rows.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addRecord(String moduleCode, Map<String, Object> payload) {
        CeDynamicModels.ModuleSchema schema = authorizedSchema(moduleCode, "add");
        List<CeDynamicModels.FieldDefinition> fields = schema.getFields().stream()
            .filter(field -> Boolean.TRUE.equals(field.getFormVisible()))
            .toList();
        List<Object> values = valuesForWrite(fields, payload, true);
        List<String> columns = new ArrayList<>(fields.stream().map(field -> quote(field.getDbColumn())).toList());
        columns.add("create_by");
        columns.add("create_time");
        values.add(currentUserId());
        values.add(LocalDateTime.now());
        String sql = "INSERT INTO " + quote(schema.getTableName()) + " (" + String.join(", ", columns) + ") VALUES ("
            + String.join(", ", Collections.nCopies(values.size(), "?")) + ")";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(String moduleCode, Map<String, Object> payload) {
        CeDynamicModels.ModuleSchema schema = authorizedSchema(moduleCode, "edit");
        Long id = longValue(payload.get("id"), "id不能为空");
        List<CeDynamicModels.FieldDefinition> fields = schema.getFields().stream()
            .filter(field -> Boolean.TRUE.equals(field.getFormVisible()))
            .filter(field -> payload.containsKey(field.getFieldCode()))
            .toList();
        if (fields.isEmpty()) {
            throw new ServiceException("没有可更新的字段");
        }
        List<Object> values = valuesForWrite(fields, payload, false);
        List<String> assignments = new ArrayList<>();
        fields.forEach(field -> assignments.add(quote(field.getDbColumn()) + " = ?"));
        assignments.add("update_by = ?");
        assignments.add("update_time = ?");
        values.add(currentUserId());
        values.add(LocalDateTime.now());
        values.add(id);
        int updated = jdbcTemplate.update(
            "UPDATE " + quote(schema.getTableName()) + " SET " + String.join(", ", assignments) + " WHERE id = ?",
            values.toArray()
        );
        if (updated == 0) {
            throw new ServiceException("数据不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecords(String moduleCode, Long[] ids) {
        CeDynamicModels.ModuleSchema schema = authorizedSchema(moduleCode, "remove");
        if (ids == null || ids.length == 0) {
            throw new ServiceException("请选择要删除的数据");
        }
        if (ids.length > 500) {
            throw new ServiceException("单次最多删除 500 条数据");
        }
        String placeholders = String.join(", ", Collections.nCopies(ids.length, "?"));
        jdbcTemplate.update(
            "DELETE FROM " + quote(schema.getTableName()) + " WHERE id IN (" + placeholders + ")",
            Arrays.stream(ids).toArray()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public int importRows(String moduleCode, MultipartFile file) {
        CeDynamicModels.ModuleSchema schema = authorizedSchema(moduleCode, "add");
        List<CeDynamicModels.ParsedSheet> parsed = excelParser.parse(file);
        if (parsed.size() != 1) {
            throw new ServiceException("数据导入文件只能包含一个非空工作表");
        }
        CeDynamicModels.ParsedSheet sheet = parsed.get(0);
        if (sheet.headers().size() != schema.getFields().size()) {
            throw new ServiceException("导入表头与页面字段不一致");
        }
        for (int i = 0; i < schema.getFields().size(); i++) {
            if (!Objects.equals(sheet.headers().get(i).trim(), schema.getFields().get(i).getFieldName())) {
                throw new ServiceException("导入表头不一致，请下载当前页面模板后填写");
            }
        }
        insertParsedRows(schema, sheet.rows());
        return sheet.rows().size();
    }

    private CeDynamicModels.ModuleSchema createModule(CeDynamicModels.SheetDefinition definition,
                                                       CeDynamicModels.ParsedSheet parsedSheet) {
        String moduleCode = definition.getModuleCode().trim().toLowerCase(Locale.ROOT);
        String tableName = "ce_dyn_" + moduleCode;
        if (moduleExists(moduleCode) || tableExists(tableName)) {
            throw new ServiceException("模块编码或数据表已存在: " + moduleCode);
        }
        definition.setModuleCode(moduleCode);
        definition.getFields().forEach(field -> {
            field.setFieldCode(field.getFieldCode().trim().toLowerCase(Locale.ROOT));
            field.setDbColumn(field.getFieldCode());
        });
        createPhysicalTable(tableName, definition.getFields());
        Long moduleId = insertModule(definition, tableName);
        insertFields(moduleId, definition.getFields());
        String permissionPrefix = "enterprise:dynamic_" + moduleCode;
        Long menuId = createMenus(definition, permissionPrefix);
        jdbcTemplate.update(
            "UPDATE ce_dynamic_module SET menu_id = ?, permission_prefix = ? WHERE id = ?",
            menuId, permissionPrefix, moduleId
        );
        CeDynamicModels.ModuleSchema schema = getSchemaWithoutAuthorization(moduleCode);
        if (parsedSheet.rows().size() > MAX_IMPORT_ROWS) {
            throw new ServiceException("单个工作表最多导入 " + MAX_IMPORT_ROWS + " 行");
        }
        insertParsedRows(schema, parsedSheet.rows());
        return schema;
    }

    private void createPhysicalTable(String tableName, List<CeDynamicModels.FieldDefinition> fields) {
        List<String> columns = new ArrayList<>();
        columns.add("id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT " + quote("pk_" + tableName) + " PRIMARY KEY");
        for (CeDynamicModels.FieldDefinition field : fields) {
            String nullable = Boolean.TRUE.equals(field.getRequired()) ? " NOT NULL" : " NULL";
            columns.add(quote(field.getDbColumn()) + " " + sqlType(field) + nullable);
        }
        columns.add("create_by BIGINT NULL");
        columns.add("create_time DATETIME2 NULL");
        columns.add("update_by BIGINT NULL");
        columns.add("update_time DATETIME2 NULL");
        jdbcTemplate.execute("CREATE TABLE " + quote(tableName) + " (" + String.join(", ", columns) + ")");
    }

    private Long insertModule(CeDynamicModels.SheetDefinition definition, String tableName) {
        String sql = """
            INSERT INTO ce_dynamic_module
                (module_code, module_name, table_name, sheet_name, permission_prefix, status, create_by, create_time)
            VALUES (?, ?, ?, ?, ?, '0', ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, definition.getModuleCode());
            statement.setString(2, definition.getModuleName());
            statement.setString(3, tableName);
            statement.setString(4, definition.getSheetName());
            statement.setString(5, "enterprise:dynamic_" + definition.getModuleCode());
            statement.setLong(6, currentUserId());
            statement.setObject(7, LocalDateTime.now());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ServiceException("保存动态模块元数据失败");
        }
        return key.longValue();
    }

    private void insertFields(Long moduleId, List<CeDynamicModels.FieldDefinition> fields) {
        String sql = """
            INSERT INTO ce_dynamic_field
                (module_id, field_code, field_name, db_column, value_type, ui_type, required_flag,
                 searchable_flag, list_visible_flag, form_visible_flag, sort_order, max_length, numeric_precision, numeric_scale)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        List<Object[]> batch = new ArrayList<>();
        for (CeDynamicModels.FieldDefinition field : fields) {
            batch.add(new Object[]{
                moduleId, field.getFieldCode(), field.getFieldName(), field.getDbColumn(), field.getValueType(), field.getUiType(),
                bool(field.getRequired()), bool(field.getSearchable()), bool(field.getListVisible()), bool(field.getFormVisible()),
                field.getSortOrder(), field.getMaxLength(), field.getPrecision(), field.getScale()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    private Long createMenus(CeDynamicModels.SheetDefinition definition, String permissionPrefix) {
        if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject(
            "SELECT CASE WHEN EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = ?) THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END",
            Boolean.class,
            DYNAMIC_ROOT_MENU_ID
        ))) {
            throw new ServiceException("动态页面菜单目录未初始化，请先执行企业端 SQL 更新脚本");
        }
        Long pageMenuId = insertMenu(
            DYNAMIC_ROOT_MENU_ID,
            definition.getModuleName(),
            definition.getModuleCode(),
            "enterprise/dynamic/index",
            JsonUtils.toJsonString(Map.of("moduleCode", definition.getModuleCode())),
            "C",
            permissionPrefix + ":list",
            "table",
            10
        );
        insertMenu(pageMenuId, "查询", "#", "", "", "F", permissionPrefix + ":query", "#", 1);
        insertMenu(pageMenuId, "新增", "#", "", "", "F", permissionPrefix + ":add", "#", 2);
        insertMenu(pageMenuId, "修改", "#", "", "", "F", permissionPrefix + ":edit", "#", 3);
        insertMenu(pageMenuId, "删除", "#", "", "", "F", permissionPrefix + ":remove", "#", 4);
        refreshCurrentSessionPermissions(permissionPrefix);
        return pageMenuId;
    }

    private Long insertMenu(Long parentId, String name, String path, String component, String queryParam,
                            String menuType, String permission, String icon, int orderNum) {
        Long menuId = IdWorker.getId();
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setMenuName(name);
        menu.setOrderNum(orderNum);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setQueryParam(queryParam);
        menu.setIsFrame("1");
        menu.setIsCache("0");
        menu.setMenuType(menuType);
        menu.setVisible("F".equals(menuType) ? "1" : "0");
        menu.setStatus("0");
        menu.setPerms(permission);
        menu.setIcon(icon);
        menu.setRemark("Excel 动态页面自动生成");
        menu.setCreateBy(currentUserId());
        menu.setCreateTime(new java.util.Date());
        sysMenuMapper.insert(menu);
        grantEnterpriseAdmin(menuId);
        return menuId;
    }

    private void refreshCurrentSessionPermissions(String permissionPrefix) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            return;
        }
        Set<String> permissions = loginUser.getMenuPermission() == null
            ? new HashSet<>()
            : new HashSet<>(loginUser.getMenuPermission());
        permissions.add(permissionPrefix + ":list");
        permissions.add(permissionPrefix + ":query");
        permissions.add(permissionPrefix + ":add");
        permissions.add(permissionPrefix + ":edit");
        permissions.add(permissionPrefix + ":remove");
        loginUser.setMenuPermission(permissions);
    }

    private void grantEnterpriseAdmin(Long menuId) {
        jdbcTemplate.update("""
            IF NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = ? AND menu_id = ?)
                INSERT INTO sys_role_menu(role_id, menu_id) VALUES (?, ?)
            """, ENTERPRISE_ADMIN_ROLE_ID, menuId, ENTERPRISE_ADMIN_ROLE_ID, menuId);
    }

    private void insertParsedRows(CeDynamicModels.ModuleSchema schema, List<Map<Integer, String>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<CeDynamicModels.FieldDefinition> fields = schema.getFields();
        List<String> columns = fields.stream().map(field -> quote(field.getDbColumn())).toList();
        String sql = "INSERT INTO " + quote(schema.getTableName()) + " (" + String.join(", ", columns)
            + ", create_by, create_time) VALUES ("
            + String.join(", ", Collections.nCopies(fields.size() + 2, "?")) + ")";
        List<Object[]> batch = new ArrayList<>(rows.size());
        for (Map<Integer, String> row : rows) {
            Object[] values = new Object[fields.size() + 2];
            for (int index = 0; index < fields.size(); index++) {
                values[index] = convertValue(fields.get(index), row.get(index), true);
            }
            values[fields.size()] = currentUserId();
            values[fields.size() + 1] = LocalDateTime.now();
            batch.add(values);
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    private List<Object> valuesForWrite(List<CeDynamicModels.FieldDefinition> fields, Map<String, Object> payload,
                                        boolean create) {
        List<Object> values = new ArrayList<>(fields.size());
        for (CeDynamicModels.FieldDefinition field : fields) {
            Object raw = payload.get(field.getFieldCode());
            if (create && Boolean.TRUE.equals(field.getRequired()) && isBlank(raw)) {
                throw new ServiceException(field.getFieldName() + "不能为空");
            }
            values.add(convertValue(field, raw, true));
        }
        return values;
    }

    private Object convertValue(CeDynamicModels.FieldDefinition field, Object raw, boolean emptyAsNull) {
        if (raw == null || emptyAsNull && raw instanceof String text && StringUtils.isBlank(text)) {
            if (Boolean.TRUE.equals(field.getRequired())) {
                throw new ServiceException(field.getFieldName() + "不能为空");
            }
            return null;
        }
        String value = String.valueOf(raw).trim();
        try {
            return switch (field.getValueType()) {
                case "number" -> new BigDecimal(value.replace(",", ""));
                case "date" -> Date.valueOf(parseDate(value));
                case "boolean" -> parseBoolean(raw);
                default -> {
                    int maxLength = field.getMaxLength() == null ? 255 : field.getMaxLength();
                    if (value.length() > maxLength) {
                        throw new ServiceException(field.getFieldName() + "长度不能超过" + maxLength);
                    }
                    yield value;
                }
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ServiceException(field.getFieldName() + "格式不正确");
        }
    }

    private static LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        throw new DateTimeParseException("unsupported date", value, 0);
    }

    private static Boolean parseBoolean(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        String value = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        if (Set.of("true", "yes", "是", "1").contains(value)) {
            return true;
        }
        if (Set.of("false", "no", "否", "0").contains(value)) {
            return false;
        }
        throw new ServiceException("布尔字段只能填写是/否或 true/false");
    }

    private String selectColumns(List<CeDynamicModels.FieldDefinition> fields) {
        List<String> columns = new ArrayList<>();
        columns.add("id");
        fields.stream()
            .filter(field -> Boolean.TRUE.equals(field.getListVisible()) || Boolean.TRUE.equals(field.getFormVisible()))
            .forEach(field -> columns.add(quote(field.getDbColumn()) + " AS " + quote(field.getFieldCode())));
        columns.add("create_by AS createBy");
        columns.add("create_time AS createTime");
        columns.add("update_by AS updateBy");
        columns.add("update_time AS updateTime");
        return String.join(", ", columns);
    }

    private String resolveOrderBy(Map<String, String> params, List<CeDynamicModels.FieldDefinition> fields) {
        String requested = StringUtils.trimToEmpty(params.get("orderByColumn"));
        if (StringUtils.isBlank(requested) || "id".equals(requested)) {
            return "id " + sortDirection(params.get("isAsc"), "DESC");
        }
        CeDynamicModels.FieldDefinition field = fields.stream()
            .filter(item -> item.getFieldCode().equals(requested))
            .findFirst()
            .orElseThrow(() -> new ServiceException("不支持的排序字段"));
        return quote(field.getDbColumn()) + " " + sortDirection(params.get("isAsc"), "ASC");
    }

    private static String sortDirection(String value, String fallback) {
        return "desc".equalsIgnoreCase(value) || "descending".equalsIgnoreCase(value) ? "DESC"
            : "asc".equalsIgnoreCase(value) || "ascending".equalsIgnoreCase(value) ? "ASC" : fallback;
    }

    private CeDynamicModels.ModuleSchema authorizedSchema(String moduleCode, String action) {
        CeDynamicModels.ModuleSchema schema = getSchema(moduleCode);
        StpUtil.checkPermission(schema.getPermissionPrefix() + ":" + action);
        return schema;
    }

    private CeDynamicModels.ModuleSchema getSchemaWithoutAuthorization(String moduleCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, module_code, module_name, table_name, sheet_name, permission_prefix, menu_id, status
            FROM ce_dynamic_module WHERE module_code = ?
            """, moduleCode);
        if (rows.isEmpty()) {
            throw new ServiceException("动态模块创建失败");
        }
        CeDynamicModels.ModuleSchema schema = toSchema(rows.get(0));
        schema.setFields(loadFields(schema.getId()));
        return schema;
    }

    private CeDynamicModels.ModuleSchema toSchema(Map<String, Object> row) {
        CeDynamicModels.ModuleSchema schema = new CeDynamicModels.ModuleSchema();
        schema.setId(number(row, "id"));
        schema.setModuleCode(string(row, "module_code"));
        schema.setModuleName(string(row, "module_name"));
        schema.setTableName(string(row, "table_name"));
        schema.setSheetName(string(row, "sheet_name"));
        schema.setPermissionPrefix(string(row, "permission_prefix"));
        schema.setMenuId(number(row, "menu_id"));
        schema.setStatus(string(row, "status"));
        return schema;
    }

    private List<CeDynamicModels.FieldDefinition> loadFields(Long moduleId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT field_code, field_name, db_column, value_type, ui_type, required_flag, searchable_flag,
                   list_visible_flag, form_visible_flag, sort_order, max_length, numeric_precision, numeric_scale
            FROM ce_dynamic_field WHERE module_id = ? ORDER BY sort_order, id
            """, moduleId);
        return rows.stream().map(row -> {
            CeDynamicModels.FieldDefinition field = new CeDynamicModels.FieldDefinition();
            field.setFieldCode(string(row, "field_code"));
            field.setFieldName(string(row, "field_name"));
            field.setDbColumn(string(row, "db_column"));
            field.setValueType(string(row, "value_type"));
            field.setUiType(string(row, "ui_type"));
            field.setRequired(booleanValue(row, "required_flag"));
            field.setSearchable(booleanValue(row, "searchable_flag"));
            field.setListVisible(booleanValue(row, "list_visible_flag"));
            field.setFormVisible(booleanValue(row, "form_visible_flag"));
            field.setSortOrder(intValue(row, "sort_order"));
            field.setMaxLength(intValue(row, "max_length"));
            field.setPrecision(intValue(row, "numeric_precision"));
            field.setScale(intValue(row, "numeric_scale"));
            return field;
        }).toList();
    }

    private void validateDefinition(CeDynamicModels.SheetDefinition definition, Set<String> moduleCodes) {
        if (definition == null) {
            throw new ServiceException("页面定义不能为空");
        }
        String moduleCode = StringUtils.trimToEmpty(definition.getModuleCode()).toLowerCase(Locale.ROOT);
        validateModuleCode(moduleCode);
        if (!moduleCodes.add(moduleCode)) {
            throw new ServiceException("模块编码重复: " + moduleCode);
        }
        if (StringUtils.isBlank(definition.getModuleName()) || definition.getModuleName().length() > 100) {
            throw new ServiceException("页面名称不能为空且不能超过100个字符");
        }
        if (definition.getFields() == null || definition.getFields().isEmpty()) {
            throw new ServiceException("页面至少需要一个字段");
        }
        Set<String> fieldCodes = new HashSet<>();
        for (int i = 0; i < definition.getFields().size(); i++) {
            CeDynamicModels.FieldDefinition field = definition.getFields().get(i);
            String fieldCode = StringUtils.trimToEmpty(field.getFieldCode()).toLowerCase(Locale.ROOT);
            if (!COLUMN_IDENTIFIER.matcher(fieldCode).matches()) {
                throw new ServiceException("字段编码不合法: " + fieldCode);
            }
            if (!fieldCodes.add(fieldCode)) {
                throw new ServiceException("字段编码重复: " + fieldCode);
            }
            if (StringUtils.isBlank(field.getFieldName()) || field.getFieldName().length() > 120) {
                throw new ServiceException("字段名称不能为空且不能超过120个字符");
            }
            if (!VALUE_TYPES.contains(field.getValueType()) || !UI_TYPES.contains(field.getUiType())) {
                throw new ServiceException("字段类型不受支持: " + field.getFieldName());
            }
            if (Boolean.TRUE.equals(field.getRequired()) && !Boolean.TRUE.equals(field.getFormVisible())) {
                throw new ServiceException("必填字段必须在表单中显示: " + field.getFieldName());
            }
            field.setFieldCode(fieldCode);
            field.setDbColumn(fieldCode);
            field.setSortOrder(i + 1);
            field.setMaxLength(Math.max(1, Math.min(field.getMaxLength() == null ? 255 : field.getMaxLength(), 2000)));
            field.setPrecision(30);
            field.setScale(10);
        }
    }

    private static void validateModuleCode(String moduleCode) {
        if (!IDENTIFIER.matcher(StringUtils.trimToEmpty(moduleCode)).matches()) {
            throw new ServiceException("模块编码必须以小写字母开头，只能包含小写字母、数字和下划线，长度2-49位");
        }
    }

    private boolean moduleExists(String moduleCode) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM ce_dynamic_module WHERE module_code = ?", Integer.class, moduleCode);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM sys.tables WHERE name = ? AND schema_id = SCHEMA_ID('dbo')", Integer.class, tableName);
        return count != null && count > 0;
    }

    private String sqlType(CeDynamicModels.FieldDefinition field) {
        return switch (field.getValueType()) {
            case "number" -> "DECIMAL(30,10)";
            case "date" -> "DATE";
            case "boolean" -> "BIT";
            default -> "NVARCHAR(" + Math.max(1, Math.min(field.getMaxLength(), 2000)) + ")";
        };
    }

    public static String quote(String identifier) {
        if (!COLUMN_IDENTIFIER.matcher(identifier).matches() && !identifier.matches("^ce_dyn_[a-z0-9_]{2,55}$")
            && !identifier.matches("^pk_ce_dyn_[a-z0-9_]{2,55}$")) {
            throw new ServiceException("非法数据库标识符");
        }
        return "[" + identifier + "]";
    }

    private void ensureMetadataTables() {
        jdbcTemplate.execute("""
            IF OBJECT_ID(N'dbo.ce_dynamic_module', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.ce_dynamic_module (
                    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_ce_dynamic_module PRIMARY KEY,
                    module_code NVARCHAR(50) NOT NULL,
                    module_name NVARCHAR(100) NOT NULL,
                    table_name NVARCHAR(64) NOT NULL,
                    sheet_name NVARCHAR(100) NULL,
                    permission_prefix NVARCHAR(100) NOT NULL,
                    menu_id BIGINT NULL,
                    status NCHAR(1) NOT NULL CONSTRAINT df_ce_dynamic_module_status DEFAULT (N'0'),
                    create_by BIGINT NULL,
                    create_time DATETIME2 NULL,
                    update_by BIGINT NULL,
                    update_time DATETIME2 NULL
                );
                CREATE UNIQUE INDEX ux_ce_dynamic_module_code ON dbo.ce_dynamic_module(module_code);
                CREATE UNIQUE INDEX ux_ce_dynamic_module_table ON dbo.ce_dynamic_module(table_name);
            END
            """);
        jdbcTemplate.execute("""
            IF OBJECT_ID(N'dbo.ce_dynamic_field', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.ce_dynamic_field (
                    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_ce_dynamic_field PRIMARY KEY,
                    module_id BIGINT NOT NULL,
                    field_code NVARCHAR(64) NOT NULL,
                    field_name NVARCHAR(120) NOT NULL,
                    db_column NVARCHAR(64) NOT NULL,
                    value_type NVARCHAR(20) NOT NULL,
                    ui_type NVARCHAR(20) NOT NULL,
                    required_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_required DEFAULT (0),
                    searchable_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_searchable DEFAULT (0),
                    list_visible_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_list DEFAULT (1),
                    form_visible_flag BIT NOT NULL CONSTRAINT df_ce_dynamic_field_form DEFAULT (1),
                    sort_order INT NOT NULL,
                    max_length INT NULL,
                    numeric_precision INT NULL,
                    numeric_scale INT NULL,
                    CONSTRAINT fk_ce_dynamic_field_module FOREIGN KEY (module_id) REFERENCES dbo.ce_dynamic_module(id)
                );
                CREATE UNIQUE INDEX ux_ce_dynamic_field_code ON dbo.ce_dynamic_field(module_id, field_code);
            END
            """);
    }

    private static int positiveInt(String raw, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(StringUtils.trimToEmpty(raw));
            return Math.max(min, Math.min(value, max));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean isBlank(Object value) {
        return value == null || value instanceof String text && StringUtils.isBlank(text);
    }

    private static long currentUserId() {
        Long userId = LoginHelper.getUserId();
        return userId == null ? 0L : userId;
    }

    private static long longValue(Object value, String message) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            throw new ServiceException(message);
        }
    }

    private static int bool(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private static String string(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer intValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Boolean booleanValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value instanceof Number number && number.intValue() != 0;
    }
}
