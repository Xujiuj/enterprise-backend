package org.dromara.carbon.enterprise.factor.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.vendor.client.CeVendorFactorOpenClient;
import org.dromara.carbon.enterprise.factor.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.factor.domain.CeFactorCacheVersion;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.factor.domain.CeFactorSyncResponse;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorFactorRecord;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorFactorSyncResponse;
import org.dromara.carbon.enterprise.factor.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeFactorCacheVersionMapper;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.shared.service.ICeFactorSyncService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Enterprise factor sync service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeFactorSyncServiceImpl implements ICeFactorSyncService {

    private static final String LICENSE_STATUS_VALID = "VALID";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CeLicenseStateMapper licenseStateMapper;
    private final CeFactorCacheVersionMapper factorCacheVersionMapper;
    private final CeFactorCacheRecordMapper factorCacheRecordMapper;
    private final CeVendorFactorOpenClient vendorFactorOpenClient;

    @Override
    public CeFactorSyncResponse syncCurrentLicenseFactors() {
        CeLicenseState license = requireCurrentLicense();
        CeFactorCacheVersion currentCache = findCurrentCache(license.getLicenseId());
        String currentVersionCode = currentCache == null ? null : currentCache.getVersionCode();
        CeVendorFactorSyncResponse vendorResponse = vendorFactorOpenClient.syncFactors(
            license.getLicenseId(),
            license.getInstallId(),
            currentVersionCode
        );
        validateVendorResponse(vendorResponse);
        Date syncedTime = new Date();
        CeFactorCacheVersion cacheVersion = upsertCacheVersion(license.getLicenseId(), vendorResponse, syncedTime);
        upsertCacheRecords(cacheVersion.getId(), vendorResponse.getRecords(), syncedTime);
        return toSyncResponse(license.getLicenseId(), vendorResponse, syncedTime);
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

    private CeFactorCacheVersion findCurrentCache(String licenseId) {
        return factorCacheVersionMapper.selectOne(Wrappers.<CeFactorCacheVersion>lambdaQuery()
            .eq(CeFactorCacheVersion::getLicenseId, licenseId)
            .orderByDesc(CeFactorCacheVersion::getSyncedTime)
            .orderByDesc(CeFactorCacheVersion::getId), false);
    }

    private CeFactorCacheVersion upsertCacheVersion(String licenseId, CeVendorFactorSyncResponse vendorResponse, Date syncedTime) {
        CeFactorCacheVersion cacheVersion = factorCacheVersionMapper.selectOne(Wrappers.<CeFactorCacheVersion>lambdaQuery()
            .eq(CeFactorCacheVersion::getVendorVersionId, vendorResponse.getVendorVersionId())
            .eq(CeFactorCacheVersion::getLicenseId, licenseId), false);
        if (cacheVersion == null) {
            cacheVersion = new CeFactorCacheVersion();
            cacheVersion.setVendorVersionId(vendorResponse.getVendorVersionId());
            cacheVersion.setLicenseId(licenseId);
            cacheVersion.setVersionCode(vendorResponse.getVersionCode());
            cacheVersion.setFrozenFlag(vendorResponse.getFrozenFlag());
            cacheVersion.setSyncedTime(syncedTime);
            factorCacheVersionMapper.insert(cacheVersion);
        } else {
            cacheVersion.setVersionCode(vendorResponse.getVersionCode());
            cacheVersion.setFrozenFlag(vendorResponse.getFrozenFlag());
            cacheVersion.setSyncedTime(syncedTime);
            factorCacheVersionMapper.updateById(cacheVersion);
        }
        return cacheVersion;
    }

    private void upsertCacheRecords(Long cacheVersionId, List<CeVendorFactorRecord> records, Date syncedTime) {
        for (CeVendorFactorRecord record : records) {
            CeFactorCacheRecord cacheRecord = factorCacheRecordMapper.selectOne(Wrappers.<CeFactorCacheRecord>lambdaQuery()
                .eq(CeFactorCacheRecord::getCacheVersionId, cacheVersionId)
                .eq(CeFactorCacheRecord::getFactorTableCode,
                    StringUtils.isBlank(record.getFactorTableCode()) ? "201ef" : record.getFactorTableCode())
                .eq(CeFactorCacheRecord::getFactorCode, record.getFactorCode()), false);
            if (cacheRecord == null) {
                cacheRecord = new CeFactorCacheRecord();
                cacheRecord.setCacheVersionId(cacheVersionId);
                copyRecord(record, cacheRecord, syncedTime);
                factorCacheRecordMapper.insert(cacheRecord);
            } else {
                copyRecord(record, cacheRecord, syncedTime);
                factorCacheRecordMapper.updateById(cacheRecord);
            }
        }
    }

    private void copyRecord(CeVendorFactorRecord source, CeFactorCacheRecord target, Date syncedTime) {
        target.setFactorTableCode(StringUtils.isBlank(source.getFactorTableCode()) ? "201ef" : source.getFactorTableCode());
        target.setFactorCode(source.getFactorCode());
        target.setFactorName(source.getFactorName());
        target.setFactorCategory(source.getFactorCategory());
        target.setFactorValue(source.getFactorValue());
        target.setFactorUnit(source.getFactorUnit());
        target.setFactorKey(source.getFactorKey());
        target.setEmissionSourceName(source.getEmissionSourceName());
        target.setEmissionSourceNameEn(source.getEmissionSourceNameEn());
        target.setFuelMaterialCategory(source.getFuelMaterialCategory());
        target.setSourceUnit(source.getSourceUnit());
        target.setCo2(source.getCo2());
        target.setCh4(source.getCh4());
        target.setN2o(source.getN2o());
        target.setHfcs(source.getHfcs());
        target.setPfcs(source.getPfcs());
        target.setSf6(source.getSf6());
        target.setNf3(source.getNf3());
        target.setApplicableScope(source.getApplicableScope());
        target.setFactorSource(source.getFactorSource());
        target.setGwpCh4(source.getGwpCh4());
        target.setGwpN2o(source.getGwpN2o());
        target.setGwpHfcs(source.getGwpHfcs());
        target.setGwpPfcs(source.getGwpPfcs());
        target.setGwpSf6(source.getGwpSf6());
        target.setGwpNf3(source.getGwpNf3());
        target.setFactorGwp(source.getFactorGwp());
        target.setVersionProvinceCode(source.getVersionProvinceCode());
        target.setFactorVersion(source.getFactorVersion());
        target.setDivisionCode(source.getDivisionCode());
        target.setDivisionName(source.getDivisionName());
        target.setRegionName(source.getRegionName());
        target.setProvinceFactor(source.getProvinceFactor());
        target.setRegionFactor(source.getRegionFactor());
        target.setNationalFactor(source.getNationalFactor());
        target.setNonFossilExcludedFactor(source.getNonFossilExcludedFactor());
        target.setNationalFossilPowerFactor(source.getNationalFossilPowerFactor());
        target.setRowNo(source.getRowNo());
        target.setFuelLevel1(source.getFuelLevel1());
        target.setFuelLevel2(source.getFuelLevel2());
        target.setFuelLevel3(source.getFuelLevel3());
        target.setFuelLevel4(source.getFuelLevel4());
        target.setLowerHeatValue(source.getLowerHeatValue());
        target.setLowerHeatValueCv(source.getLowerHeatValueCv());
        target.setCo2Factor(source.getCo2Factor());
        target.setCo2FactorCv(source.getCo2FactorCv());
        target.setGwpValue(source.getGwpValue());
        target.setConvertedFactor(source.getConvertedFactor());
        target.setSourceRef(source.getSourceRef());
        target.setCustomFields(source.getCustomFields() == null || source.getCustomFields().isEmpty()
            ? null
            : toJsonString(source.getCustomFields()));
        target.setEnabledFlag(Boolean.TRUE);
        target.setSyncedTime(syncedTime);
    }

    private String toJsonString(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ServiceException("factor custom fields serialization failed");
        }
    }

    private void validateVendorResponse(CeVendorFactorSyncResponse vendorResponse) {
        if (vendorResponse == null
            || StringUtils.isBlank(vendorResponse.getVendorVersionId())
            || StringUtils.isBlank(vendorResponse.getVersionCode())
            || vendorResponse.getRecords() == null) {
            throw new ServiceException("vendor factor sync response is incomplete");
        }
    }

    private CeFactorSyncResponse toSyncResponse(String licenseId, CeVendorFactorSyncResponse vendorResponse, Date syncedTime) {
        CeFactorSyncResponse response = new CeFactorSyncResponse();
        response.setLicenseId(licenseId);
        response.setVendorVersionId(vendorResponse.getVendorVersionId());
        response.setVersionCode(vendorResponse.getVersionCode());
        response.setFrozenFlag(vendorResponse.getFrozenFlag());
        response.setChanged(vendorResponse.isChanged());
        response.setRecordCount(vendorResponse.getRecords().size());
        response.setSyncedTime(syncedTime);
        return response;
    }
}
