SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清理旧的 source(A) 记录，为重新导入做准备
DELETE FROM ce_extension_field_value WHERE owner_record_id IN (SELECT id FROM ce_activity_data WHERE remark = 'source(A)');
DELETE FROM ce_activity_data WHERE remark = 'source(A)';
DELETE FROM ce_green_power_certificate WHERE remark = 'source(A)';
DELETE FROM ce_intensity_metric WHERE remark = 'source(A)';
DELETE FROM ce_intensity_denominator_fact WHERE remark = 'source(A)';
DELETE FROM ce_intensity_target WHERE remark = 'source(A)';
DELETE FROM ce_intensity_tolerance WHERE remark = 'source(A)';
DELETE FROM ce_intensity_denominator_rule WHERE remark = 'source(A)';
DELETE FROM ce_emission_source WHERE remark = 'source(A)';
DELETE FROM ce_ef_factor WHERE remark = 'source(A)';
DELETE FROM ce_base_year WHERE remark = 'source(A)';
DELETE FROM ce_emission_source_category WHERE remark = 'source(A)';
DELETE FROM ce_company_factory WHERE remark = 'source(A)';
DELETE FROM ce_admin_division WHERE remark = 'source(A)';

SET FOREIGN_KEY_CHECKS = 1;
