package org.dromara.carbon.enterprise.factor.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 温室气体维度实体.
 */
@Data
@TableName("ce_greenhouse_gas")
public class CeGreenhouseGas implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 气体编码 */
    private String gasCode;

    /** 气体名称 */
    private String gasName;

    /** 气体英文名称 */
    private String gasNameEn;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;
}
