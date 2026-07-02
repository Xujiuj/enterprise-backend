package org.dromara.carbon.enterprise.shared.service;

/**
 * Syncs company factory records into the system department tree.
 */
public interface ICeCompanyFactoryDeptSyncService {

    /**
     * Ensures current company factories have department nodes.
     */
    void syncCompanyFactoriesToSysDept();

    /**
     * Applies a single company/factory edit to its department node, then reconciles current rows.
     */
    void syncCompanyFactoryChange(String previousCompanyCode, String previousFactoryName,
                                  String currentCompanyCode, String currentFactoryName, String activeFlag);

    /**
     * Disables a department node when the corresponding company/factory row is removed.
     */
    void disableCompanyFactoryDept(String companyCode, String factoryName);
}
