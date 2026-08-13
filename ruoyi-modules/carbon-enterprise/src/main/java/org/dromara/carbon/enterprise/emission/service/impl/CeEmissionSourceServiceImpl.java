package org.dromara.carbon.enterprise.emission.service.impl;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.emission.domain.bo.CeEmissionSourceBo;
import org.dromara.carbon.enterprise.emission.domain.vo.CeEmissionSourceVo;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionSourceService;
import org.dromara.carbon.enterprise.shared.support.CeEnterpriseDataScopeSupport;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysDept;
import org.dromara.system.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Enterprise local emission source service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeEmissionSourceServiceImpl implements ICeEmissionSourceService {

    private final CeEmissionSourceMapper emissionSourceMapper;
    private final CeDimensionProjectionMapper dimensionProjectionMapper;
    private final CeEmissionSourceCategoryMapper emissionSourceCategoryMapper;
    private final SysDeptMapper sysDeptMapper;
    private final CeEnterpriseDataScopeSupport dataScopeSupport;

    @Override
    public TableDataInfo<CeEmissionSourceVo> queryPageList(CeEmissionSourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeEmissionSource> wrapper = applyResponsibleDeptScope(buildQueryWrapper(bo))
            .orderByDesc(CeEmissionSource::getSourceIdentificationCode)
            .orderByDesc(CeEmissionSource::getId);
        IPage<CeEmissionSourceVo> page = emissionSourceMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeEmissionSourceVo> queryList(CeEmissionSourceBo bo) {
        return emissionSourceMapper.selectVoList(applyResponsibleDeptScope(buildQueryWrapper(bo))
            .orderByDesc(CeEmissionSource::getSourceIdentificationCode)
            .orderByDesc(CeEmissionSource::getId));
    }

    @Override
    public CeEmissionSourceVo queryById(Long id) {
        CeEmissionSourceVo row = emissionSourceMapper.selectVoById(id);
        return row == null || dataScopeSupport.canAccessDept(row.getResponsibleDept()) ? row : null;
    }

    @Override
    public Boolean insertByBo(CeEmissionSourceBo bo) {
        validateForeignKeys(bo);
        resolveFactorFromEfFactor(bo);
        bo.setSourceIdentificationCode(nextSourceIdentificationCode(bo.getFactoryCode()));
        CeEmissionSource add = toEntity(bo);
        if (add.getEnabledFlag() == null) {
            add.setEnabledFlag(Boolean.TRUE);
        }
        boolean flag = emissionSourceMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeEmissionSourceBo bo) {
        CeEmissionSource existing = emissionSourceMapper.selectById(bo.getId());
        if (existing != null && !dataScopeSupport.canAccessDept(existing.getResponsibleDept())) {
            throw new ServiceException("无权修改其他部门的排放源识别数据");
        }
        validateForeignKeys(bo);
        resolveFactorFromEfFactor(bo);
        if (StringUtils.isBlank(bo.getSourceIdentificationCode())) {
            bo.setSourceIdentificationCode(nextSourceIdentificationCode(bo.getFactoryCode()));
        }
        CeEmissionSource update = toEntity(bo);
        return emissionSourceMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<CeEmissionSource> wrapper = new LambdaQueryWrapper<CeEmissionSource>()
            .in(CeEmissionSource::getId, ids);
        applyResponsibleDeptScope(wrapper);
        return emissionSourceMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(MultipartFile file) {
        validateImportFile(file);
        List<ParsedSheetRow> sheetRows;
        try (InputStream inputStream = file.getInputStream()) {
            sheetRows = readSheetRows(inputStream);
        } catch (IOException e) {
            throw new ServiceException("读取排放源识别 Excel 文件失败");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("解析排放源识别 Excel 文件失败");
        }
        if (sheetRows.isEmpty()) {
            throw new ServiceException("排放源识别 Excel 至少需要一行表头");
        }

        Map<Integer, String> headerBindings = resolveHeaderBindings(sheetRows.get(0).values());
        int imported = 0;
        for (ParsedSheetRow sheetRow : sheetRows) {
            if (sheetRow.rowIndex() == 0) {
                continue;
            }
            CeEmissionSourceBo row = toImportBo(sheetRow.values(), headerBindings);
            if (row == null) {
                continue;
            }
            resolveImportDisplayValues(row);
            insertByBo(row);
            imported++;
        }
        return imported;
    }

    protected CeEmissionSource toEntity(CeEmissionSourceBo bo) {
        return MapstructUtils.convert(bo, CeEmissionSource.class);
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择排放源识别 Excel 文件");
        }
        String fileName = normalized(file.getOriginalFilename());
        if (StringUtils.isBlank(fileName) || !fileName.toLowerCase().endsWith(".xlsx")) {
            throw new ServiceException("排放源识别仅支持上传 .xlsx 文件");
        }
    }

    private List<ParsedSheetRow> readSheetRows(InputStream inputStream) {
        List<ParsedSheetRow> rows = new java.util.ArrayList<>();
        FastExcel.read(inputStream, new EmissionSourceRowListener(rows))
            .autoCloseStream(false)
            .headRowNumber(0)
            .sheet(0)
            .doRead();
        return rows;
    }

    private Map<Integer, String> resolveHeaderBindings(Map<Integer, String> headerRow) {
        Map<Integer, String> bindings = new LinkedHashMap<>();
        if (headerRow == null) {
            return bindings;
        }
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            String prop = headerToProp(entry.getValue());
            if (StringUtils.isNotBlank(prop)) {
                bindings.put(entry.getKey(), prop);
            }
        }
        return bindings;
    }

    private String headerToProp(String header) {
        return switch (normalized(header).replaceFirst("^(FK|PK|SK|BK)_", "")) {
            case "公司", "公司编号" -> "companyCode";
            case "公司名称" -> "companyName";
            case "工厂", "工厂名称" -> "factoryName";
            case "工厂编号" -> "factoryCode";
            case "范围", "核算范围" -> "scopeName";
            case "范围子类别" -> "sourceCategoryKey";
            case "单位" -> "sourceUnit";
            case "排放源识别编号" -> "sourceIdentificationCode";
            case "排放源识别" -> "sourceIdentificationName";
            case "排放源" -> "emissionSourceName";
            case "数据频次" -> "dataFrequency";
            case "负责人" -> "responsibleUserName";
            case "负责人姓名" -> "responsibleUserName";
            case "负责部门" -> "responsibleDept";
            case "数据来源" -> "dataSource";
            case "因子", "适用因子", "排放因子" -> "factorKey";
            case "状态" -> "enabledFlag";
            default -> null;
        };
    }

    private CeEmissionSourceBo toImportBo(Map<Integer, String> values, Map<Integer, String> headerBindings) {
        CeEmissionSourceBo bo = new CeEmissionSourceBo();
        boolean hasValue = false;
        for (Map.Entry<Integer, String> binding : headerBindings.entrySet()) {
            String value = normalized(values == null ? null : values.get(binding.getKey()));
            if (StringUtils.isBlank(value)) {
                continue;
            }
            hasValue = true;
            applyImportValue(bo, binding.getValue(), value);
        }
        return hasValue ? bo : null;
    }

    private void applyImportValue(CeEmissionSourceBo bo, String prop, String value) {
        switch (prop) {
            case "companyCode" -> bo.setCompanyCode(value);
            case "companyName" -> bo.setCompanyName(value);
            case "factoryCode" -> bo.setFactoryCode(value);
            case "factoryName" -> bo.setFactoryName(value);
            case "sourceCategoryKey" -> bo.setSourceCategoryKey(value);
            case "scopeName" -> bo.setScopeName(value);
            case "sourceUnit" -> bo.setSourceUnit(value);
            case "sourceIdentificationCode" -> {
            }
            case "sourceIdentificationName" -> bo.setSourceIdentificationName(value);
            case "emissionSourceName" -> bo.setEmissionSourceName(value);
            case "dataFrequency" -> bo.setDataFrequency(normalizeFrequency(value));
            case "responsibleUserName" -> bo.setResponsibleUserName(value);
            case "responsibleDept" -> bo.setResponsibleDept(value);
            case "dataSource" -> bo.setDataSource(value);
            case "factorKey" -> bo.setFactorKey(extractBusinessCode(value));
            case "enabledFlag" -> bo.setEnabledFlag(normalizeEnabledFlag(value));
            default -> {
            }
        }
    }

    private void resolveImportDisplayValues(CeEmissionSourceBo bo) {
        resolveImportCompany(bo);
        resolveImportFactory(bo);
        resolveImportSourceCategory(bo);
        resolveFactorFromEfFactor(bo);
        if (bo.getEnabledFlag() == null) {
            bo.setEnabledFlag(Boolean.TRUE);
        }
    }

    private void resolveImportCompany(CeEmissionSourceBo bo) {
        String value = normalized(bo.getCompanyCode());
        if (StringUtils.isBlank(value)) {
            return;
        }
        List<SysDept> organization = organizationNodes();
        for (SysDept row : organization) {
            if (isCompanyNode(row, organization) && matchesBusinessLabel(value, row.getDeptCategory(), row.getDeptName())) {
                bo.setCompanyCode(row.getDeptCategory());
                if (StringUtils.isBlank(bo.getCompanyName())) {
                    bo.setCompanyName(row.getDeptName());
                }
                return;
            }
        }
    }

    private void resolveImportFactory(CeEmissionSourceBo bo) {
        String factoryValue = StringUtils.defaultIfBlank(normalized(bo.getFactoryCode()), normalized(bo.getFactoryName()));
        if (StringUtils.isBlank(factoryValue)) {
            return;
        }
        List<SysDept> organization = organizationNodes();
        for (SysDept row : organization) {
            SysDept company = organization.stream().filter(item -> item.getDeptId().equals(row.getParentId())).findFirst().orElse(null);
            if (isFactoryNode(row, company) && (StringUtils.isBlank(bo.getCompanyCode()) || bo.getCompanyCode().equals(company.getDeptCategory()))
                && matchesBusinessLabel(factoryValue, row.getFactoryCode(), row.getDeptName())) {
                bo.setCompanyCode(StringUtils.defaultIfBlank(bo.getCompanyCode(), company.getDeptCategory()));
                bo.setCompanyName(StringUtils.defaultIfBlank(bo.getCompanyName(), company.getDeptName()));
                bo.setFactoryCode(row.getFactoryCode());
                bo.setFactoryName(row.getDeptName());
                return;
            }
        }
    }

    private void resolveImportSourceCategory(CeEmissionSourceBo bo) {
        String value = normalized(bo.getSourceCategoryKey());
        if (StringUtils.isBlank(value)) {
            return;
        }
        List<CeEmissionSourceCategory> categories = emissionSourceCategoryMapper.selectList(new LambdaQueryWrapper<CeEmissionSourceCategory>()
            .select(CeEmissionSourceCategory::getCategorySk, CeEmissionSourceCategory::getGhgScope, CeEmissionSourceCategory::getGhgScopeCategory)
            .isNotNull(CeEmissionSourceCategory::getCategorySk));
        for (CeEmissionSourceCategory category : categories) {
            boolean keyMatches = matchesText(value, category.getCategorySk());
            boolean subcategoryMatches = matchesText(value, category.getGhgScopeCategory());
            boolean scopeMatches = StringUtils.isBlank(bo.getScopeName()) || matchesText(bo.getScopeName(), category.getGhgScope());
            if (keyMatches || (subcategoryMatches && scopeMatches)) {
                bo.setSourceCategoryKey(category.getCategorySk());
                bo.setScopeName(category.getGhgScope());
                bo.setScopeSubcategory(category.getGhgScopeCategory());
                return;
            }
        }
    }

    private String normalizeFrequency(String value) {
        return switch (normalized(value)) {
            case "月报", "monthly", "MONTHLY" -> "monthly";
            case "日报", "daily", "DAILY" -> "daily";
            case "季报", "quarterly", "QUARTERLY" -> "quarterly";
            default -> value;
        };
    }

    private Boolean normalizeEnabledFlag(String value) {
        return switch (normalized(value)) {
            case "是", "启用", "有效", "1", "true", "TRUE" -> Boolean.TRUE;
            case "否", "停用", "无效", "0", "false", "FALSE" -> Boolean.FALSE;
            default -> null;
        };
    }

    private String nextSourceIdentificationCode(String factoryCode) {
        String prefix = normalized(factoryCode);
        if (StringUtils.isBlank(prefix)) {
            throw new ServiceException("宸ュ巶缂栧彿涓嶈兘涓虹┖锛屾棤娉曠敓鎴愭帓鏀炬簮璇嗗埆缂栧彿");
        }
        List<CeEmissionSource> rows = emissionSourceMapper.selectList(new LambdaQueryWrapper<CeEmissionSource>()
            .select(CeEmissionSource::getSourceIdentificationCode)
            .eq(CeEmissionSource::getFactoryCode, prefix)
            .likeRight(CeEmissionSource::getSourceIdentificationCode, prefix));
        int maxSequence = 0;
        int width = 3;
        for (CeEmissionSource row : rows) {
            String code = normalized(row.getSourceIdentificationCode());
            if (!code.startsWith(prefix) || code.length() <= prefix.length()) {
                continue;
            }
            String suffix = code.substring(prefix.length());
            if (!suffix.matches("\\d+")) {
                continue;
            }
            maxSequence = Math.max(maxSequence, Integer.parseInt(suffix));
            width = Math.max(width, suffix.length());
        }
        int nextSequence = maxSequence + 1;
        String candidate;
        do {
            candidate = prefix + String.format("%0" + width + "d", nextSequence++);
        } while (sourceIdentificationCodeExists(candidate));
        return candidate;
    }

    private boolean sourceIdentificationCodeExists(String code) {
        Long count = emissionSourceMapper.selectCount(new LambdaQueryWrapper<CeEmissionSource>()
            .eq(CeEmissionSource::getSourceIdentificationCode, code));
        return count != null && count > 0;
    }

    private boolean matchesBusinessLabel(String value, String code, String name) {
        String raw = normalized(value);
        return matchesText(raw, code)
            || matchesText(raw, name)
            || matchesText(raw, normalized(code) + " " + normalized(name))
            || matchesText(raw, normalized(code) + " - " + normalized(name))
            || matchesText(raw, normalized(name) + " (" + normalized(code) + ")");
    }

    private boolean matchesText(String left, String right) {
        return Objects.equals(normalized(left), normalized(right));
    }

    private String extractBusinessCode(String value) {
        String raw = normalized(value);
        int splitIndex = raw.indexOf(" - ");
        if (splitIndex > 0) {
            return raw.substring(0, splitIndex).trim();
        }
        splitIndex = raw.indexOf(' ');
        if (splitIndex > 0) {
            return raw.substring(0, splitIndex).trim();
        }
        return raw;
    }

    private LambdaQueryWrapper<CeEmissionSource> buildQueryWrapper(CeEmissionSourceBo bo) {
        return new LambdaQueryWrapper<CeEmissionSource>()
            .like(StringUtils.isNotBlank(bo.getCompanyCode()), CeEmissionSource::getCompanyCode, bo.getCompanyCode())
            .like(StringUtils.isNotBlank(bo.getCompanyName()), CeEmissionSource::getCompanyName, bo.getCompanyName())
            .like(StringUtils.isNotBlank(bo.getFactoryName()), CeEmissionSource::getFactoryName, bo.getFactoryName())
            .eq(StringUtils.isNotBlank(bo.getSourceCategoryKey()), CeEmissionSource::getSourceCategoryKey, bo.getSourceCategoryKey())
            .like(StringUtils.isNotBlank(bo.getScopeName()), CeEmissionSource::getScopeName, bo.getScopeName())
            .like(StringUtils.isNotBlank(bo.getScopeSubcategory()), CeEmissionSource::getScopeSubcategory, bo.getScopeSubcategory())
            .like(StringUtils.isNotBlank(bo.getSourceIdentificationCode()), CeEmissionSource::getSourceIdentificationCode, bo.getSourceIdentificationCode())
            .like(StringUtils.isNotBlank(bo.getSourceIdentificationName()), CeEmissionSource::getSourceIdentificationName, bo.getSourceIdentificationName())
            .like(StringUtils.isNotBlank(bo.getEmissionSourceName()), CeEmissionSource::getEmissionSourceName, bo.getEmissionSourceName())
            .like(StringUtils.isNotBlank(bo.getResponsibleDept()), CeEmissionSource::getResponsibleDept, bo.getResponsibleDept())
            .eq(StringUtils.isNotBlank(bo.getDataFrequency()), CeEmissionSource::getDataFrequency, bo.getDataFrequency())
            .eq(bo.getResponsibleUserId() != null, CeEmissionSource::getResponsibleUserId, bo.getResponsibleUserId())
            .like(StringUtils.isNotBlank(bo.getResponsibleUserName()), CeEmissionSource::getResponsibleUserName, bo.getResponsibleUserName())
            .like(StringUtils.isNotBlank(bo.getDataSource()), CeEmissionSource::getDataSource, bo.getDataSource())
            .eq(StringUtils.isNotBlank(bo.getFactorKey()), CeEmissionSource::getFactorKey, bo.getFactorKey())
            .eq(bo.getEnabledFlag() != null, CeEmissionSource::getEnabledFlag, bo.getEnabledFlag());
    }

    private LambdaQueryWrapper<CeEmissionSource> applyResponsibleDeptScope(LambdaQueryWrapper<CeEmissionSource> wrapper) {
        if (dataScopeSupport.unrestricted()) {
            return wrapper;
        }
        List<String> deptNames = dataScopeSupport.allowedDeptNames();
        if (deptNames.isEmpty()) {
            return wrapper.apply("1 = 0");
        }
        return wrapper.in(CeEmissionSource::getResponsibleDept, deptNames);
    }

    private void validateForeignKeys(CeEmissionSourceBo bo) {
        if (StringUtils.isNotBlank(bo.getCompanyCode())) {
            List<SysDept> organization = organizationNodes();
            SysDept company = organization.stream().filter(row -> isCompanyNode(row, organization)
                && bo.getCompanyCode().equals(row.getDeptCategory())).findFirst().orElse(null);
            if (company == null) {
                throw new ServiceException("公司编号不存在：" + bo.getCompanyCode());
            }
            if (StringUtils.isBlank(bo.getCompanyName())) {
                bo.setCompanyName(company.getDeptName());
            }
        }
        if (StringUtils.isNotBlank(bo.getFactoryCode()) || StringUtils.isNotBlank(bo.getFactoryName())) {
            List<SysDept> organization = organizationNodes();
            SysDept factory = organization.stream().filter(row -> {
                SysDept company = organization.stream().filter(item -> item.getDeptId().equals(row.getParentId())).findFirst().orElse(null);
                boolean sameCompany = StringUtils.isBlank(bo.getCompanyCode()) || (company != null && bo.getCompanyCode().equals(company.getDeptCategory()));
                return isFactoryNode(row, company) && sameCompany
                    && (matchesText(bo.getFactoryCode(), row.getFactoryCode()) || matchesText(bo.getFactoryName(), row.getDeptName()));
            }).findFirst().orElse(null);
            if (factory == null) {
                throw new ServiceException("工厂不存在：" + StringUtils.defaultIfBlank(bo.getFactoryCode(), bo.getFactoryName()));
            }
            SysDept company = organization.stream().filter(row -> row.getDeptId().equals(factory.getParentId())).findFirst().orElse(null);
            if (company == null) {
                throw new ServiceException("工厂未归属有效公司：" + factory.getDeptName());
            }
            if (StringUtils.isBlank(bo.getCompanyCode())) {
                bo.setCompanyCode(company.getDeptCategory());
            }
            if (StringUtils.isBlank(bo.getCompanyName())) {
                bo.setCompanyName(company.getDeptName());
            }
            if (StringUtils.isBlank(bo.getFactoryCode())) {
                bo.setFactoryCode(factory.getFactoryCode());
            }
            if (StringUtils.isBlank(bo.getFactoryName())) {
                bo.setFactoryName(factory.getDeptName());
            }
        }
        if (StringUtils.isNotBlank(bo.getSourceCategoryKey())) {
            List<CeEmissionSourceCategory> categories = emissionSourceCategoryMapper.selectList(
                new LambdaQueryWrapper<CeEmissionSourceCategory>()
                    .select(
                        CeEmissionSourceCategory::getCategorySk,
                        CeEmissionSourceCategory::getGhgScope,
                        CeEmissionSourceCategory::getGhgScopeCategory
                    )
                    .eq(CeEmissionSourceCategory::getCategorySk, bo.getSourceCategoryKey()));
            if (categories.isEmpty()) {
                throw new ServiceException("排放源分类不存在：" + bo.getSourceCategoryKey());
            }
            CeEmissionSourceCategory category = categories.get(0);
            if (StringUtils.isBlank(bo.getScopeName())) {
                bo.setScopeName(category.getGhgScope());
            }
            if (StringUtils.isBlank(bo.getScopeSubcategory())) {
                bo.setScopeSubcategory(category.getGhgScopeCategory());
            }
        }
    }

    private void resolveFactorFromEfFactor(CeEmissionSourceBo bo) {
        String factorKey = normalized(bo.getFactorKey());
        String emissionSourceName = normalized(bo.getEmissionSourceName());
        if (StringUtils.isBlank(factorKey) && StringUtils.isBlank(emissionSourceName)) {
            return;
        }
        for (CeDimensionRecordVo factor : dimensionProjectionMapper.selectByDimensionCode("ef-factor")) {
            String recordCode = normalized(factor.getRecordCode());
            String recordName = normalized(factor.getRecordName());
            if (!matchesEfFactor(factorKey, emissionSourceName, recordCode, recordName)) {
                continue;
            }
            if (StringUtils.isBlank(bo.getFactorKey())) {
                bo.setFactorKey(recordCode);
            }
            if (StringUtils.isBlank(bo.getEmissionSourceName())) {
                bo.setEmissionSourceName(recordName);
            }
            if (StringUtils.isBlank(bo.getSourceUnit())) {
                String unit = StringUtils.defaultIfBlank(normalized(factor.getSourceUnit()), normalized(factor.getFactorUnit()));
                if (StringUtils.isNotBlank(unit)) {
                    bo.setSourceUnit(unit);
                }
            }
            return;
        }
        throw new ServiceException("排放源必须来自201排放因子表：" + StringUtils.defaultIfBlank(emissionSourceName, factorKey));
    }

    private boolean matchesEfFactor(String factorKey, String emissionSourceName, String recordCode, String recordName) {
        if (StringUtils.isNotBlank(factorKey) && factorKey.equals(recordCode)) {
            return true;
        }
        return StringUtils.isNotBlank(emissionSourceName) && emissionSourceName.equals(recordName);
    }

    private List<SysDept> organizationNodes() {
        return sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId, SysDept::getParentId, SysDept::getDeptName, SysDept::getDeptCategory,
                SysDept::getFactoryCode, SysDept::getStatus, SysDept::getDelFlag)
            .eq(SysDept::getDelFlag, "0")
            .eq(SysDept::getStatus, "0"));
    }

    private boolean isCompanyNode(SysDept dept, List<SysDept> organization) {
        if (dept == null || StringUtils.isBlank(dept.getDeptCategory())) return false;
        SysDept parent = organization.stream().filter(row -> row.getDeptId().equals(dept.getParentId())).findFirst().orElse(null);
        return parent == null || StringUtils.isBlank(parent.getDeptCategory());
    }

    private boolean isFactoryNode(SysDept dept, SysDept company) {
        return dept != null && company != null
            && StringUtils.equals(dept.getDeptCategory(), company.getDeptCategory())
            && StringUtils.isNotBlank(dept.getFactoryCode());
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private record ParsedSheetRow(int rowIndex, Map<Integer, String> values) {
    }

    private static final class EmissionSourceRowListener extends AnalysisEventListener<Map<Integer, String>> {

        private final List<ParsedSheetRow> rows;

        private EmissionSourceRowListener(List<ParsedSheetRow> rows) {
            this.rows = rows;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            Map<Integer, String> values = data == null ? Map.of() : new LinkedHashMap<>(data);
            rows.add(new ParsedSheetRow(context.readRowHolder().getRowIndex(), values));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
        }
    }
}
