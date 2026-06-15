package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.client.CeVendorDimensionOpenClient;
import org.dromara.carbon.enterprise.domain.CeDimensionRecord;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionListResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionRecord;
import org.dromara.carbon.enterprise.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.mapper.CeDimensionRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.ICeDimensionRecordService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Enterprise dimension record service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeDimensionRecordServiceImpl implements ICeDimensionRecordService {

    private static final String LICENSE_STATUS_VALID = "VALID";

    private static final Set<String> ALLOWED_DIMENSION_CODES = Set.of(
        "admin-division",
        "company",
        "emission-source-category",
        "emission-source",
        "base-year",
        "ef-factor",
        "ef-electricity-factor",
        "ef-electricity-version",
        "ef-electricity-scope",
        "greenhouse-gas",
        "emission-activity-data",
        "green-electricity-data",
        "intensity-denominator",
        "intensity-target",
        "denominator-fact",
        "intensity-tolerance",
        "data-validation",
        "report-template-download"
    );

    private static final Set<String> VENDOR_ONLY_DIMENSION_CODES = Set.of(
        "admin-division",
        "emission-source-category",
        "base-year",
        "ef-electricity-factor",
        "ef-electricity-version",
        "ef-electricity-scope",
        "greenhouse-gas",
        "report-template-download"
    );

    private final CeDimensionRecordMapper dimensionRecordMapper;
    private final CeLicenseStateMapper licenseStateMapper;
    private final CeVendorDimensionOpenClient vendorDimensionOpenClient;

    @Override
    public TableDataInfo<CeDimensionRecordVo> queryPageList(CeDimensionRecordBo bo, PageQuery pageQuery) {
        validateDimensionCode(bo.getDimensionCode());
        if (isVendorOnlyDimension(bo.getDimensionCode())) {
            return queryVendorPageList(bo, pageQuery);
        }
        LambdaQueryWrapper<CeDimensionRecord> wrapper = buildQueryWrapper(bo)
            .orderByAsc(CeDimensionRecord::getSortOrder)
            .orderByAsc(CeDimensionRecord::getId);
        IPage<CeDimensionRecordVo> page = dimensionRecordMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeDimensionRecordVo> queryList(CeDimensionRecordBo bo) {
        validateDimensionCode(bo.getDimensionCode());
        if (isVendorOnlyDimension(bo.getDimensionCode())) {
            return queryVendorPageList(bo, new PageQuery(Integer.MAX_VALUE, 1)).getRows();
        }
        return dimensionRecordMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeDimensionRecord::getSortOrder)
            .orderByAsc(CeDimensionRecord::getId));
    }

    @Override
    public CeDimensionRecordVo queryById(Long id) {
        CeDimensionRecordVo record = dimensionRecordMapper.selectVoById(id);
        if (record != null && isVendorOnlyDimension(record.getDimensionCode())) {
            throw new ServiceException("Vendor-owned dimension records must be queried through vendor open APIs");
        }
        return record;
    }

    @Override
    public Boolean insertByBo(CeDimensionRecordBo bo) {
        validateDimensionCode(bo.getDimensionCode());
        assertEnterpriseWritable(bo.getDimensionCode());
        CeDimensionRecord add = MapstructUtils.convert(bo, CeDimensionRecord.class);
        boolean flag = dimensionRecordMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeDimensionRecordBo bo) {
        validateDimensionCode(bo.getDimensionCode());
        assertEnterpriseWritable(bo.getDimensionCode());
        CeDimensionRecord existing = requireLocalRecord(bo.getId());
        assertEnterpriseWritable(existing.getDimensionCode());
        CeDimensionRecord update = MapstructUtils.convert(bo, CeDimensionRecord.class);
        return dimensionRecordMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        for (Long id : ids) {
            CeDimensionRecord existing = requireLocalRecord(id);
            assertEnterpriseWritable(existing.getDimensionCode());
        }
        return dimensionRecordMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeDimensionRecord> buildQueryWrapper(CeDimensionRecordBo bo) {
        return new LambdaQueryWrapper<CeDimensionRecord>()
            .eq(StringUtils.isNotBlank(bo.getDimensionCode()), CeDimensionRecord::getDimensionCode, bo.getDimensionCode())
            .notIn(StringUtils.isBlank(bo.getDimensionCode()), CeDimensionRecord::getDimensionCode, VENDOR_ONLY_DIMENSION_CODES)
            .like(StringUtils.isNotBlank(bo.getRecordCode()), CeDimensionRecord::getRecordCode, bo.getRecordCode())
            .like(StringUtils.isNotBlank(bo.getRecordName()), CeDimensionRecord::getRecordName, bo.getRecordName())
            .eq(StringUtils.isNotBlank(bo.getParentCode()), CeDimensionRecord::getParentCode, bo.getParentCode())
            .eq(StringUtils.isNotBlank(bo.getStatus()), CeDimensionRecord::getStatus, bo.getStatus());
    }

    private void validateDimensionCode(String dimensionCode) {
        if (StringUtils.isBlank(dimensionCode)) {
            return;
        }
        if (!ALLOWED_DIMENSION_CODES.contains(dimensionCode)) {
            throw new ServiceException("Unsupported enterprise dimension code: " + dimensionCode);
        }
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
            throw new ServiceException("vendor dimension response is incomplete");
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
        target.setField01(source.getField01());
        target.setField02(source.getField02());
        target.setField03(source.getField03());
        target.setField04(source.getField04());
        target.setField05(source.getField05());
        target.setField06(source.getField06());
        target.setSortOrder(source.getSortOrder());
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setRemark(source.getRemark());
        return target;
    }

    private CeLicenseState requireCurrentLicense() {
        List<CeLicenseState> states = licenseStateMapper.selectList(new LambdaQueryWrapper<CeLicenseState>()
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

    private CeDimensionRecord requireLocalRecord(Long id) {
        if (id == null) {
            throw new ServiceException("dimension record id cannot be null");
        }
        CeDimensionRecord existing = dimensionRecordMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("dimension record does not exist");
        }
        return existing;
    }

    private void assertEnterpriseWritable(String dimensionCode) {
        if (isVendorOnlyDimension(dimensionCode)) {
            throw new ServiceException("Vendor-owned dimension data must be maintained in vendor backend");
        }
    }

    private boolean isVendorOnlyDimension(String dimensionCode) {
        return VENDOR_ONLY_DIMENSION_CODES.contains(dimensionCode);
    }
}
