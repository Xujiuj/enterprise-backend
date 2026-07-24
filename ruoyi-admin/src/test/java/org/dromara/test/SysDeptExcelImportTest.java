package org.dromara.test;

import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.vo.SysDeptImportVo;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.impl.SysDeptServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class SysDeptExcelImportTest {

    @Test
    void importsDepartmentBelowFactory() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDept root = dept(100L, 0L, "组织", null, "0");
        SysDept company = dept(101L, 100L, "风行智成", "C001", "0,100");
        SysDept factory = dept(102L, 101L, "一号工厂", "C001", "0,100,101");
        when(deptMapper.selectList(any())).thenReturn(List.of(root, company, factory));
        when(deptMapper.selectById(102L)).thenReturn(factory);
        when(deptMapper.selectById(101L)).thenReturn(company);
        when(deptMapper.insert(any(SysDept.class))).thenReturn(1);

        Converter converter = mock(Converter.class);
        when(converter.convert(any(SysDeptBo.class), eq(SysDept.class))).thenAnswer(invocation -> {
            SysDeptBo source = invocation.getArgument(0);
            SysDept target = new SysDept();
            target.setParentId(source.getParentId());
            target.setDeptName(source.getDeptName());
            target.setDeptCategory(source.getDeptCategory());
            target.setOrderNum(source.getOrderNum());
            target.setLeader(source.getLeader());
            target.setPhone(source.getPhone());
            target.setEmail(source.getEmail());
            target.setStatus(source.getStatus());
            return target;
        });

        int imported;
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(Converter.class)).thenReturn(converter);
            imported = service(deptMapper).importDeptList(List.of(importRow("C001", "一号工厂", "生产部")));
        }

        assertEquals(1, imported);
        ArgumentCaptor<SysDept> inserted = ArgumentCaptor.forClass(SysDept.class);
        verify(deptMapper).insert(inserted.capture());
        assertEquals(102L, inserted.getValue().getParentId());
        assertEquals("C001", inserted.getValue().getDeptCategory());
        assertEquals("生产部", inserted.getValue().getDeptName());
        assertEquals("0,100,101,102", inserted.getValue().getAncestors());
    }

    @Test
    void rejectsFactoryFromAnotherCompany() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDept root = dept(100L, 0L, "组织", null, "0");
        SysDept companyA = dept(101L, 100L, "甲公司", "C001", "0,100");
        SysDept companyB = dept(201L, 100L, "乙公司", "C002", "0,100");
        SysDept factoryB = dept(202L, 201L, "二号工厂", "C002", "0,100,201");
        when(deptMapper.selectList(any())).thenReturn(List.of(root, companyA, companyB, factoryB));

        assertThrows(ServiceException.class, () -> service(deptMapper)
            .importDeptList(List.of(importRow("C001", "二号工厂", "生产部"))));
    }

    private SysDeptServiceImpl service(SysDeptMapper deptMapper) {
        return new SysDeptServiceImpl(deptMapper, mock(SysRoleMapper.class), mock(SysUserMapper.class));
    }

    private SysDeptImportVo importRow(String company, String factory, String departmentName) {
        SysDeptImportVo row = new SysDeptImportVo();
        row.setCompany(company);
        row.setFactory(factory);
        row.setDeptName(departmentName);
        row.setStatus("0");
        return row;
    }

    private SysDept dept(Long id, Long parentId, String name, String companyCode, String ancestors) {
        SysDept dept = new SysDept();
        dept.setDeptId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setDeptCategory(companyCode);
        dept.setAncestors(ancestors);
        dept.setStatus(SystemConstants.NORMAL);
        dept.setDelFlag(SystemConstants.NORMAL);
        return dept;
    }
}
