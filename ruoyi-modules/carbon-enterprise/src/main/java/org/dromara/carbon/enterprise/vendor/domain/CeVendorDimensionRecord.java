package org.dromara.carbon.enterprise.vendor.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Vendor dimension record exposed by vendor open APIs.
 */
@Data
public class CeVendorDimensionRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String dimensionCode;

    private String recordCode;

    private String recordName;

    private String parentCode;

    private String field01;

    private String field02;

    private String field03;

    private String field04;

    private String field05;

    private String field06;

    private String field07;

    private String field08;

    private String field09;

    private String field10;

    private String field11;

    private String field12;

    private String field13;

    private String field14;

    private String field15;

    private String field16;

    private String field17;

    private String field18;

    private String field19;

    private String field20;

    private String field21;

    private String field22;

    private Integer sortOrder;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private String remark;
}
