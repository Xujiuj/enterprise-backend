package org.dromara.carbon.enterprise.domain.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Vendor announcement record returned by vendor open API.
 */
@Data
public class CeVendorAnnouncementRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long noticeId;

    private String noticeTitle;

    private String noticeType;

    private String noticeContent;

    private String status;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
