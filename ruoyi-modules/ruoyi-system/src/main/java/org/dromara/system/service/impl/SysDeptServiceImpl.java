package org.dromara.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.lang.Validator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.dto.DeptDTO;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.DeptService;
import org.dromara.common.core.utils.*;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysDeptImportVo;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysDeptService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

/**
 * 部门管理 服务实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDeptServiceImpl implements ISysDeptService, DeptService {

    private final SysDeptMapper baseMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;

    /**
     * 分页查询部门管理数据
     *
     * @param dept      部门信息
     * @param pageQuery 分页对象
     * @return 部门信息集合
     */
    @Override
    public TableDataInfo<SysDeptVo> selectPageDeptList(SysDeptBo dept, PageQuery pageQuery) {
        Page<SysDeptVo> page = baseMapper.selectPageDeptList(pageQuery.build(), buildQueryWrapper(dept));
        return TableDataInfo.build(page);
    }

    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    public List<SysDeptVo> selectDeptList(SysDeptBo dept) {
        LambdaQueryWrapper<SysDept> lqw = buildQueryWrapper(dept);
        return baseMapper.selectDeptList(lqw);
    }

    /**
     * 查询部门树结构信息
     *
     * @param bo 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<Tree<Long>> selectDeptTreeList(SysDeptBo bo) {
        LambdaQueryWrapper<SysDept> lqw = buildQueryWrapper(bo);
        List<SysDeptVo> depts = baseMapper.selectDeptList(lqw);
        return buildDeptTreeSelect(depts);
    }

    @Override
    public List<Tree<Long>> selectEnterpriseDeptTreeList(SysDeptBo bo) {
        LambdaQueryWrapper<SysDept> lqw = buildQueryWrapper(bo);
        lqw.isNotNull(SysDept::getDeptCategory);
        lqw.ne(SysDept::getDeptCategory, StringUtils.EMPTY);
        List<SysDeptVo> depts = baseMapper.selectDeptList(lqw);
        return buildDeptTreeSelect(depts);
    }

    private LambdaQueryWrapper<SysDept> buildQueryWrapper(SysDeptBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysDept> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysDept::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getDeptId()), SysDept::getDeptId, bo.getDeptId());
        lqw.eq(ObjectUtil.isNotNull(bo.getParentId()), SysDept::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getDeptName()), SysDept::getDeptName, bo.getDeptName());
        lqw.eq(StringUtils.isNotBlank(bo.getDeptCategory()), SysDept::getDeptCategory, bo.getDeptCategory());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysDept::getStatus, bo.getStatus());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            SysDept::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByAsc(SysDept::getAncestors);
        lqw.orderByAsc(SysDept::getParentId);
        lqw.orderByAsc(SysDept::getOrderNum);
        lqw.orderByAsc(SysDept::getDeptId);
        if (ObjectUtil.isNotNull(bo.getBelongDeptId())) {
            //部门树搜索
            lqw.and(x -> {
                List<Long> deptIds = baseMapper.selectDeptAndChildById(bo.getBelongDeptId());
                x.in(SysDept::getDeptId, deptIds);
            });
        }
        return lqw;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<Tree<Long>> buildDeptTreeSelect(List<SysDeptVo> depts) {
        if (CollUtil.isEmpty(depts)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.buildMultiRoot(
            depts,
            SysDeptVo::getDeptId,
            SysDeptVo::getParentId,
            (node, treeNode) -> treeNode
                .setId(node.getDeptId())
                .setParentId(node.getParentId())
                .setName(node.getDeptName())
                .setWeight(node.getOrderNum())
                .putExtra("disabled", SystemConstants.DISABLE.equals(node.getStatus()))
        );
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        return baseMapper.selectDeptListByRoleId(roleId, role.getDeptCheckStrictly());
    }

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Cacheable(cacheNames = CacheNames.SYS_DEPT, key = "#deptId")
    @Override
    public SysDeptVo selectDeptById(Long deptId) {
        SysDeptVo dept = baseMapper.selectVoById(deptId);
        if (ObjectUtil.isNull(dept)) {
            return null;
        }
        SysDeptVo parentDept = baseMapper.selectVoOne(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptName).eq(SysDept::getDeptId, dept.getParentId()));
        dept.setParentName(ObjectUtils.notNullGetter(parentDept, SysDeptVo::getDeptName));
        return dept;
    }

    @Override
    public List<SysDeptVo> selectDeptByIds(List<Long> deptIds) {
        return baseMapper.selectDeptList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId, SysDept::getDeptName, SysDept::getLeader)
            .eq(SysDept::getStatus, SystemConstants.NORMAL)
            .in(CollUtil.isNotEmpty(deptIds), SysDept::getDeptId, deptIds));
    }

    /**
     * 通过部门ID查询部门名称
     *
     * @param deptIds 部门ID串逗号分隔
     * @return 部门名称串逗号分隔
     */
    @Override
    public String selectDeptNameByIds(String deptIds) {
        List<String> list = new ArrayList<>();
        for (Long id : StringUtils.splitTo(deptIds, Convert::toLong)) {
            SysDeptVo vo = SpringUtils.getAopProxy(this).selectDeptById(id);
            if (ObjectUtil.isNotNull(vo)) {
                list.add(vo.getDeptName());
            }
        }
        return StringUtils.joinComma(list);
    }

    /**
     * 根据部门ID查询部门负责人
     *
     * @param deptId 部门ID，用于指定需要查询的部门
     * @return 返回该部门的负责人ID
     */
    @Override
    public Long selectDeptLeaderById(Long deptId) {
        SysDeptVo vo = SpringUtils.getAopProxy(this).selectDeptById(deptId);
        return vo.getLeader();
    }

    /**
     * 查询部门
     *
     * @return 部门列表
     */
    @Override
    public List<DeptDTO> selectDeptsByList() {
        List<SysDeptVo> list = baseMapper.selectDeptList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId, SysDept::getDeptName, SysDept::getParentId)
            .eq(SysDept::getStatus, SystemConstants.NORMAL));
        return BeanUtil.copyToList(list, DeptDTO.class);
    }

    /**
     * 根据ID查询所有子部门数（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public long selectNormalChildrenDeptById(Long deptId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getStatus, SystemConstants.NORMAL)
            .apply(DataBaseHelper.findInSet(deptId, "ancestors")));
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId) {
        return baseMapper.exists(new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getParentId, deptId));
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId) {
        return userMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getDeptId, deptId));
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDeptBo dept) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getDeptName, dept.getDeptName())
            .eq(SysDept::getParentId, dept.getParentId())
            .eq(StringUtils.isNotBlank(dept.getDeptCategory()), SysDept::getDeptCategory, dept.getDeptCategory())
            .ne(ObjectUtil.isNotNull(dept.getDeptId()), SysDept::getDeptId, dept.getDeptId()));
        return !exist;
    }

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId) {
        if (ObjectUtil.isNull(deptId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (baseMapper.countDeptById(deptId) == 0) {
            throw new ServiceException("没有权限访问部门数据！");
        }
    }

    /**
     * 新增保存部门信息
     *
     * @param bo 部门信息
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_DEPT_AND_CHILD, allEntries = true)
    @Override
    public int insertDept(SysDeptBo bo) {
        return baseMapper.insert(prepareDeptForInsert(bo));
    }

    @CacheEvict(cacheNames = CacheNames.SYS_DEPT_AND_CHILD, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importDeptList(List<SysDeptImportVo> rows) {
        List<SysDeptImportVo> sourceRows = rows == null ? List.of() : rows.stream()
            .filter(this::hasImportContent)
            .toList();
        if (sourceRows.isEmpty()) {
            throw new ServiceException("导入文件没有可新增的部门数据");
        }
        if (sourceRows.size() > 2000) {
            throw new ServiceException("单次最多导入2000个部门");
        }

        List<SysDept> organization = new ArrayList<>(baseMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .eq(SysDept::getDelFlag, SystemConstants.NORMAL)));
        List<DeptImportRow> pending = new ArrayList<>();
        for (int index = 0; index < sourceRows.size(); index++) {
            pending.add(new DeptImportRow(index + 2, sourceRows.get(index)));
        }

        int imported = 0;
        while (!pending.isEmpty()) {
            int before = pending.size();
            Iterator<DeptImportRow> iterator = pending.iterator();
            while (iterator.hasNext()) {
                DeptImportRow pendingRow = iterator.next();
                SysDeptImportVo row = pendingRow.row();
                validateImportRow(pendingRow.excelRow(), row);
                Map<Long, SysDept> byId = StreamUtils.toMap(organization, SysDept::getDeptId, item -> item);
                SysDept company = resolveCompany(pendingRow.excelRow(), row.getCompany(), organization, byId);
                SysDept factory = resolveFactory(pendingRow.excelRow(), row.getFactory(), company, organization);
                SysDept parent = resolveImportParent(row.getParentDepartment(), factory, organization);
                if (parent == null) {
                    continue;
                }
                if (departmentExists(parent.getDeptId(), company.getDeptCategory(), row.getDeptName(), organization)) {
                    throw importError(pendingRow.excelRow(), "同一上级下已存在部门“" + row.getDeptName().trim() + "”");
                }

                SysDeptBo bo = new SysDeptBo();
                bo.setParentId(parent.getDeptId());
                bo.setDeptName(row.getDeptName().trim());
                bo.setDeptCategory(company.getDeptCategory());
                bo.setOrderNum(row.getOrderNum() == null ? 0 : row.getOrderNum());
                bo.setLeader(resolveLeaderId(pendingRow.excelRow(), row.getLeaderUserName()));
                bo.setPhone(StringUtils.trimToNull(row.getPhone()));
                bo.setEmail(StringUtils.trimToNull(row.getEmail()));
                bo.setStatus(normalizeImportStatus(pendingRow.excelRow(), row.getStatus()));

                SysDept inserted = prepareDeptForInsert(bo);
                baseMapper.insert(inserted);
                organization.add(inserted);
                imported++;
                iterator.remove();
            }
            if (pending.size() == before) {
                DeptImportRow unresolved = pending.get(0);
                throw importError(unresolved.excelRow(), "找不到上级部门“" + unresolved.row().getParentDepartment() +
                    "”，请使用工厂下的部门名称或“一级部门/二级部门”路径");
            }
        }
        return imported;
    }

    private SysDept prepareDeptForInsert(SysDeptBo bo) {
        SysDept info = baseMapper.selectById(bo.getParentId());
        if (ObjectUtil.isNull(info)) {
            throw new ServiceException("请选择有效的上级部门");
        }
        // 如果父节点不为正常状态,则不允许新增子节点
        if (!SystemConstants.NORMAL.equals(info.getStatus())) {
            throw new ServiceException("部门停用，不允许新增");
        }
        SysDept dept = MapstructUtils.convert(bo, SysDept.class);
        validateSameCompanyParent(dept, info);
        validateNotCompanyDirectParent(info);
        dept.setAncestors(info.getAncestors() + StringUtils.SEPARATOR + dept.getParentId());
        return dept;
    }

    private boolean hasImportContent(SysDeptImportVo row) {
        return row != null && Stream.of(row.getCompany(), row.getFactory(), row.getParentDepartment(), row.getDeptName(),
                row.getLeaderUserName(), row.getPhone(), row.getEmail(), row.getStatus())
            .anyMatch(StringUtils::isNotBlank);
    }

    private void validateImportRow(int excelRow, SysDeptImportVo row) {
        if (StringUtils.isBlank(row.getCompany())) {
            throw importError(excelRow, "所属公司不能为空");
        }
        if (StringUtils.isBlank(row.getFactory())) {
            throw importError(excelRow, "所属工厂不能为空");
        }
        if (StringUtils.isBlank(row.getDeptName())) {
            throw importError(excelRow, "部门名称不能为空");
        }
        if (row.getDeptName().trim().length() > 30) {
            throw importError(excelRow, "部门名称长度不能超过30个字符");
        }
        if (row.getOrderNum() != null && row.getOrderNum() < 0) {
            throw importError(excelRow, "显示排序不能小于0");
        }
        String phone = StringUtils.trimToEmpty(row.getPhone());
        if (phone.length() > 11) {
            throw importError(excelRow, "联系电话长度不能超过11个字符");
        }
        String email = StringUtils.trimToEmpty(row.getEmail());
        if (StringUtils.isNotBlank(email) && (email.length() > 50 || !Validator.isEmail(email))) {
            throw importError(excelRow, "邮箱格式不正确");
        }
    }

    private SysDept resolveCompany(int excelRow, String companyValue, List<SysDept> organization,
                                   Map<Long, SysDept> byId) {
        String value = companyValue.trim();
        List<SysDept> matches = organization.stream()
            .filter(item -> isCompanyNode(item, byId))
            .filter(item -> value.equalsIgnoreCase(item.getDeptCategory()) || value.equalsIgnoreCase(item.getDeptName()))
            .toList();
        if (matches.size() != 1) {
            throw importError(excelRow, matches.isEmpty() ? "找不到所属公司“" + value + "”" : "所属公司“" + value + "”不唯一，请填写公司编号");
        }
        return matches.get(0);
    }

    private SysDept resolveFactory(int excelRow, String factoryValue, SysDept company, List<SysDept> organization) {
        String value = factoryValue.trim();
        List<SysDept> matches = organization.stream()
            .filter(item -> company.getDeptId().equals(item.getParentId()))
            .filter(item -> StringUtils.equals(company.getDeptCategory(), item.getDeptCategory()))
            .filter(item -> value.equalsIgnoreCase(item.getDeptName()) || value.equals(String.valueOf(item.getDeptId())))
            .toList();
        if (matches.size() != 1) {
            throw importError(excelRow, matches.isEmpty()
                ? "公司“" + company.getDeptName() + "”下找不到工厂“" + value + "”"
                : "工厂“" + value + "”不唯一");
        }
        return matches.get(0);
    }

    private SysDept resolveImportParent(String parentPath, SysDept factory, List<SysDept> organization) {
        if (StringUtils.isBlank(parentPath)) {
            return factory;
        }
        SysDept current = factory;
        String[] segments = parentPath.trim().split("[/\\\\>]+");
        for (String segment : segments) {
            String name = segment.trim();
            if (StringUtils.isBlank(name)) {
                continue;
            }
            SysDept parent = current;
            List<SysDept> matches = organization.stream()
                .filter(item -> parent.getDeptId().equals(item.getParentId()))
                .filter(item -> name.equalsIgnoreCase(item.getDeptName()))
                .toList();
            if (matches.size() != 1) {
                return null;
            }
            current = matches.get(0);
        }
        return current;
    }

    private boolean departmentExists(Long parentId, String companyCode, String deptName, List<SysDept> organization) {
        String normalizedName = deptName.trim();
        return organization.stream().anyMatch(item -> parentId.equals(item.getParentId())
            && StringUtils.equals(companyCode, item.getDeptCategory())
            && normalizedName.equalsIgnoreCase(item.getDeptName()));
    }

    private boolean isCompanyNode(SysDept dept, Map<Long, SysDept> byId) {
        if (StringUtils.isBlank(dept.getDeptCategory())) {
            return false;
        }
        SysDept parent = byId.get(dept.getParentId());
        return parent == null || StringUtils.isBlank(parent.getDeptCategory());
    }

    private Long resolveLeaderId(int excelRow, String userName) {
        if (StringUtils.isBlank(userName)) {
            return null;
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUserName, userName.trim())
            .eq(SysUser::getDelFlag, SystemConstants.NORMAL));
        if (users.size() != 1) {
            throw importError(excelRow, "找不到唯一的负责人账号“" + userName.trim() + "”");
        }
        return users.get(0).getUserId();
    }

    private String normalizeImportStatus(int excelRow, String status) {
        if (StringUtils.isBlank(status) || Set.of("正常", "启用", "0").contains(status.trim())) {
            return SystemConstants.NORMAL;
        }
        if (Set.of("停用", "禁用", "1").contains(status.trim())) {
            return SystemConstants.DISABLE;
        }
        throw importError(excelRow, "状态只能填写正常或停用");
    }

    private ServiceException importError(int excelRow, String message) {
        return new ServiceException("Excel第" + excelRow + "行：" + message);
    }

    private record DeptImportRow(int excelRow, SysDeptImportVo row) {
    }

    /**
     * 修改保存部门信息
     *
     * @param bo 部门信息
     * @return 结果
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.SYS_DEPT, key = "#bo.deptId"),
        @CacheEvict(cacheNames = CacheNames.SYS_DEPT_AND_CHILD, allEntries = true)
    })
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDept(SysDeptBo bo) {
        SysDept dept = MapstructUtils.convert(bo, SysDept.class);
        SysDept oldDept = baseMapper.selectById(dept.getDeptId());
        if (ObjectUtil.isNull(oldDept)) {
            throw new ServiceException("部门不存在，无法修改");
        }
        SysDept parentDept = baseMapper.selectById(dept.getParentId());
        validateSameCompanyParent(dept, parentDept);
        if (!oldDept.getParentId().equals(dept.getParentId())) {
            validateNotCompanyDirectParent(parentDept);
            // 如果是新父部门 则校验是否具有新父部门权限 避免越权
            this.checkDeptDataScope(dept.getParentId());
            SysDept newParentDept = parentDept;
            if (ObjectUtil.isNotNull(newParentDept)) {
                String newAncestors = newParentDept.getAncestors() + StringUtils.SEPARATOR + newParentDept.getDeptId();
                String oldAncestors = oldDept.getAncestors();
                dept.setAncestors(newAncestors);
                updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
            }
        } else {
            dept.setAncestors(oldDept.getAncestors());
        }
        int result = baseMapper.updateById(dept);
        // 如果部门状态为启用，且部门祖级列表不为空，且部门祖级列表不等于根部门祖级列表（如果部门祖级列表不等于根部门祖级列表，则说明存在上级部门）
        if (SystemConstants.NORMAL.equals(dept.getStatus())
            && StringUtils.isNotEmpty(dept.getAncestors())
            && !StringUtils.equals(SystemConstants.ROOT_DEPT_ANCESTORS, dept.getAncestors())) {
            // 如果该部门是启用状态，则启用该部门的所有上级部门
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    private void validateSameCompanyParent(SysDept dept, SysDept parentDept) {
        if (ObjectUtil.isNull(parentDept)) {
            return;
        }
        if (StringUtils.isNotBlank(parentDept.getDeptCategory())
            && StringUtils.isNotBlank(dept.getDeptCategory())
            && !StringUtils.equals(parentDept.getDeptCategory(), dept.getDeptCategory())) {
            throw new ServiceException("上级部门必须属于同一部门类别");
        }
    }

    private void validateNotCompanyDirectParent(SysDept parentDept) {
        if (ObjectUtil.isNull(parentDept) || StringUtils.isBlank(parentDept.getDeptCategory())) {
            return;
        }
        SysDept grandParentDept = baseMapper.selectById(parentDept.getParentId());
        if (ObjectUtil.isNull(grandParentDept) || StringUtils.isBlank(grandParentDept.getDeptCategory())) {
            throw new ServiceException("部门必须归属于工厂，不能直接挂在公司下");
        }
    }

    /**
     * 修改该部门的父级部门状态
     *
     * @param dept 当前部门
     */
    private void updateParentDeptStatusNormal(SysDept dept) {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        baseMapper.update(null, new LambdaUpdateWrapper<SysDept>()
            .set(SysDept::getStatus, SystemConstants.NORMAL)
            .in(SysDept::getDeptId, Arrays.asList(deptIds)));
    }

    /**
     * 修改子元素关系
     *
     * @param deptId       被修改的部门ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    private void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors) {
        List<SysDept> children = baseMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .apply(DataBaseHelper.findInSet(deptId, "ancestors")));
        List<SysDept> list = new ArrayList<>();
        for (SysDept child : children) {
            SysDept dept = new SysDept();
            dept.setDeptId(child.getDeptId());
            dept.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            list.add(dept);
        }
        if (CollUtil.isNotEmpty(list)) {
            if (baseMapper.updateBatchById(list)) {
                list.forEach(dept -> CacheUtils.evict(CacheNames.SYS_DEPT, dept.getDeptId()));
            }
        }
    }

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.SYS_DEPT, key = "#deptId"),
        @CacheEvict(cacheNames = CacheNames.SYS_DEPT_AND_CHILD, key = "#deptId")
    })
    @Override
    public int deleteDeptById(Long deptId) {
        return baseMapper.deleteById(deptId);
    }


    /**
     * 根据部门 ID 列表查询部门名称映射关系
     *
     * @param deptIds 部门 ID 列表
     * @return Map，其中 key 为部门 ID，value 为对应的部门名称
     */
    @Override
    public Map<Long, String> selectDeptNamesByIds(List<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyMap();
        }
        List<SysDept> list = baseMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .select(SysDept::getDeptId, SysDept::getDeptName)
                .in(SysDept::getDeptId, deptIds)
        );
        return StreamUtils.toMap(list, SysDept::getDeptId, SysDept::getDeptName);
    }

}
