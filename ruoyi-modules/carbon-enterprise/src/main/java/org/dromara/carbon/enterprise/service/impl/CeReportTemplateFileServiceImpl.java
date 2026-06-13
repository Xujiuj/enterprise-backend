package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.domain.bo.CeReportTemplateFileBo;
import org.dromara.carbon.enterprise.domain.vo.CeReportTemplateFileVo;
import org.dromara.carbon.enterprise.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.service.ICeReportTemplateFileService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;

/**
 * Enterprise local report template download catalog service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeReportTemplateFileServiceImpl implements ICeReportTemplateFileService {

    private static final String DEFAULT_TEMPLATE_ROOT = "enterprise/report-templates";

    private final CeReportTemplateFileMapper reportTemplateFileMapper;

    @Value("${carbon.enterprise.report-template-root:" + DEFAULT_TEMPLATE_ROOT + "}")
    private String reportTemplateRoot = DEFAULT_TEMPLATE_ROOT;

    @Override
    public TableDataInfo<CeReportTemplateFileVo> queryPageList(CeReportTemplateFileBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeReportTemplateFile> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeReportTemplateFile::getCreateTime)
            .orderByDesc(CeReportTemplateFile::getId);
        IPage<CeReportTemplateFileVo> page = reportTemplateFileMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeReportTemplateFileVo> queryList(CeReportTemplateFileBo bo) {
        return reportTemplateFileMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeReportTemplateFile::getTemplateCode)
            .orderByAsc(CeReportTemplateFile::getId));
    }

    @Override
    public CeReportTemplateFileVo queryById(Long id) {
        return reportTemplateFileMapper.selectVoById(id);
    }

    @Override
    public void download(Long id, HttpServletResponse response) throws IOException {
        ReportTemplateDownload download = resolveDownload(id);
        if (!Files.isRegularFile(download.path(), LinkOption.NOFOLLOW_LINKS)) {
            throw new ServiceException("report template file path must point to a file");
        }
        FileUtils.setAttachmentResponseHeader(response, download.downloadFileName());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
        response.setContentLengthLong(Files.size(download.path()));
        try (InputStream inputStream = Files.newInputStream(download.path(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    @Override
    public Boolean insertByBo(CeReportTemplateFileBo bo) {
        CeReportTemplateFile add = MapstructUtils.convert(bo, CeReportTemplateFile.class);
        if (add.getEnabledFlag() == null) {
            add.setEnabledFlag(Boolean.TRUE);
        }
        boolean flag = reportTemplateFileMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeReportTemplateFileBo bo) {
        CeReportTemplateFile update = MapstructUtils.convert(bo, CeReportTemplateFile.class);
        return reportTemplateFileMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return reportTemplateFileMapper.deleteByIds(ids) > 0;
    }

    private ReportTemplateDownload resolveDownload(Long id) {
        CeReportTemplateFile record = reportTemplateFileMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("report template file record does not exist");
        }
        if (!Boolean.TRUE.equals(record.getEnabledFlag())) {
            throw new ServiceException("report template file is disabled");
        }
        if (StringUtils.isBlank(record.getFilePath())) {
            throw new ServiceException("report template file path cannot be blank");
        }

        Path templateRoot = resolveTemplateRoot();
        Path normalizedPath = resolveTemplatePath(record.getFilePath(), templateRoot);
        if (Files.isDirectory(normalizedPath)) {
            throw new ServiceException("report template file path must point to a file");
        }
        if (!Files.exists(normalizedPath)) {
            throw new ServiceException("report template file does not exist");
        }
        if (!Files.isReadable(normalizedPath)) {
            throw new ServiceException("report template file is not readable");
        }
        Path realFile = resolveRealPathWithinTemplateRoot(templateRoot, normalizedPath);
        if (!Files.isRegularFile(realFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new ServiceException("report template file path must point to a file");
        }

        String physicalFileName = realFile.getFileName() == null ? "" : realFile.getFileName().toString();
        String downloadFileName = StringUtils.isNotBlank(record.getFileName()) ? record.getFileName().trim() : physicalFileName;
        if (StringUtils.isBlank(downloadFileName)) {
            throw new ServiceException("report template download file name cannot be resolved");
        }
        return new ReportTemplateDownload(realFile, downloadFileName);
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

    private Path resolveTemplatePath(String rawPath, Path templateRoot) {
        try {
            Path configuredRoot = Path.of(reportTemplateRoot.trim());
            Path configuredRelativeRoot = configuredRoot.isAbsolute() ? null : configuredRoot.normalize();
            Path configuredPath = Path.of(rawPath.trim());
            Path normalizedPath;
            if (configuredPath.isAbsolute()) {
                normalizedPath = configuredPath.normalize();
            } else {
                Path relativePath = configuredPath.normalize();
                if (configuredRelativeRoot != null && relativePath.startsWith(configuredRelativeRoot)) {
                    relativePath = configuredRelativeRoot.relativize(relativePath);
                }
                normalizedPath = templateRoot.resolve(relativePath).normalize();
            }
            if (!normalizedPath.startsWith(templateRoot)) {
                throw new ServiceException("report template file path is outside template root");
            }
            return normalizedPath;
        } catch (InvalidPathException ex) {
            throw new ServiceException("report template file path is invalid");
        }
    }

    private Path resolveRealPathWithinTemplateRoot(Path templateRoot, Path normalizedPath) {
        try {
            Path realRoot = templateRoot.toRealPath();
            Path realFile = normalizedPath.toRealPath();
            if (!realFile.startsWith(realRoot)) {
                throw new ServiceException("report template file path is outside template root");
            }
            return realFile;
        } catch (ServiceException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ServiceException("report template file path is outside template root");
        }
    }

    private LambdaQueryWrapper<CeReportTemplateFile> buildQueryWrapper(CeReportTemplateFileBo bo) {
        return new LambdaQueryWrapper<CeReportTemplateFile>()
            .like(StringUtils.isNotBlank(bo.getTemplateCode()), CeReportTemplateFile::getTemplateCode, bo.getTemplateCode())
            .like(StringUtils.isNotBlank(bo.getTemplateName()), CeReportTemplateFile::getTemplateName, bo.getTemplateName())
            .eq(StringUtils.isNotBlank(bo.getTemplateType()), CeReportTemplateFile::getTemplateType, bo.getTemplateType())
            .like(StringUtils.isNotBlank(bo.getFileName()), CeReportTemplateFile::getFileName, bo.getFileName())
            .eq(bo.getEnabledFlag() != null, CeReportTemplateFile::getEnabledFlag, bo.getEnabledFlag());
    }

    private record ReportTemplateDownload(Path path, String downloadFileName) {
    }
}
