package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise dimension record.
 */
@Data
@TableName("ce_dimension_record")
public class CeDimensionRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String dimensionCode;

    private String recordCode;

    private String recordName;

    private String parentCode;

    private String sourceType;

    private String field01;

    private String field02;

    private String field03;

    private String field04;

    private String field05;

    private String field06;

    private Integer sortOrder;

    private String status;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
