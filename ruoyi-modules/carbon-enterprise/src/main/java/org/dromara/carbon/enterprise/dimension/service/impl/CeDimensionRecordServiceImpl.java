package org.dromara.carbon.enterprise.dimension.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.vendor.client.CeVendorDimensionOpenClient;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionListResponse;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionRecord;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.shared.service.ICeDimensionRecordService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Vendor-open dimension proxy.
 *
 * <p>The former local ce_dimension_record table was removed when enterprise
 * sample-aligned tables were split into concrete business tables.</p>
 */
@RequiredArgsConstructor
@Service
public class CeDimensionRecordServiceImpl implements ICeDimensionRecordService {

    private static final String LICENSE_STATUS_VALID = "VALID";

    private static final Set<String> ALLOWED_VENDOR_DIMENSION_CODES = Set.of(
        "admin-division",
        "emission-source-category",
        "base-year",
        "ef-factor",
        "ef-electricity-factor",
        "ef-electricity-version",
        "ef-electricity-scope",
        "greenhouse-gas"
    );

    private static final Set<String> ALLOWED_LOCAL_PROJECTION_CODES = Set.of(
        "admin-division",
        "company",
        "emission-source-category",
        "base-year",
        "ef-factor",
        "ef-electricity-factor",
        "ef-electricity-version",
        "ef-electricity-scope",
        "greenhouse-gas",
        "intensity-denominator",
        "intensity-target",
        "denominator-fact",
        "intensity-tolerance"
    );

    private static final Set<String> ENTERPRISE_EDITABLE_DIMENSION_CODES = Set.of(
        "company",
        "base-year",
        "ef-factor",
        "ef-electricity-version",
        "intensity-denominator",
        "intensity-target",
        "denominator-fact",
        "intensity-tolerance"
    );

    private final CeDimensionProjectionMapper dimensionProjectionMapper;
    private final CeLicenseStateMapper licenseStateMapper;
    private final CeVendorDimensionOpenClient vendorDimensionOpenClient;

    @Override
    public TableDataInfo<CeDimensionRecordVo> queryPageList(CeDimensionRecordBo bo, PageQuery pageQuery) {
        validateDimensionCode(bo.getDimensionCode());
        return queryLocalProjectionPageList(bo, pageQuery);
    }

    @Override
    public List<CeDimensionRecordVo> queryList(CeDimensionRecordBo bo) {
        validateDimensionCode(bo.getDimensionCode());
        return filterProjectionRows(bo, dimensionProjectionMapper.selectByDimensionCode(bo.getDimensionCode()));
    }

    @Override
    public CeDimensionRecordVo queryById(String dimensionCode, Long id) {
        validateDimensionCode(dimensionCode);
        CeDimensionRecordVo record = dimensionProjectionMapper.selectByDimensionCodeAndId(dimensionCode, id);
        if (record == null) {
            throw new ServiceException("维度记录不存在：" + dimensionCode + "/" + id);
        }
        return record;
    }

    @Override
    public Boolean insertByBo(CeDimensionRecordBo bo) {
        validateEditableDimensionCode(bo.getDimensionCode());
        normalizeCompanyRecord(bo);
        return dimensionProjectionMapper.insertByDimensionCode(bo) > 0;
    }

    @Override
    public Boolean updateByBo(CeDimensionRecordBo bo) {
        validateEditableDimensionCode(bo.getDimensionCode());
        normalizeCompanyRecord(bo);
        return dimensionProjectionMapper.updateByDimensionCode(bo) > 0;
    }

    @Override
    public Boolean deleteByIds(String dimensionCode, Collection<Long> ids) {
        validateEditableDimensionCode(dimensionCode);
        boolean changed = false;
        for (Long id : ids) {
            changed = dimensionProjectionMapper.deleteByDimensionCodeAndId(dimensionCode, id) > 0 || changed;
        }
        return changed;
    }

    private void validateDimensionCode(String dimensionCode) {
        if (StringUtils.isBlank(dimensionCode)) {
            throw new ServiceException("维度编码不能为空");
        }
        if (!ALLOWED_LOCAL_PROJECTION_CODES.contains(dimensionCode)) {
            throw new ServiceException("该功能已迁移到企业端业务表，请从对应业务页面打开：" + dimensionCode);
        }
    }

    private void validateEditableDimensionCode(String dimensionCode) {
        validateDimensionCode(dimensionCode);
        if (!ENTERPRISE_EDITABLE_DIMENSION_CODES.contains(dimensionCode)) {
            throw new ServiceException("当前维度不允许企业端编辑：" + dimensionCode);
        }
    }

    private TableDataInfo<CeDimensionRecordVo> queryLocalProjectionPageList(CeDimensionRecordBo bo, PageQuery pageQuery) {
        PageQuery effectivePageQuery = pageQuery == null ? new PageQuery(PageQuery.DEFAULT_PAGE_SIZE, PageQuery.DEFAULT_PAGE_NUM) : pageQuery;
        IPage<CeDimensionRecordVo> page = dimensionProjectionMapper.selectPageByDimensionCode(effectivePageQuery.build(), bo);
        return TableDataInfo.build(page);
    }

    private List<CeDimensionRecordVo> filterProjectionRows(CeDimensionRecordBo bo, List<CeDimensionRecordVo> rows) {
        Stream<CeDimensionRecordVo> stream = rows.stream();
        if (StringUtils.isNotBlank(bo.getRecordCode())) {
            stream = stream.filter(row -> contains(row.getRecordCode(), bo.getRecordCode()));
        }
        if (StringUtils.isNotBlank(bo.getRecordName())) {
            stream = stream.filter(row -> contains(row.getRecordName(), bo.getRecordName()));
        }
        if (StringUtils.isNotBlank(bo.getParentCode())) {
            stream = stream.filter(row -> bo.getParentCode().equals(row.getParentCode()));
        }
        if (StringUtils.isNotBlank(bo.getStatus())) {
            stream = stream.filter(row -> bo.getStatus().equals(row.getStatus()));
        }
        return stream.toList();
    }

    private boolean contains(String value, String query) {
        return StringUtils.isNotBlank(value) && value.contains(query);
    }

    private void normalizeCompanyRecord(CeDimensionRecordBo bo) {
        if (!"company".equals(bo.getDimensionCode())) {
            return;
        }
        if (StringUtils.isBlank(bo.getCompanySk())) {
            bo.setCompanySk(buildCompanySk(bo));
        }
        if (StringUtils.isBlank(bo.getActiveFlag())) {
            bo.setActiveFlag("1".equals(bo.getStatus()) ? "N" : "Y");
        }
    }

    private String buildCompanySk(CeDimensionRecordBo bo) {
        String companyCode = normalizeKeyPart(bo.getRecordCode());
        String factoryCode = normalizeKeyPart(bo.getParentCode());
        if (StringUtils.isBlank(factoryCode)) {
            return "SK_" + companyCode;
        }
        return "SK_" + companyCode + "_" + factoryCode;
    }

    private String normalizeKeyPart(String value) {
        return StringUtils.trimToEmpty(value).replaceAll("\\s+", "_");
    }

    private TableDataInfo<CeDimensionRecordVo> queryVendorPageList(CeDimensionRecordBo bo, PageQuery pageQuery) {
        CeLicenseState license = requireCurrentLicense();
        CeVendorDimensionListResponse vendorResponse = vendorDimensionOpenClient.listDimensions(
            license.getLicenseId(),
            license.getInstallId(),
            bo,
            pageQuery.getPageNum(),
            pageQuery.getPageSize()
        );
        if (vendorResponse == null || vendorResponse.getRecords() == null) {
            throw new ServiceException("厂商维度接口返回数据不完整");
        }
        return new TableDataInfo<>(
            vendorResponse.getRecords().stream().map(this::toDimensionRecordVo).toList(),
            vendorResponse.getTotal()
        );
    }

    private CeDimensionRecordVo toDimensionRecordVo(CeVendorDimensionRecord source) {
        CeDimensionRecordVo target = new CeDimensionRecordVo();
        target.setId(source.getId());
        target.setDimensionCode(source.getDimensionCode());
        target.setRecordCode(source.getRecordCode());
        target.setRecordName(source.getRecordName());
        target.setParentCode(source.getParentCode());
        mapVendorFields(source, target);
        Integer sortOrder = source.getSortOrder();
        target.setSortOrder(sortOrder == null ? null : String.valueOf(sortOrder));
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setRemark(source.getRemark());
        return target;
    }

    private void mapVendorFields(CeVendorDimensionRecord source, CeDimensionRecordVo target) {
        switch (source.getDimensionCode()) {
            case "emission-source-category" -> {
                target.setCategorySk(source.getCategorySk());
                target.setBusinessKey(source.getBusinessKey());
                target.setGhgScope(source.getGhgScope());
                target.setGhgScopeCategorySort(asString(source.getGhgScopeCategorySort()));
                target.setGhgScopeCategory(source.getGhgScopeCategory());
                target.setGhgScopeEn(source.getGhgScopeEn());
                target.setGhgScopeCategoryEn(source.getGhgScopeCategoryEn());
                target.setIsoCategory(source.getIsoCategory());
                target.setIsoCategoryEn(source.getIsoCategoryEn());
                target.setIsoCategoryDescription(source.getIsoCategoryDescription());
                target.setIsoCategoryDescriptionEn(source.getIsoCategoryDescriptionEn());
                target.setIsoCustomSubcategory(source.getIsoCustomSubcategory());
                target.setGbScopeCategory(source.getGbScopeCategory());
                target.setGbSubcategory(source.getGbSubcategory());
                target.setEffectiveDate(asString(source.getEffectiveDate()));
                target.setExpiryDate(asString(source.getExpireDate()));
                target.setCurrentFlag(source.getCurrentFlag());
                target.setVersionNo(source.getVersionNo());
                target.setUnifiedStandardCategory(source.getStandardCategory());
            }
            case "base-year" -> {
                target.setBaseYear(asString(source.getBaseYear()));
                target.setCurrentBaseFlag(source.getIsCurrent() != null && source.getIsCurrent() == 1 ? "Y" : "N");
            }
            case "ef-electricity-factor" -> {
                target.setFactorVersion(source.getFactorVersion());
                target.setDivisionCode(source.getDivisionCode());
                target.setDivisionName(source.getDivisionName());
                target.setRegionName(source.getRegionName());
                target.setProvinceFactor(asString(source.getProvinceFactor()));
                target.setRegionFactor(asString(source.getRegionFactor()));
                target.setNationalFactor(asString(source.getNationalFactor()));
                target.setNonFossilExcludedFactor(asString(source.getNonFossilExcludedFactor()));
                target.setNationalFossilPowerFactor(asString(source.getNationalFossilPowerFactor()));
            }
            case "ef-electricity-version" -> {
                target.setFactorVersion(source.getFactorVersion());
                target.setEffectiveYear(asString(source.getEffectiveYear()));
            }
            case "ef-electricity-scope" -> {
                target.setScopeKey(source.getScopeKey());
                target.setScopeName(source.getScopeName());
            }
            case "greenhouse-gas" -> target.setGasNameEn(source.getGasNameEn());
            default -> {
            }
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private CeLicenseState requireCurrentLicense() {
        List<CeLicenseState> states = licenseStateMapper.selectList(new LambdaQueryWrapper<CeLicenseState>()
            .eq(CeLicenseState::getLicenseStatus, LICENSE_STATUS_VALID)
            .orderByDesc(CeLicenseState::getLastVerifiedTime)
            .orderByDesc(CeLicenseState::getId));
        CeLicenseState license = states.stream().findFirst()
            .orElseThrow(() -> new ServiceException("未找到有效授权状态"));
        if (StringUtils.isBlank(license.getLicenseId()) || StringUtils.isBlank(license.getInstallId())) {
            throw new ServiceException("有效授权状态缺少必要信息");
        }
        Date now = new Date();
        if ((license.getValidFrom() != null && license.getValidFrom().after(now))
            || (license.getValidTo() != null && license.getValidTo().before(now))) {
            throw new ServiceException("当前时间不在授权有效期内");
        }
        return license;
    }
}
