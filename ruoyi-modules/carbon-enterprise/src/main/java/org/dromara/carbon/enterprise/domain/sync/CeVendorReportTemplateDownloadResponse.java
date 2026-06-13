package org.dromara.carbon.enterprise.domain.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Vendor report template download metadata response.
 */
@Data
public class CeVendorReportTemplateDownloadResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String licenseId;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private String templateVersion;

    private String fileName;

    private String fileUri;

    private String downloadToken;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date downloadTokenExpiresTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishedTime;
}
