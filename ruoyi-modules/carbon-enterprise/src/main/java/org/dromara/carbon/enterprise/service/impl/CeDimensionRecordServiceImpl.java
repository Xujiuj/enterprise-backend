package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.client.CeVendorDimensionOpenClient;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionListResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorDimensionRecord;
import org.dromara.carbon.enterprise.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.ICeDimensionRecordService;
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
        "intensity-tolerance",
        "report-template-download"
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
        if (!"report-template-download".equals(bo.getDimensionCode())) {
            return queryLocalProjectionPageList(bo, pageQuery);
        }
        return queryVendorPageList(bo, pageQuery);
    }

    @Override
    public List<CeDimensionRecordVo> queryList(CeDimensionRecordBo bo) {
        validateDimensionCode(bo.getDimensionCode());
        if (!"report-template-download".equals(bo.getDimensionCode())) {
            return filterProjectionRows(bo, dimensionProjectionMapper.selectByDimensionCode(bo.getDimensionCode()));
        }
        return queryVendorPageList(bo, new PageQuery(Integer.MAX_VALUE, 1)).getRows();
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
        return dimensionProjectionMapper.insertByDimensionCode(bo) > 0;
    }

    @Override
    public Boolean updateByBo(CeDimensionRecordBo bo) {
        validateEditableDimensionCode(bo.getDimensionCode());
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
        List<CeDimensionRecordVo> rows = filterProjectionRows(bo, dimensionProjectionMapper.selectByDimensionCode(bo.getDimensionCode()));
        int pageNum = pageQuery == null || pageQuery.getPageNum() == null ? 1 : pageQuery.getPageNum();
        int pageSize = pageQuery == null || pageQuery.getPageSize() == null ? rows.size() : pageQuery.getPageSize();
        int fromIndex = Math.min(Math.max(pageNum - 1, 0) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new TableDataInfo<>(rows.subList(fromIndex, toIndex), rows.size());
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
        target.setField01(source.getField01());
        target.setField02(source.getField02());
        target.setField03(source.getField03());
        target.setField04(source.getField04());
        target.setField05(source.getField05());
        target.setField06(source.getField06());
        target.setField07(source.getField07());
        target.setField08(source.getField08());
        target.setField09(source.getField09());
        target.setField10(source.getField10());
        target.setField11(source.getField11());
        target.setField12(source.getField12());
        target.setField13(source.getField13());
        target.setField14(source.getField14());
        target.setField15(source.getField15());
        target.setField16(source.getField16());
        target.setField17(source.getField17());
        target.setField18(source.getField18());
        target.setField19(source.getField19());
        target.setField20(source.getField20());
        target.setField21(source.getField21());
        target.setField22(source.getField22());
        Integer sortOrder = source.getSortOrder();
        target.setSortOrder(sortOrder == null ? null : String.valueOf(sortOrder));
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
