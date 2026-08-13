package org.dromara.carbon.enterprise.shared.service;

/**
 * Projects the system department tree into the legacy company/factory table.
 */
public interface ICeCompanyFactoryDeptSyncService {

    /**
     * Rebuilds the compatibility projection from company and factory nodes.
     */
    void syncSysDeptToCompanyFactories();
}
