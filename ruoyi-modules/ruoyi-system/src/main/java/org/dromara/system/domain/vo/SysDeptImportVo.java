package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Department Excel import row. Organization IDs are resolved by the backend.
 */
@Data
@NoArgsConstructor
@ExcelIgnoreUnannotated
public class SysDeptImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("所属公司（名称或编号）")
    private String company;

    @ExcelProperty("所属工厂（名称）")
    private String factory;

    @ExcelProperty("上级部门（可选，支持/分层）")
    private String parentDepartment;

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("显示排序")
    private Integer orderNum;

    @ExcelProperty("负责人账号（可选）")
    private String leaderUserName;

    @ExcelProperty("联系电话（可选）")
    private String phone;

    @ExcelProperty("邮箱（可选）")
    private String email;

    @ExcelProperty("状态（正常/停用）")
    private String status;
}
