package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 维度同步结果响应.
 */
@Data
public class CeDimensionSyncResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 许可证ID */
    private String licenseId;

    /** 同步的维度编码 */
    private String dimensionCode;

    /** 同步记录数 */
    private int syncedCount;

    /** 同步时间 */
    private Date syncedTime;

    /** 是否成功 */
    private boolean success;

    /** 错误信息（失败时） */
    private String errorMessage;
}
