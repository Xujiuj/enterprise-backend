package org.dromara.carbon.enterprise.dimension.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 基准年维度实体.
 */
@Data
@TableName("ce_base_year")
public class CeBaseYear implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 工厂编码 */
    private String factoryCode;

    /** 工厂名称 */
    private String factoryName;

    /** 厂商基准年业务键 */
    private String baseYearKey;

    /** 厂商描述 */
    private String description;

    /** 基准年 */
    private Integer baseYear;

    /** 厂商当前基准标识 */
    private Integer isCurrent;

    /** 启用标志 1启用 0停用 */
    private Integer enabledFlag;

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
