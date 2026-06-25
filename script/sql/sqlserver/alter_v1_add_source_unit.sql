-- ============================================================
-- 迁移脚本：为 ce_emission_source 表添加 source_unit 列
-- 执行方式：在 SQL Server Management Studio 或 sqlcmd 中执行
-- 日期：2026-06-25
-- ============================================================

-- 添加 source_unit 列（排放源单位，如 m3、kg、t.km 等）
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('ce_emission_source') AND name = 'source_unit')
BEGIN
    ALTER TABLE ce_emission_source ADD source_unit NVARCHAR(64) NULL;
    PRINT '已添加 ce_emission_source.source_unit 列';
END
ELSE
BEGIN
    PRINT 'ce_emission_source.source_unit 列已存在，跳过';
END
GO
