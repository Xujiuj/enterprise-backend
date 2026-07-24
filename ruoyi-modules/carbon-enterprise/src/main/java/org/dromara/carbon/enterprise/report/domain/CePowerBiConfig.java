package org.dromara.carbon.enterprise.report.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise Power BI embed configuration.
 */
@Data
public class CePowerBiConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Power BI嵌入链接不能为空")
    @Size(max = 8000, message = "Power BI嵌入链接长度不能超过8000个字符")
    private String embedUrl;

    private Long updateBy;

    private Date updateTime;
}
