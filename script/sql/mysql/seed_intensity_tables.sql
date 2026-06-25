-- 强度管理种子数据：分母维度规则、强度目标、强度指标
-- 与现有 factory_type（集团总部/多晶硅生产/电力生产/水泥生产）保持一致

SET NAMES utf8mb4;

-- 1. ce_intensity_denominator_rule（碳排放强度分母维度表）
INSERT INTO ce_intensity_denominator_rule
  (denominator_rule_key, factory_type, denominator_type, denominator_metric_name, intensity_unit_display, enabled_flag, remark)
VALUES
  ('RULE-SILICON-OUTPUT',   '多晶硅生产', '产品产量', '多晶硅产量',             '吨',   1, 'source(A)'),
  ('RULE-SILICON-POWER',    '多晶硅生产', '电力消耗', '多晶硅生产用电量',         'MWh',  1, 'source(A)'),
  ('RULE-SILICON-RAW',      '多晶硅生产', '原材料量', '多晶硅原材料消耗量',       '吨',   1, 'source(A)'),
  ('RULE-POWER-OUTPUT',     '电力生产',   '发电量',   '火力发电量',               'MWh',  1, 'source(A)'),
  ('RULE-POWER-HEAT',       '电力生产',   '供热量',   '热电联产供热总量',         'GJ',   1, 'source(A)'),
  ('RULE-CEMENT-OUTPUT',    '水泥生产',   '产品产量', '水泥熟料产量',             '吨',   1, 'source(A)'),
  ('RULE-CEMENT-CLINKER',   '水泥生产',   '熟料产量', '水泥熟料生产量',           '吨',   1, 'source(A)'),
  ('RULE-HQ-AREA',          '集团总部',   '建筑面积', '集团总部办公建筑面积',     'm²',   1, 'source(A)');

-- 2. ce_intensity_target（强度目标表）
INSERT INTO ce_intensity_target
  (factory_type, target_year, target_value, unit_name, remark)
VALUES
  ('多晶硅生产', 2024, 8.50,  'tCO2e/吨多晶硅',   'source(A)'),
  ('多晶硅生产', 2025, 8.20,  'tCO2e/吨多晶硅',   'source(A)'),
  ('多晶硅生产', 2026, 7.80,  'tCO2e/吨多晶硅',   'source(A)'),
  ('电力生产',   2024, 0.85,  'tCO2e/MWh',        'source(A)'),
  ('电力生产',   2025, 0.82,  'tCO2e/MWh',        'source(A)'),
  ('电力生产',   2026, 0.78,  'tCO2e/MWh',        'source(A)'),
  ('水泥生产',   2024, 0.72,  'tCO2e/吨熟料',     'source(A)'),
  ('水泥生产',   2025, 0.70,  'tCO2e/吨熟料',     'source(A)'),
  ('水泥生产',   2026, 0.68,  'tCO2e/吨熟料',     'source(A)'),
  ('集团总部',   2024, 0.035, 'tCO2e/m²',         'source(A)'),
  ('集团总部',   2025, 0.033, 'tCO2e/m²',         'source(A)'),
  ('集团总部',   2026, 0.030, 'tCO2e/m²',         'source(A)');

-- 3. ce_intensity_metric（碳排放强度指标）
-- 引用 ce_intensity_denominator_fact 中已有的实际分母数据进行计算
-- 公式：intensity_value = numerator_emission / denominator_value
INSERT INTO ce_intensity_metric
  (metric_code, metric_name, rule_code, metric_period, numerator_emission, denominator_fact_id, denominator_value, denominator_unit, intensity_value, target_code, metric_status, remark)
SELECT
  CONCAT('IM-', df.factory_code, '-', df.fact_year, '-', LPAD(df.fact_month, 2, '0')),
  CONCAT(cf.factory_name, ' ', df.fact_year, '年', df.fact_month, '月碳排放强度'),
  CASE df.denominator_type
    WHEN '产品产量' THEN 'RULE-SILICON-OUTPUT'
    WHEN '电力消耗' THEN 'RULE-SILICON-POWER'
    WHEN '原材料量' THEN 'RULE-SILICON-RAW'
    WHEN '发电量'   THEN 'RULE-POWER-OUTPUT'
    ELSE 'RULE-CEMENT-OUTPUT'
  END,
  CONCAT(df.fact_year, '-', LPAD(df.fact_month, 2, '0')),
  ROUND(df.denominator_value * 0.85, 2),
  df.id,
  df.denominator_value,
  df.unit_name,
  CASE WHEN df.denominator_value > 0
    THEN ROUND(df.denominator_value * 0.85 / df.denominator_value, 4)
    ELSE 0
  END,
  CONCAT('TARGET-', df.fact_year),
  'draft',
  'source(A)'
FROM ce_intensity_denominator_fact df
JOIN ce_company_factory cf ON cf.factory_code = df.factory_code
WHERE df.denominator_value > 0;
