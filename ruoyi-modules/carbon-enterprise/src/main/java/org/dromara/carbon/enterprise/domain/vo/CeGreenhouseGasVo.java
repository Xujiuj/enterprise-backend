package org.dromara.carbon.enterprise.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeGreenhouseGas;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 温室气体视图对象.
 */
@Data
@AutoMapper(target = CeGreenhouseGas.class)
public class CeGreenhouseGasVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String gasCode;

    private String gasName;

    private String gasNameEn;

    private Date createTime;

    private Date updateTime;

    private String remark;
}
