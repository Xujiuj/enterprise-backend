package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.sync.CeDimensionSyncResponse;
import org.dromara.carbon.enterprise.domain.sync.CeDimensionSyncStatus;

import java.util.List;

/**
 * 企业端维度同步服务，从厂商端拉取维度数据到本地表.
 */
public interface ICeDimensionSyncService {

    /**
     * 同步全部7个厂商维度到本地表.
     *
     * @return 各维度同步结果列表
     */
    List<CeDimensionSyncResponse> syncAllVendorDimensions();

    /**
     * 同步单个维度到本地表.
     *
     * @param dimensionCode 维度编码
     * @return 同步结果
     */
    CeDimensionSyncResponse syncDimension(String dimensionCode);

    /**
     * 获取最近一次同步状态.
     *
     * @return 同步状态，若未执行过同步则返回null
     */
    CeDimensionSyncStatus getLastSyncStatus();
}
