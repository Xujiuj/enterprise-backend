# 诊断和修复：活动数据保存问题

## 问题描述
- 用户报告：企业端排放活动数据新增时，点击保存显示成功，但数据没有增加
- 需要：查看服务器日志，检查原因并修复

## 根本原因分析

### 已发现的潜在问题

1. **数据库表可能不存在**
   - 相关表：ce_capture_batch, ce_capture_row, ce_capture_cell
   - 表定义在：`enterprise-backend/script/sql/mysql/carbon_enterprise_schema_v1.sql`
   - 可能原因：数据库迁移脚本未执行或执行失败

2. **模板配置可能缺失**
   - 相关表：ce_template_sheet, ce_template_field
   - 需要检查：是否配置了 sheet_656 的模板

3. **验证逻辑可能过于严格**
   - 后端在保存前会验证数据
   - 如果验证失败，不会插入数据库

4. **前端参数传递可能有问题**
   - 需要确认前端构建的请求数据格式是否正确

## 修复方案

### 步骤1：验证数据库表是否存在

```sql
-- 检查 capture 相关表是否存在
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'enterprise' 
  AND TABLE_NAME IN ('ce_capture_batch', 'ce_capture_row', 'ce_capture_cell');

-- 检查模板相关表
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'enterprise' 
  AND TABLE_NAME IN ('ce_template_sheet', 'ce_template_field');
```

### 步骤2：检查模板配置

```sql
-- 检查 sheet_656 模板是否配置
SELECT * FROM ce_template_sheet WHERE target_table_code = 'sheet_656';
SELECT * FROM ce_template_field WHERE sheet_id IN (
    SELECT id FROM ce_template_sheet WHERE target_table_code = 'sheet_656'
);
```

### 步骤3：执行数据库迁移脚本（如果表不存在）

```bash
# 执行主 schema 脚本
mysql -u root -p enterprise < enterprise-backend/script/sql/mysql/carbon_enterprise_schema_v1.sql
```

### 步骤4：添加诊断日志（已完成）

在 `CeSheet656ActivityCaptureServiceImpl.validateAndPersist` 方法中添加了诊断日志，标记为【诊断】。

### 步骤5：重启服务器并测试

```bash
# 重新构建项目
cd enterprise-backend
mvn clean package -DskipTests

# 重启服务器（假设使用 Spring Boot）
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

### 步骤6：查看诊断日志

```bash
# 查看最新的日志
tail -f enterprise-backend/logs/sys-console.log | grep "【诊断】"
```

## 关键检查点

在浏览器中重现问题后，查看日志：

1. **【诊断】validateAndPersist 开始** → 请求到达后端
2. **【诊断】验证结果** → 检查 isValid 和 blocking 的值
   - 如果 isValid=false：数据验证失败，需要检查验证逻辑
   - 如果 blocking=true：存在阻塞性错误，需要修复
3. **【诊断】找到模板** → 确认模板配置存在
   - 如果异常：模板配置缺失，需要创建
4. **【诊断】插入批次成功** → 数据库插入成功
5. **【诊断】持久化行成功** → 数据完整保存

## 预期修复结果

1. 数据库表正确创建
2. 模板配置完整
3. 保存功能正常工作
4. 数据成功插入数据库
5. 用户看到"保存成功"后，数据能正确显示

## 附加诊断工具

已创建诊断脚本：`enterprise-backend/diagnose-save.sh`
- 检查服务器状态
- 检查数据库表
- 提供日志查看指南
