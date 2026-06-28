package org.dromara.carbon.enterprise.factor.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 电力因子范围维度实体.
 */
@Data
@TableName("ce_electricity_factor_scope")
public class CeElectricityFactorScope implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 范围编码 */
    private String scopeKey;

    /** 范围名称 */
    private String scopeName;

    /** 厂商排序号 */
    private Integer sortOrder;

    /** 厂商状态 */
    private String status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;
}
