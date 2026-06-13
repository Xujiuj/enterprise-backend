package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeDimensionRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise dimension record view object.
 */
@Data
@AutoMapper(target = CeDimensionRecord.class)
public class CeDimensionRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
