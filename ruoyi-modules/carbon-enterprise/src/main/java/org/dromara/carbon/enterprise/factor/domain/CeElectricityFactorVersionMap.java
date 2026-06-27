package org.dromara.carbon.enterprise.factor.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 电力因子版本映射维度实体.
 */
@Data
@TableName("ce_electricity_factor_version_map")
public class CeElectricityFactorVersionMap implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 因子版本 */
    private String factorVersion;

    /** 生效年度 */
    private Integer effectiveYear;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;
}
