package org.dromara.carbon.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.report.domain.CeReportContent;
import org.dromara.carbon.enterprise.report.domain.CeReportContentSyncResponse;
import org.dromara.carbon.enterprise.report.domain.vo.CeReportContentVo;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.report.mapper.CeReportContentMapper;
import org.dromara.carbon.enterprise.shared.service.ICeReportContentService;
import org.dromara.carbon.enterprise.vendor.client.CeVendorReportContentOpenClient;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorReportContentListResponse;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorReportContentRecord;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Enterprise local report content catalog service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeReportContentServiceImpl implements ICeReportContentService {

    private static final String LICENSE_STATUS_VALID = "VALID";

    private final CeLicenseStateMapper licenseStateMapper;
    private final CeReportContentMapper reportContentMapper;
    private final CeVendorReportContentOpenClient vendorReportContentOpenClient;

    @Override
    public List<CeReportContentVo> listContent() {
        if (reportContentMapper.selectCount(Wrappers.lambdaQuery(CeReportContent.class)) == 0) {
            syncContent();
        }
        return reportContentMapper.selectVoList(new LambdaQueryWrapper<CeReportContent>()
            .orderByAsc(CeReportContent::getDisplayOrder)
            .orderByAsc(CeReportContent::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CeReportContentSyncResponse syncContent() {
        CeLicenseState license = requireCurrentLicense();
        CeVendorReportContentListResponse vendorResponse = vendorReportContentOpenClient.listContents(
            license.getLicenseId(), license.getInstallId());
        validateVendorResponse(license, vendorResponse);

        Date syncedTime = new Date();
        reportContentMapper.delete(Wrappers.lambdaQuery(CeReportContent.class));
        for (CeVendorReportContentRecord record : vendorResponse.getContents()) {
            CeReportContent entity = new CeReportContent();
            entity.setDirectoryNo(record.getDirectoryNo());
            entity.setDirectoryName(record.getDirectoryName());
            entity.setSubdirectoryNo(record.getSubdirectoryNo());
            entity.setSubdirectoryName(record.getSubdirectoryName());
            entity.setChartNames(record.getChartNames());
            entity.setDisplayOrder(record.getDisplayOrder());
            entity.setCreateTime(syncedTime);
            entity.setUpdateTime(syncedTime);
            entity.setRemark(record.getRemark());
            reportContentMapper.insert(entity);
        }

        CeReportContentSyncResponse response = new CeReportContentSyncResponse();
        response.setLicenseId(license.getLicenseId());
        response.setContentCount(vendorResponse.getContents().size());
        response.setSyncedTime(syncedTime);
        return response;
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

    private void validateVendorResponse(CeLicenseState license, CeVendorReportContentListResponse response) {
        if (response == null
            || !license.getLicenseId().equals(response.getLicenseId())
            || response.getContents() == null) {
            throw new ServiceException("厂商报表内容目录响应不完整");
        }
    }
}
