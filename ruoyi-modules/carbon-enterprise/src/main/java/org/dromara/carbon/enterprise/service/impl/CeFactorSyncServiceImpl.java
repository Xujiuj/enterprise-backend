package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.client.CeVendorFactorOpenClient;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.domain.CeFactorCacheVersion;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.sync.CeFactorSyncResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorFactorRecord;
import org.dromara.carbon.enterprise.domain.sync.CeVendorFactorSyncResponse;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheVersionMapper;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.ICeFactorSyncService;
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
            .orElseThrow(() -> new ServiceException("valid license state does not exist"));
        if (StringUtils.isBlank(license.getLicenseId()) || StringUtils.isBlank(license.getInstallId())) {
            throw new ServiceException("valid license state is incomplete");
        }
        Date now = new Date();
        if ((license.getValidFrom() != null && license.getValidFrom().after(now))
            || (license.getValidTo() != null && license.getValidTo().before(now))) {
            throw new ServiceException("valid license state is not currently valid");
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
        target.setFactorCode(source.getFactorCode());
        target.setFactorName(source.getFactorName());
        target.setFactorCategory(source.getFactorCategory());
        target.setFactorValue(source.getFactorValue());
        target.setFactorUnit(source.getFactorUnit());
        target.setSourceRef(source.getSourceRef());
        target.setEnabledFlag(Boolean.TRUE);
        target.setSyncedTime(syncedTime);
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
