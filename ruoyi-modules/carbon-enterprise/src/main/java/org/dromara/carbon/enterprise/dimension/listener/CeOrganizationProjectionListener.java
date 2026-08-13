package org.dromara.carbon.enterprise.dimension.listener;

import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.shared.service.ICeCompanyFactoryDeptSyncService;
import org.dromara.system.event.DepartmentTreeChangedEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the legacy company table aligned whenever organization management changes.
 */
@RequiredArgsConstructor
@Component
public class CeOrganizationProjectionListener {

    private final ICeCompanyFactoryDeptSyncService companyFactoryDeptSyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDepartmentTreeChanged(DepartmentTreeChangedEvent ignored) {
        companyFactoryDeptSyncService.syncSysDeptToCompanyFactories();
    }
}
