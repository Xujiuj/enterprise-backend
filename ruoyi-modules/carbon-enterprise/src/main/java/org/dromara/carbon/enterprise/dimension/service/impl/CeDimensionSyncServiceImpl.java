package org.dromara.carbon.enterprise.dimension.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.dimension.domain.CeAdminDivision;
import org.dromara.carbon.enterprise.dimension.domain.CeBaseYear;
import org.dromara.carbon.enterprise.dimension.domain.CeCompanyFactory;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.dimension.domain.CeDimensionSyncResponse;
import org.dromara.carbon.enterprise.dimension.domain.CeDimensionSyncStatus;
import org.dromara.carbon.enterprise.dimension.mapper.CeAdminDivisionMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeBaseYearMapper;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactor;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactorScope;
import org.dromara.carbon.enterprise.factor.domain.CeElectricityFactorVersionMap;
import org.dromara.carbon.enterprise.factor.domain.CeGreenhouseGas;
import org.dromara.carbon.enterprise.factor.mapper.CeElectricityFactorMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeElectricityFactorScopeMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeElectricityFactorVersionMapMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeGreenhouseGasMapper;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionListResponse;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionRecord;
import org.dromara.carbon.enterprise.vendor.client.CeVendorDimensionOpenClient;
import org.dromara.carbon.enterprise.shared.service.ICeDimensionSyncService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 企业端维度同步服务实现.
 * 从厂商端开放接口拉取维度数据，写入企业本地维度表。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CeDimensionSyncServiceImpl implements ICeDimensionSyncService {

    private static final String LICENSE_STATUS_VALID = "VALID";
    private static final int PAGE_SIZE = 500;

    /** 可同步的7个厂商维度编码 */
    private static final List<String> VENDOR_DIMENSION_CODES = List.of(
        "admin-division",
        "emission-source-category",
        "base-year",
        "ef-electricity-factor",
        "ef-electricity-version",
        "ef-electricity-scope",
        "greenhouse-gas"
    );

    private final CeLicenseStateMapper licenseStateMapper;
    private final CeVendorDimensionOpenClient vendorDimensionOpenClient;
    private final CeAdminDivisionMapper adminDivisionMapper;
    private final CeEmissionSourceCategoryMapper emissionSourceCategoryMapper;
    private final CeBaseYearMapper baseYearMapper;
    private final CeElectricityFactorMapper electricityFactorMapper;
    private final CeElectricityFactorVersionMapMapper electricityFactorVersionMapMapper;
    private final CeElectricityFactorScopeMapper electricityFactorScopeMapper;
    private final CeGreenhouseGasMapper greenhouseGasMapper;

    /** 最近一次同步状态（内存存储，重启后清空） */
    private volatile CeDimensionSyncStatus lastSyncStatus;

    @Override
    public List<CeDimensionSyncResponse> syncAllVendorDimensions() {
        CeLicenseState license = requireCurrentLicense();
        List<CeDimensionSyncResponse> results = new ArrayList<>();
        for (String dimensionCode : VENDOR_DIMENSION_CODES) {
            try {
                CeDimensionSyncResponse response = syncDimensionInternal(license, dimensionCode);
                results.add(response);
            } catch (Exception e) {
                log.error("维度同步失败: dimensionCode={}, error={}", dimensionCode, e.getMessage(), e);
                CeDimensionSyncResponse failResponse = new CeDimensionSyncResponse();
                failResponse.setLicenseId(license.getLicenseId());
                failResponse.setDimensionCode(dimensionCode);
                failResponse.setSuccess(false);
                failResponse.setErrorMessage(e.getMessage());
                failResponse.setSyncedTime(new Date());
                results.add(failResponse);
            }
        }
        updateLastSyncStatus(license.getLicenseId(), results);
        return results;
    }

    @Override
    public CeDimensionSyncResponse syncDimension(String dimensionCode) {
        if (StringUtils.isBlank(dimensionCode)) {
            throw new ServiceException("dimensionCode不能为空");
        }
        CeLicenseState license = requireCurrentLicense();
        CeDimensionSyncResponse response = syncDimensionInternal(license, dimensionCode);
        List<CeDimensionSyncResponse> results = new ArrayList<>();
        results.add(response);
        updateLastSyncStatus(license.getLicenseId(), results);
        return response;
    }

    @Override
    public CeDimensionSyncStatus getLastSyncStatus() {
        return lastSyncStatus;
    }

    /**
     * 同步单个维度的内部实现.
     */
    private CeDimensionSyncResponse syncDimensionInternal(CeLicenseState license, String dimensionCode) {
        log.info("开始同步维度: dimensionCode={}, licenseId={}", dimensionCode, license.getLicenseId());
        Date syncedTime = new Date();
        int recordCount = 0;

        CeDimensionRecordBo query = new CeDimensionRecordBo();
        query.setDimensionCode(dimensionCode);

        int pageNum = 1;
        boolean hasMore = true;
        while (hasMore) {
            CeVendorDimensionListResponse vendorResponse = vendorDimensionOpenClient.listDimensions(
                license.getLicenseId(),
                license.getInstallId(),
                query,
                pageNum,
                PAGE_SIZE
            );
            List<CeVendorDimensionRecord> records = vendorResponse.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (CeVendorDimensionRecord record : records) {
                upsertDimensionRecord(dimensionCode, record);
                recordCount++;
            }
            hasMore = records.size() >= PAGE_SIZE;
            pageNum++;
        }

        log.info("维度同步完成: dimensionCode={}, recordCount={}", dimensionCode, recordCount);
        CeDimensionSyncResponse response = new CeDimensionSyncResponse();
        response.setLicenseId(license.getLicenseId());
        response.setDimensionCode(dimensionCode);
        response.setRecordCount(recordCount);
        response.setSyncedTime(syncedTime);
        response.setSuccess(true);
        return response;
    }

    /**
     * 根据维度编码将厂商记录写入对应的本地表（upsert）.
     */
    private void upsertDimensionRecord(String dimensionCode, CeVendorDimensionRecord record) {
        switch (dimensionCode) {
            case "admin-division" -> upsertAdminDivision(record);
            case "emission-source-category" -> upsertEmissionSourceCategory(record);
            case "base-year" -> upsertBaseYear(record);
            case "ef-electricity-factor" -> upsertElectricityFactor(record);
            case "ef-electricity-version" -> upsertElectricityFactorVersionMap(record);
            case "ef-electricity-scope" -> upsertElectricityFactorScope(record);
            case "greenhouse-gas" -> upsertGreenhouseGas(record);
            default -> log.warn("未知的维度编码: {}", dimensionCode);
        }
    }

    private void upsertAdminDivision(CeVendorDimensionRecord record) {
        String divisionCode = record.getRecordCode();
        if (StringUtils.isBlank(divisionCode)) {
            return;
        }
        CeAdminDivision existing = adminDivisionMapper.selectOne(
            Wrappers.<CeAdminDivision>lambdaQuery()
                .eq(CeAdminDivision::getDivisionCode, divisionCode), false);
        if (existing == null) {
            CeAdminDivision entity = new CeAdminDivision();
            entity.setDivisionCode(divisionCode);
            entity.setDivisionName(record.getRecordName());
            entity.setParentCode(record.getParentCode());
            entity.setLevelType(record.getLevelType());
            entity.setSortOrder(record.getSortOrder());
            entity.setStatus(record.getStatus());
            entity.setRemark(record.getRemark());
            adminDivisionMapper.insert(entity);
        } else {
            existing.setDivisionName(record.getRecordName());
            existing.setParentCode(record.getParentCode());
            existing.setLevelType(record.getLevelType());
            existing.setSortOrder(record.getSortOrder());
            existing.setStatus(record.getStatus());
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            adminDivisionMapper.updateById(existing);
        }
    }

    private void upsertEmissionSourceCategory(CeVendorDimensionRecord record) {
        String categorySk = StringUtils.isNotBlank(record.getCategorySk()) ? record.getCategorySk() : record.getRecordCode();
        if (StringUtils.isBlank(categorySk)) {
            return;
        }
        String businessKey = StringUtils.isNotBlank(record.getBusinessKey()) ? record.getBusinessKey() : categorySk;
        CeEmissionSourceCategory existing = emissionSourceCategoryMapper.selectOne(
            Wrappers.<CeEmissionSourceCategory>lambdaQuery()
                .eq(CeEmissionSourceCategory::getCategorySk, categorySk), false);
        if (existing == null) {
            CeEmissionSourceCategory entity = new CeEmissionSourceCategory();
            entity.setCategorySk(categorySk);
            entity.setBusinessKey(businessKey);
            entity.setParentCode(record.getParentCode());
            entity.setCategoryNameEn(record.getCategoryNameEn());
            entity.setGhgScope(record.getGhgScope());
            entity.setGhgScopeCategorySort(record.getGhgScopeCategorySort());
            entity.setGhgScopeCategory(record.getGhgScopeCategory());
            entity.setGhgScopeEn(record.getGhgScopeEn());
            entity.setGhgScopeCategoryEn(record.getGhgScopeCategoryEn());
            entity.setIsoCategory(record.getIsoCategory());
            entity.setIsoCategoryEn(record.getIsoCategoryEn());
            entity.setIsoCategoryDescription(record.getIsoCategoryDescription());
            entity.setIsoCategoryDescriptionEn(record.getIsoCategoryDescriptionEn());
            entity.setIsoCustomSubcategory(record.getIsoCustomSubcategory());
            entity.setGbScopeCategory(record.getGbScopeCategory());
            entity.setGbSubcategory(record.getGbSubcategory());
            entity.setEffectiveDate(formatDate(record.getEffectiveDate()));
            entity.setExpiryDate(formatDate(record.getExpireDate()));
            entity.setIsCurrent(record.getCurrentFlag());
            entity.setVersionNo(record.getVersionNo());
            entity.setUnifiedStandardCategory(record.getStandardCategory());
            entity.setSortOrder(record.getSortOrder());
            entity.setStatus(record.getStatus());
            entity.setRemark(record.getRemark());
            emissionSourceCategoryMapper.insert(entity);
        } else {
            existing.setCategorySk(categorySk);
            existing.setBusinessKey(businessKey);
            existing.setParentCode(record.getParentCode());
            existing.setCategoryNameEn(record.getCategoryNameEn());
            existing.setGhgScope(record.getGhgScope());
            existing.setGhgScopeCategorySort(record.getGhgScopeCategorySort());
            existing.setGhgScopeCategory(record.getGhgScopeCategory());
            existing.setGhgScopeEn(record.getGhgScopeEn());
            existing.setGhgScopeCategoryEn(record.getGhgScopeCategoryEn());
            existing.setIsoCategory(record.getIsoCategory());
            existing.setIsoCategoryEn(record.getIsoCategoryEn());
            existing.setIsoCategoryDescription(record.getIsoCategoryDescription());
            existing.setIsoCategoryDescriptionEn(record.getIsoCategoryDescriptionEn());
            existing.setIsoCustomSubcategory(record.getIsoCustomSubcategory());
            existing.setGbScopeCategory(record.getGbScopeCategory());
            existing.setGbSubcategory(record.getGbSubcategory());
            existing.setEffectiveDate(formatDate(record.getEffectiveDate()));
            existing.setExpiryDate(formatDate(record.getExpireDate()));
            existing.setIsCurrent(record.getCurrentFlag());
            existing.setVersionNo(record.getVersionNo());
            existing.setUnifiedStandardCategory(record.getStandardCategory());
            existing.setSortOrder(record.getSortOrder());
            existing.setStatus(record.getStatus());
            existing.setRemark(record.getRemark());
            emissionSourceCategoryMapper.updateById(existing);
        }
    }

    private void upsertBaseYear(CeVendorDimensionRecord record) {
        String baseYearKey = StringUtils.isNotBlank(record.getBaseYearKey()) ? record.getBaseYearKey() : record.getRecordCode();
        if (StringUtils.isBlank(baseYearKey)) {
            return;
        }
        CeBaseYear existing = baseYearMapper.selectOne(
            Wrappers.<CeBaseYear>lambdaQuery()
                .eq(CeBaseYear::getBaseYearKey, baseYearKey), false);
        if (existing == null) {
            CeBaseYear entity = new CeBaseYear();
            entity.setBaseYearKey(baseYearKey);
            entity.setDescription(record.getDescription());
            entity.setBaseYear(record.getBaseYear());
            entity.setIsCurrent(record.getIsCurrent());
            entity.setEnabledFlag(record.getIsCurrent() != null && record.getIsCurrent() == 0 || "1".equals(record.getStatus()) ? 0 : 1);
            entity.setSortOrder(record.getSortOrder());
            entity.setStatus(record.getStatus());
            entity.setRemark(record.getRemark());
            baseYearMapper.insert(entity);
        } else {
            existing.setBaseYearKey(baseYearKey);
            existing.setDescription(record.getDescription());
            existing.setBaseYear(record.getBaseYear());
            existing.setIsCurrent(record.getIsCurrent());
            existing.setEnabledFlag(record.getIsCurrent() != null && record.getIsCurrent() == 0 || "1".equals(record.getStatus()) ? 0 : 1);
            existing.setSortOrder(record.getSortOrder());
            existing.setStatus(record.getStatus());
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            baseYearMapper.updateById(existing);
        }
    }

    private void upsertElectricityFactor(CeVendorDimensionRecord record) {
        String versionProvinceCode = StringUtils.isNotBlank(record.getVersionProvinceCode())
            ? record.getVersionProvinceCode()
            : record.getRecordCode();
        if (StringUtils.isBlank(versionProvinceCode)) {
            return;
        }
        CeElectricityFactor existing = electricityFactorMapper.selectOne(
            Wrappers.<CeElectricityFactor>lambdaQuery()
                .eq(CeElectricityFactor::getVersionProvinceCode, versionProvinceCode), false);
        if (existing == null) {
            CeElectricityFactor entity = new CeElectricityFactor();
            entity.setVersionProvinceCode(versionProvinceCode);
            entity.setFactorVersion(record.getFactorVersion());
            entity.setDivisionCode(record.getDivisionCode());
            entity.setDivisionName(record.getDivisionName());
            entity.setRegionName(record.getRegionName());
            entity.setProvinceFactor(record.getProvinceFactor());
            entity.setRegionFactor(record.getRegionFactor());
            entity.setNationalFactor(record.getNationalFactor());
            entity.setNonFossilExcludedFactor(record.getNonFossilExcludedFactor());
            entity.setNationalFossilPowerFactor(record.getNationalFossilPowerFactor());
            entity.setSortOrder(record.getSortOrder());
            entity.setStatus(record.getStatus());
            entity.setRemark(record.getRemark());
            electricityFactorMapper.insert(entity);
        } else {
            existing.setFactorVersion(record.getFactorVersion());
            existing.setDivisionCode(record.getDivisionCode());
            existing.setDivisionName(record.getDivisionName());
            existing.setRegionName(record.getRegionName());
            existing.setProvinceFactor(record.getProvinceFactor());
            existing.setRegionFactor(record.getRegionFactor());
            existing.setNationalFactor(record.getNationalFactor());
            existing.setNonFossilExcludedFactor(record.getNonFossilExcludedFactor());
            existing.setNationalFossilPowerFactor(record.getNationalFossilPowerFactor());
            existing.setSortOrder(record.getSortOrder());
            existing.setStatus(record.getStatus());
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            electricityFactorMapper.updateById(existing);
        }
    }

    private void upsertElectricityFactorVersionMap(CeVendorDimensionRecord record) {
        String factorVersion = record.getRecordCode();
        if (StringUtils.isBlank(factorVersion)) {
            return;
        }
        List<CeElectricityFactorVersionMap> existingRecords = electricityFactorVersionMapMapper.selectList(
            Wrappers.<CeElectricityFactorVersionMap>lambdaQuery()
                .eq(CeElectricityFactorVersionMap::getFactorVersion, factorVersion)
                .orderByAsc(CeElectricityFactorVersionMap::getId));
        CeElectricityFactorVersionMap existing = existingRecords.stream()
            .filter(item -> Objects.equals(item.getEffectiveYear(), record.getEffectiveYear()))
            .findFirst()
            .orElse(existingRecords.isEmpty() ? null : existingRecords.get(0));
        if (existing == null) {
            CeElectricityFactorVersionMap entity = new CeElectricityFactorVersionMap();
            entity.setFactorVersion(factorVersion);
            entity.setEffectiveYear(record.getEffectiveYear());
            entity.setSortOrder(record.getSortOrder());
            entity.setStatus(record.getStatus());
            entity.setRemark(record.getRemark());
            electricityFactorVersionMapMapper.insert(entity);
        } else {
            for (CeElectricityFactorVersionMap duplicate : existingRecords) {
                if (!Objects.equals(duplicate.getId(), existing.getId())) {
                    electricityFactorVersionMapMapper.deleteById(duplicate.getId());
                }
            }
            existing.setEffectiveYear(record.getEffectiveYear());
            existing.setSortOrder(record.getSortOrder());
            existing.setStatus(record.getStatus());
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            electricityFactorVersionMapMapper.updateById(existing);
        }
    }

    private void upsertElectricityFactorScope(CeVendorDimensionRecord record) {
        String scopeKey = record.getRecordCode();
        if (StringUtils.isBlank(scopeKey)) {
            return;
        }
        CeElectricityFactorScope existing = electricityFactorScopeMapper.selectOne(
            Wrappers.<CeElectricityFactorScope>lambdaQuery()
                .eq(CeElectricityFactorScope::getScopeKey, scopeKey), false);
        if (existing == null) {
            CeElectricityFactorScope entity = new CeElectricityFactorScope();
            entity.setScopeKey(scopeKey);
            entity.setScopeName(record.getRecordName());
            entity.setSortOrder(record.getSortOrder());
            entity.setStatus(record.getStatus());
            entity.setRemark(record.getRemark());
            electricityFactorScopeMapper.insert(entity);
        } else {
            existing.setScopeName(record.getRecordName());
            existing.setSortOrder(record.getSortOrder());
            existing.setStatus(record.getStatus());
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            electricityFactorScopeMapper.updateById(existing);
        }
    }

    private void upsertGreenhouseGas(CeVendorDimensionRecord record) {
        String gasCode = record.getRecordCode();
        if (StringUtils.isBlank(gasCode)) {
            return;
        }
        CeGreenhouseGas existing = greenhouseGasMapper.selectOne(
            Wrappers.<CeGreenhouseGas>lambdaQuery()
                .eq(CeGreenhouseGas::getGasCode, gasCode), false);
        if (existing == null) {
            CeGreenhouseGas entity = new CeGreenhouseGas();
            entity.setGasCode(gasCode);
            entity.setGasName(record.getRecordName());
            entity.setGasNameEn(record.getGasNameEn());
            entity.setGwpValue(record.getGwpValue());
            entity.setGwpVersion(record.getGwpVersion());
            entity.setChemicalFormula(record.getChemicalFormula());
            entity.setSortOrder(record.getSortOrder());
            entity.setStatus(record.getStatus());
            entity.setRemark(record.getRemark());
            greenhouseGasMapper.insert(entity);
        } else {
            existing.setGasName(record.getRecordName());
            existing.setGasNameEn(record.getGasNameEn());
            existing.setGwpValue(record.getGwpValue());
            existing.setGwpVersion(record.getGwpVersion());
            existing.setChemicalFormula(record.getChemicalFormula());
            existing.setSortOrder(record.getSortOrder());
            existing.setStatus(record.getStatus());
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            greenhouseGasMapper.updateById(existing);
        }
    }

    private CeLicenseState requireCurrentLicense() {
        List<CeLicenseState> states = licenseStateMapper.selectList(Wrappers.<CeLicenseState>lambdaQuery()
            .eq(CeLicenseState::getLicenseStatus, LICENSE_STATUS_VALID)
            .orderByDesc(CeLicenseState::getLastVerifiedTime)
            .orderByDesc(CeLicenseState::getId));
        CeLicenseState license = states.stream().findFirst()
            .orElseThrow(() -> new ServiceException("未找到有效的许可证状态"));
        if (StringUtils.isBlank(license.getLicenseId()) || StringUtils.isBlank(license.getInstallId())) {
            throw new ServiceException("许可证状态信息不完整");
        }
        Date now = new Date();
        if ((license.getValidFrom() != null && license.getValidFrom().after(now))
            || (license.getValidTo() != null && license.getValidTo().before(now))) {
            throw new ServiceException("许可证当前不在有效期内");
        }
        return license;
    }

    private void updateLastSyncStatus(String licenseId, List<CeDimensionSyncResponse> results) {
        CeDimensionSyncStatus status = new CeDimensionSyncStatus();
        status.setLicenseId(licenseId);
        status.setLastSyncTime(new Date());
        status.setResults(results);
        status.setSuccessCount((int) results.stream().filter(CeDimensionSyncResponse::isSuccess).count());
        status.setFailCount((int) results.stream().filter(r -> !r.isSuccess()).count());
        lastSyncStatus = status;
    }

    private Integer parseInteger(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析整数: {}", value);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析数值: {}", value);
            return null;
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
