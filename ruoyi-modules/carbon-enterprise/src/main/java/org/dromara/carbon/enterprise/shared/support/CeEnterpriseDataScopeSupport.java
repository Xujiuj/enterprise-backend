package org.dromara.carbon.enterprise.shared.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysDept;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.service.ISysDataScopeService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies enterprise business data scope to tables that store department names.
 */
@Component
@RequiredArgsConstructor
public class CeEnterpriseDataScopeSupport {

    private static final String DATA_SCOPE_ALL = "1";
    private static final String DATA_SCOPE_CUSTOM = "2";
    private static final String DATA_SCOPE_DEPT = "3";
    private static final String DATA_SCOPE_DEPT_AND_CHILD = "4";
    private static final String DATA_SCOPE_SELF = "5";
    private static final String DATA_SCOPE_DEPT_AND_CHILD_OR_SELF = "6";

    private final ISysDataScopeService dataScopeService;
    private final SysDeptMapper sysDeptMapper;

    public boolean unrestricted() {
        LoginUser user = LoginHelper.getLoginUser();
        if (user == null) {
            return true;
        }
        if (LoginHelper.isSuperAdmin(user.getUserId()) || LoginHelper.isTenantAdmin(user.getRolePermission())) {
            return true;
        }
        return user.getRoles() != null && user.getRoles().stream()
            .anyMatch(role -> DATA_SCOPE_ALL.equals(role.getDataScope()));
    }

    public List<String> allowedDeptNames() {
        if (unrestricted()) {
            return List.of();
        }
        LoginUser user = LoginHelper.getLoginUser();
        if (user == null || user.getDeptId() == null) {
            return List.of();
        }
        Set<Long> deptIds = resolveDeptIds(user);
        if (deptIds.isEmpty()) {
            return List.of();
        }
        return sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .select(SysDept::getDeptName)
                .eq(SysDept::getDelFlag, "0")
                .in(SysDept::getDeptId, deptIds))
            .stream()
            .map(SysDept::getDeptName)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();
    }

    public boolean canAccessDept(String responsibleDept) {
        if (unrestricted()) {
            return true;
        }
        String dept = responsibleDept == null ? "" : responsibleDept.trim();
        if (StringUtils.isBlank(dept)) {
            return false;
        }
        return allowedDeptNames().contains(dept);
    }

    private Set<Long> resolveDeptIds(LoginUser user) {
        Set<Long> deptIds = new LinkedHashSet<>();
        List<RoleDTO> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            deptIds.add(user.getDeptId());
            return deptIds;
        }
        for (RoleDTO role : roles) {
            String dataScope = role.getDataScope();
            if (DATA_SCOPE_CUSTOM.equals(dataScope)) {
                deptIds.addAll(parseDeptIds(dataScopeService.getRoleCustom(role.getRoleId())));
            } else if (DATA_SCOPE_DEPT.equals(dataScope) || DATA_SCOPE_SELF.equals(dataScope)) {
                deptIds.add(user.getDeptId());
            } else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope) || DATA_SCOPE_DEPT_AND_CHILD_OR_SELF.equals(dataScope)) {
                deptIds.addAll(parseDeptIds(dataScopeService.getDeptAndChild(user.getDeptId())));
            }
        }
        if (deptIds.isEmpty()) {
            deptIds.add(user.getDeptId());
        }
        return deptIds;
    }

    private Set<Long> parseDeptIds(String deptIds) {
        if (StringUtils.isBlank(deptIds) || "-1".equals(deptIds.trim())) {
            return Set.of();
        }
        return Arrays.stream(deptIds.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(this::toLong)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Long toLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
