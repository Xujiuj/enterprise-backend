package org.dromara.carbon.enterprise.domain.sync;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 维度全量同步状态.
 */
@Data
public class CeDimensionSyncStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 许可证ID */
    private String licenseId;

    /** 最近一次全量同步时间 */
    private Date lastSyncTime;

    /** 各维度同步结果 */
    private List<CeDimensionSyncResponse> results = new ArrayList<>();

    /** 成功维度数 */
    private int successCount;

    /** 失败维度数 */
    private int failCount;
}
