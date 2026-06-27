package org.dromara.carbon.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.client.CeVendorReportTemplateOpenClient;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.report.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.report.domain.CeReportTemplateSyncResponse;
import org.dromara.carbon.enterprise.report.domain.CeVendorReportTemplateDownloadResponse;
import org.dromara.carbon.enterprise.report.domain.CeVendorReportTemplateListResponse;
import org.dromara.carbon.enterprise.report.domain.CeVendorReportTemplateRecord;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.report.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.shared.service.ICeReportTemplateSyncService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

/**
 * Enterprise report template sync service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeReportTemplateSyncServiceImpl implements ICeReportTemplateSyncService {

    private static final String LICENSE_STATUS_VALID = "VALID";
    private static final String TEMPLATE_TYPE_VENDOR = "vendor";
    private static final String DEFAULT_TEMPLATE_ROOT = "enterprise/report-templates";

    private final CeLicenseStateMapper licenseStateMapper;
    private final CeReportTemplateFileMapper reportTemplateFileMapper;
    private final CeVendorReportTemplateOpenClient vendorReportTemplateOpenClient;

    @Value("${carbon.enterprise.report-template-root:" + DEFAULT_TEMPLATE_ROOT + "}")
    private String reportTemplateRoot = DEFAULT_TEMPLATE_ROOT;

    @Override
    public CeReportTemplateSyncResponse syncCurrentLicenseReportTemplates() {
        CeLicenseState license = requireCurrentLicense();
        CeVendorReportTemplateListResponse listResponse = vendorReportTemplateOpenClient.listTemplates(
            license.getLicenseId(),
            license.getInstallId()
        );
        validateListResponse(license, listResponse);
        List<MaterializedTemplate> templates = listResponse.getTemplates().stream()
            .map(template -> materializeTemplate(downloadTemplate(license, template)))
            .toList();

        Date syncedTime = new Date();
        for (MaterializedTemplate template : templates) {
            upsertTemplateFile(template, syncedTime);
        }

        CeReportTemplateSyncResponse response = new CeReportTemplateSyncResponse();
        response.setLicenseId(license.getLicenseId());
        response.setTemplateCount(templates.size());
        response.setSyncedTime(syncedTime);
        return response;
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

    private CeVendorReportTemplateDownloadResponse downloadTemplate(
        CeLicenseState license,
        CeVendorReportTemplateRecord template
    ) {
        if (template == null || template.getTemplateId() == null) {
            throw new ServiceException("vendor report template list response is incomplete");
        }
        CeVendorReportTemplateDownloadResponse download = vendorReportTemplateOpenClient.downloadTemplate(
            template.getTemplateId(),
            license.getLicenseId(),
            license.getInstallId()
        );
        validateDownloadResponse(license, template, download);
        return download;
    }

    private MaterializedTemplate materializeTemplate(CeVendorReportTemplateDownloadResponse download) {
        validateDownloadToken(download);
        byte[] content = vendorReportTemplateOpenClient.downloadTemplateFile(download.getDownloadToken());
        if (content == null || content.length == 0) {
            throw new ServiceException("vendor report template file content is empty");
        }
        Path root = resolveTemplateRoot();
        String fileName = safeFileName(download.getFileName());
        String relativePath = "vendor/" + safeFileName(download.getTemplateCode())
            + "-" + download.getTemplateId() + "-" + fileName;
        Path targetPath = root.resolve(relativePath).normalize();
        if (!targetPath.startsWith(root)) {
            throw new ServiceException("report template target path is outside template root");
        }
        try {
            Files.createDirectories(targetPath.getParent());
            writeAtomically(targetPath, content);
        } catch (IOException ex) {
            throw new ServiceException("report template file cannot be materialized locally");
        }
        return new MaterializedTemplate(download, relativePath);
    }

    private void writeAtomically(Path targetPath, byte[] content) throws IOException {
        Path parent = targetPath.getParent();
        String fileName = tempFilePrefix(targetPath.getFileName().toString());
        Path tempFile = Files.createTempFile(parent, fileName, ".tmp");
        try {
            Files.write(tempFile, content);
            try {
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String tempFilePrefix(String fileName) {
        return fileName.length() >= 3 ? fileName : "tpl-" + fileName;
    }

    private void upsertTemplateFile(MaterializedTemplate template, Date syncedTime) {
        CeVendorReportTemplateDownloadResponse download = template.download();
        CeReportTemplateFile file = reportTemplateFileMapper.selectOne(Wrappers.<CeReportTemplateFile>lambdaQuery()
            .eq(CeReportTemplateFile::getTemplateCode, download.getTemplateCode()), false);
        if (file == null) {
            file = new CeReportTemplateFile();
            copyTemplate(download, template.localPath(), file, syncedTime);
            reportTemplateFileMapper.insert(file);
        } else {
            copyTemplate(download, template.localPath(), file, syncedTime);
            reportTemplateFileMapper.updateById(file);
        }
    }

    private void copyTemplate(CeVendorReportTemplateDownloadResponse source, String localPath,
                              CeReportTemplateFile target, Date syncedTime) {
        target.setTemplateCode(source.getTemplateCode());
        target.setTemplateName(source.getTemplateName());
        target.setTemplateType(TEMPLATE_TYPE_VENDOR);
        target.setFileName(source.getFileName());
        target.setFilePath(localPath);
        target.setEnabledFlag(Boolean.TRUE);
        target.setUpdateTime(syncedTime);
        target.setRemark("synced from vendor template " + source.getTemplateId() + " version " + source.getTemplateVersion());
    }

    private void validateListResponse(CeLicenseState license, CeVendorReportTemplateListResponse response) {
        if (response == null
            || !license.getLicenseId().equals(response.getLicenseId())
            || response.getTemplates() == null) {
            throw new ServiceException("vendor report template list response is incomplete");
        }
    }

    private void validateDownloadResponse(
        CeLicenseState license,
        CeVendorReportTemplateRecord template,
        CeVendorReportTemplateDownloadResponse response
    ) {
        if (response == null
            || !license.getLicenseId().equals(response.getLicenseId())
            || !template.getTemplateId().equals(response.getTemplateId())
            || StringUtils.isBlank(response.getTemplateCode())
            || StringUtils.isBlank(response.getTemplateName())
            || StringUtils.isBlank(response.getFileName())) {
            throw new ServiceException("vendor report template download response is incomplete");
        }
    }

    private void validateDownloadToken(CeVendorReportTemplateDownloadResponse response) {
        if (response == null || StringUtils.isBlank(response.getDownloadToken())) {
            throw new ServiceException("vendor report template download token is missing");
        }
    }

    private Path resolveTemplateRoot() {
        if (StringUtils.isBlank(reportTemplateRoot)) {
            throw new ServiceException("report template root is invalid");
        }
        try {
            Path configuredRoot = Path.of(reportTemplateRoot.trim());
            if (configuredRoot.isAbsolute()) {
                return configuredRoot.normalize();
            }
            return Path.of("").toAbsolutePath().normalize().resolve(configuredRoot).normalize();
        } catch (InvalidPathException ex) {
            throw new ServiceException("report template root is invalid");
        }
    }

    private String safeFileName(String value) {
        if (StringUtils.isBlank(value)) {
            throw new ServiceException("report template file name is invalid");
        }
        String safe = value.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (StringUtils.isBlank(safe) || ".".equals(safe) || "..".equals(safe)) {
            throw new ServiceException("report template file name is invalid");
        }
        return safe;
    }

    private record MaterializedTemplate(CeVendorReportTemplateDownloadResponse download, String localPath) {
    }
}
