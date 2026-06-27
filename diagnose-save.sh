#!/bin/bash
# 诊断脚本：测试活动数据保存功能

echo "=== 诊断：活动数据保存功能 ==="

# 1. 检查服务器状态
echo "1. 检查服务器状态..."
curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "服务器未启动或无法访问"

# 2. 检查数据库表
echo ""
echo "2. 检查数据库表是否存在..."
echo "需要检查以下表："
echo "  - ce_capture_batch"
echo "  - ce_capture_row"
echo "  - ce_capture_cell"
echo "  - ce_template_sheet"
echo "  - ce_template_field"

# 3. 检查模板配置
echo ""
echo "3. 检查模板配置..."
echo "需要验证 ce_template_sheet 中存在 sheet_656 的配置"

# 4. 提示查看日志
echo ""
echo "4. 查看诊断日志..."
echo "请在浏览器中测试保存功能，然后检查日志文件"
echo "日志位置: D:\\project\\fx\\enterprise-backend\\logs\\sys-console.log"
echo "关注标记: 【诊断】开头的日志"
echo ""
echo "关键检查点："
echo "  - 【诊断】validateAndPersist 开始: 确认请求是否到达后端"
echo "  - 【诊断】验证结果: 检查 isValid 和 blocking 的值"
echo "  - 【诊断】找到模板: 确认模板配置是否存在"
echo "  - 【诊断】插入批次成功: 确认数据库插入是否成功"
echo "  - 【诊断】持久化行成功: 确认数据是否完整保存"
