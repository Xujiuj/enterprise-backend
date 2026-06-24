package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.client.CeVendorDimensionOpenClient;
import org.dromara.carbon.enterprise.domain.*;
import org.dromara.carbon.enterprise.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.domain.sync.CeDimensionSyncResponse;
import org.dromara.carbon.enterprise.domain.sync.CeDimensionSyncStatus;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionListResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionRecord;
import org.dromara.carbon.enterprise.mapper.*;
import org.dromara.carbon.enterprise.service.ICeDimensionSyncService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        int syncedCount = 0;

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
                syncedCount++;
            }
            hasMore = records.size() >= PAGE_SIZE;
            pageNum++;
        }

        log.info("维度同步完成: dimensionCode={}, syncedCount={}", dimensionCode, syncedCount);
        CeDimensionSyncResponse response = new CeDimensionSyncResponse();
        response.setLicenseId(license.getLicenseId());
        response.setDimensionCode(dimensionCode);
        response.setSyncedCount(syncedCount);
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
            entity.setRemark(record.getRemark());
            adminDivisionMapper.insert(entity);
        } else {
            existing.setDivisionName(record.getRecordName());
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            adminDivisionMapper.updateById(existing);
        }
    }

    private void upsertEmissionSourceCategory(CeVendorDimensionRecord record) {
        String businessKey = record.getRecordCode();
        if (StringUtils.isBlank(businessKey)) {
            return;
        }
        CeEmissionSourceCategory existing = emissionSourceCategoryMapper.selectOne(
            Wrappers.<CeEmissionSourceCategory>lambdaQuery()
                .eq(CeEmissionSourceCategory::getBusinessKey, businessKey), false);
        if (existing == null) {
            CeEmissionSourceCategory entity = new CeEmissionSourceCategory();
            entity.setCategorySk(record.getField01());
            entity.setBusinessKey(businessKey);
            entity.setGhgScope(record.getField03());
            entity.setGhgScopeCategorySort(parseInteger(record.getField04()));
            entity.setGhgScopeCategory(record.getField05());
            entity.setGhgScopeEn(record.getField06());
            entity.setGhgScopeCategoryEn(record.getField07());
            entity.setIsoCategory(record.getField08());
            entity.setIsoCategoryEn(record.getField09());
            entity.setIsoCategoryDescription(record.getField10());
            entity.setIsoCategoryDescriptionEn(record.getField11());
            entity.setIsoCustomSubcategory(record.getField12());
            entity.setGbScopeCategory(record.getField13());
            entity.setGbSubcategory(record.getField14());
            entity.setIsCurrent(record.getField17());
            entity.setVersionNo(record.getField18());
            entity.setUnifiedStandardCategory(record.getField19());
            entity.setRemark(record.getRemark());
            emissionSourceCategoryMapper.insert(entity);
        } else {
            existing.setCategorySk(record.getField01());
            existing.setGhgScope(record.getField03());
            existing.setGhgScopeCategorySort(parseInteger(record.getField04()));
            existing.setGhgScopeCategory(record.getField05());
            existing.setGhgScopeEn(record.getField06());
            existing.setGhgScopeCategoryEn(record.getField07());
            existing.setIsoCategory(record.getField08());
            existing.setIsoCategoryEn(record.getField09());
            existing.setIsoCategoryDescription(record.getField10());
            existing.setIsoCategoryDescriptionEn(record.getField11());
            existing.setIsoCustomSubcategory(record.getField12());
            existing.setGbScopeCategory(record.getField13());
            existing.setGbSubcategory(record.getField14());
            existing.setIsCurrent(record.getField17());
            existing.setVersionNo(record.getField18());
            existing.setUnifiedStandardCategory(record.getField19());
            existing.setRemark(record.getRemark());
            emissionSourceCategoryMapper.updateById(existing);
        }
    }

    private void upsertBaseYear(CeVendorDimensionRecord record) {
        String factoryCode = record.getRecordCode();
        if (StringUtils.isBlank(factoryCode)) {
            return;
        }
        CeBaseYear existing = baseYearMapper.selectOne(
            Wrappers.<CeBaseYear>lambdaQuery()
                .eq(CeBaseYear::getFactoryCode, factoryCode), false);
        if (existing == null) {
            CeBaseYear entity = new CeBaseYear();
            entity.setFactoryCode(factoryCode);
            entity.setFactoryName(record.getRecordName());
            entity.setBaseYear(parseInteger(record.getField01()));
            entity.setEnabledFlag("N".equals(record.getField02()) || "1".equals(record.getStatus()) ? 0 : 1);
            entity.setRemark(record.getRemark());
            baseYearMapper.insert(entity);
        } else {
            existing.setFactoryName(record.getRecordName());
            existing.setBaseYear(parseInteger(record.getField01()));
            existing.setEnabledFlag("N".equals(record.getField02()) || "1".equals(record.getStatus()) ? 0 : 1);
            existing.setUpdateTime(new Date());
            existing.setRemark(record.getRemark());
            baseYearMapper.updateById(existing);
        }
    }

    private void upsertElectricityFactor(CeVendorDimensionRecord record) {
        String versionProvinceCode = record.getRecordCode();
        if (StringUtils.isBlank(versionProvinceCode)) {
            return;
        }
        CeElectricityFactor existing = electricityFactorMapper.selectOne(
            Wrappers.<CeElectricityFactor>lambdaQuery()
                .eq(CeElectricityFactor::getVersionProvinceCode, versionProvinceCode), false);
        if (existing == null) {
            CeElectricityFactor entity = new CeElectricityFactor();
            entity.setVersionProvinceCode(versionProvinceCode);
            entity.setFactorVersion(record.getField01());
            entity.setDivisionCode(record.getField02());
            entity.setDivisionName(record.getField03());
            entity.setRegionName(record.getField04());
            entity.setProvinceFactor(parseBigDecimal(record.getField05()));
            entity.setRegionFactor(parseBigDecimal(record.getField06()));
            entity.setNationalFactor(parseBigDecimal(record.getField07()));
            entity.setNonFossilExcludedFactor(parseBigDecimal(record.getField08()));
            entity.setNationalFossilPowerFactor(parseBigDecimal(record.getField09()));
            entity.setRemark(record.getRemark());
            electricityFactorMapper.insert(entity);
        } else {
            existing.setFactorVersion(record.getField01());
            existing.setDivisionCode(record.getField02());
            existing.setDivisionName(record.getField03());
            existing.setRegionName(record.getField04());
            existing.setProvinceFactor(parseBigDecimal(record.getField05()));
            existing.setRegionFactor(parseBigDecimal(record.getField06()));
            existing.setNationalFactor(parseBigDecimal(record.getField07()));
            existing.setNonFossilExcludedFactor(parseBigDecimal(record.getField08()));
            existing.setNationalFossilPowerFactor(parseBigDecimal(record.getField09()));
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
        CeElectricityFactorVersionMap existing = electricityFactorVersionMapMapper.selectOne(
            Wrappers.<CeElectricityFactorVersionMap>lambdaQuery()
                .eq(CeElectricityFactorVersionMap::getFactorVersion, factorVersion), false);
        if (existing == null) {
            CeElectricityFactorVersionMap entity = new CeElectricityFactorVersionMap();
            entity.setFactorVersion(factorVersion);
            entity.setEffectiveYear(parseInteger(record.getField02()));
            entity.setRemark(record.getRemark());
            electricityFactorVersionMapMapper.insert(entity);
        } else {
            existing.setEffectiveYear(parseInteger(record.getField02()));
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
            entity.setRemark(record.getRemark());
            electricityFactorScopeMapper.insert(entity);
        } else {
            existing.setScopeName(record.getRecordName());
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
            entity.setGasNameEn(record.getField01());
            entity.setRemark(record.getRemark());
            greenhouseGasMapper.insert(entity);
        } else {
            existing.setGasName(record.getRecordName());
            existing.setGasNameEn(record.getField01());
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
}
