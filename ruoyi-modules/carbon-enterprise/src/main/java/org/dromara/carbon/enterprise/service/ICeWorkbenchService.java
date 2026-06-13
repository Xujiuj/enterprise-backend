package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.vo.CeWorkbenchOverviewVo;

/**
 * Enterprise workbench service.
 */
public interface ICeWorkbenchService {

    /**
     * Build the enterprise home overview from real backend data.
     *
     * @return workbench overview
     */
    CeWorkbenchOverviewVo overview();
}
