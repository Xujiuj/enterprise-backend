package org.dromara.carbon.enterprise.factor.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 电力排放因子维度实体.
 */
@Data
@TableName("ce_electricity_factor")
public class CeElectricityFactor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 版本省份编码 */
    private String versionProvinceCode;

    /** 因子版本 */
    private String factorVersion;

    /** 行政区划编码 */
    private String divisionCode;

    /** 行政区划名称 */
    private String divisionName;

    /** 区域名称 */
    private String regionName;

    /** 省级因子 */
    private BigDecimal provinceFactor;

    /** 区域因子 */
    private BigDecimal regionFactor;

    /** 全国因子 */
    private BigDecimal nationalFactor;

    /** 非化石能源排除因子 */
    private BigDecimal nonFossilExcludedFactor;

    /** 全国化石电力因子 */
    private BigDecimal nationalFossilPowerFactor;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;
}
