package org.dromara.carbon.enterprise.domain.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Vendor report template metadata visible in list responses.
 */
@Data
public class CeVendorReportTemplateRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private String templateVersion;

    private String fileName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishedTime;
}
