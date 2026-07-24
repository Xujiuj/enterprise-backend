package org.dromara.test;

import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantAdminRoleTest {

    @Tag("local")
    @Tag("dev")
    @Tag("prod")
    @Test
    void recognizesCarbonPlatformTenantAdminAliases() {
        assertTrue(LoginHelper.isTenantAdmin(Set.of(TenantConstants.ENTERPRISE_ADMIN_ROLE_KEY)));
        assertTrue(LoginHelper.isTenantAdmin(Set.of(TenantConstants.VENDOR_ADMIN_ROLE_KEY)));
        assertTrue(LoginHelper.isTenantAdmin(Set.of(TenantConstants.TENANT_ADMIN_ROLE_KEY)));
        assertFalse(LoginHelper.isTenantAdmin(Set.of("enterprise_operator")));
    }
}
