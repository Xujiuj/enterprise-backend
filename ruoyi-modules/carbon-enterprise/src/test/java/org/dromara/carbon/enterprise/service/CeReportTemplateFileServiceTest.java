package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.service.impl.CeReportTemplateFileServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Assumptions;
import org.dromara.common.core.utils.file.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeReportTemplateFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadsControlledRelativePathUsingConfiguredDownloadName() throws Exception {
        Path templateRoot = controlledRoot();
        Path file = Files.writeString(templateRoot.resolve("greenhouse-gas-inventory-template.xlsx"), "xlsx-binary-content");

        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(1L)).thenReturn(record(
            1L,
            "catalog-template.xlsx",
            "enterprise/report-templates/greenhouse-gas-inventory-template.xlsx",
            true
        ));

        CeReportTemplateFileServiceImpl service = newService(mapper, "enterprise/report-templates");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<Path> paths = mockStatic(Path.class, CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of("")).thenReturn(tempDir);
            service.download(1L, response);
        }

        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
        assertThat(response.getHeader("download-filename")).isEqualTo(FileUtils.percentEncode("catalog-template.xlsx"));
        assertThat(response.getContentAsByteArray()).isEqualTo(Files.readAllBytes(file));
        assertThat(response.getContentLengthLong()).isEqualTo(Files.size(file));
    }

    @Test
    void fallsBackToPhysicalFileNameWhenConfiguredFileNameIsBlank() throws Exception {
        Path templateRoot = controlledRoot();
        Path file = Files.writeString(templateRoot.resolve("physical-template.pbix"), "pbix");

        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(2L)).thenReturn(record(2L, " ", file.toString(), true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.download(2L, response);

        assertThat(response.getHeader("download-filename")).isEqualTo(FileUtils.percentEncode("physical-template.pbix"));
    }

    @Test
    void rejectsMissingRecord() {
        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(3L)).thenReturn(null);

        CeReportTemplateFileServiceImpl service = newService(mapper, tempDir.resolve("enterprise/report-templates").toString());

        assertThatThrownBy(() -> service.download(3L, new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("report template file record does not exist");
    }

    @Test
    void rejectsDisabledRecord() {
        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(4L)).thenReturn(record(4L, "template.pbix", tempDir.resolve("template.pbix").toString(), false));

        CeReportTemplateFileServiceImpl service = newService(mapper, tempDir.resolve("enterprise/report-templates").toString());

        assertThatThrownBy(() -> service.download(4L, new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("report template file is disabled");
    }

    @Test
    void rejectsBlankFilePath() {
        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(5L)).thenReturn(record(5L, "template.pbix", " ", true));

        CeReportTemplateFileServiceImpl service = newService(mapper, tempDir.resolve("enterprise/report-templates").toString());

        assertThatThrownBy(() -> service.download(5L, new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("report template file path cannot be blank");
    }

    @Test
    void rejectsDirectoryPath() throws Exception {
        Path templateRoot = controlledRoot();
        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(6L)).thenReturn(record(6L, "template.pbix", templateRoot.toString(), true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());

        assertThatThrownBy(() -> service.download(6L, new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("report template file path must point to a file");
    }

    @Test
    void rejectsMissingPhysicalFile() throws Exception {
        Path templateRoot = controlledRoot();
        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(7L)).thenReturn(record(7L, "template.pbix", templateRoot.resolve("missing.pbix").toString(), true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());

        assertThatThrownBy(() -> service.download(7L, new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("report template file does not exist");
    }

    @Test
    void rejectsUnreadablePhysicalFile() throws Exception {
        Path templateRoot = controlledRoot();
        Path file = Files.writeString(templateRoot.resolve("unreadable.pbix"), "pbix");
        Path normalizedPath = file.toAbsolutePath().normalize();

        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(8L)).thenReturn(record(8L, "template.pbix", file.toString(), true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());

        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.isReadable(normalizedPath)).thenReturn(false);

            assertThatThrownBy(() -> service.download(8L, new MockHttpServletResponse()))
                .isInstanceOf(ServiceException.class)
                .hasMessage("report template file is not readable");
        }
    }

    @Test
    void rejectsInvalidPath() throws Exception {
        String invalidPath = "invalid-path-for-test";
        Path templateRoot = controlledRoot();
        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(9L)).thenReturn(record(9L, "template.pbix", invalidPath, true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());

        try (MockedStatic<Path> paths = mockStatic(Path.class, CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of(invalidPath)).thenThrow(new InvalidPathException(invalidPath, "invalid for test"));

            assertThatThrownBy(() -> service.download(9L, new MockHttpServletResponse()))
                .isInstanceOf(ServiceException.class)
                .hasMessage("report template file path is invalid");
        }
    }

    @Test
    void rejectsPathOutsideTemplateRoot() throws Exception {
        Path templateRoot = controlledRoot();
        Path externalFile = Files.writeString(tempDir.resolve("outside-template.pbix"), "outside");

        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(10L)).thenReturn(record(10L, "template.pbix", externalFile.toString(), true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());

        assertThatThrownBy(() -> service.download(10L, new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("report template file path is outside template root");
    }

    @Test
    void rejectsSymlinkEscapingTemplateRoot() throws Exception {
        Path templateRoot = controlledRoot();
        Path externalFile = Files.writeString(tempDir.resolve("outside-linked-template.pbix"), "outside");
        Path symlink = templateRoot.resolve("linked-template.pbix");
        try {
            Files.createSymbolicLink(symlink, externalFile);
        } catch (UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false, "symbolic links are not supported on this platform");
        } catch (IOException ex) {
            Assumptions.assumeTrue(false, "symbolic links are not permitted in this environment");
        }

        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(11L)).thenReturn(record(11L, "template.pbix", symlink.toString(), true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());

        assertThatThrownBy(() -> service.download(11L, new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("report template file path is outside template root");
    }

    @Test
    void streamsResolvedRealFileInsteadOfOriginalSymlinkPath() throws Exception {
        Path templateRoot = controlledRoot();
        Path realFile = Files.writeString(templateRoot.resolve("actual-template.pbix"), "real-file-content");
        Path symlink = templateRoot.resolve("current-template.pbix");
        try {
            Files.createSymbolicLink(symlink, realFile.getFileName());
        } catch (UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false, "symbolic links are not supported on this platform");
        } catch (IOException ex) {
            Assumptions.assumeTrue(false, "symbolic links are not permitted in this environment");
        }

        CeReportTemplateFileMapper mapper = mock(CeReportTemplateFileMapper.class);
        when(mapper.selectById(12L)).thenReturn(record(12L, "template.pbix", symlink.toString(), true));

        CeReportTemplateFileServiceImpl service = newService(mapper, templateRoot.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.size(symlink)).thenThrow(new AssertionError("should not size symlink path"));
            files.when(() -> Files.newInputStream(symlink, StandardOpenOption.READ, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenThrow(new AssertionError("should not open symlink path"));

            service.download(12L, response);
        }

        assertThat(response.getContentAsByteArray()).isEqualTo("real-file-content".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getContentLengthLong()).isEqualTo(Files.size(realFile));
    }

    private Path controlledRoot() throws IOException {
        return Files.createDirectories(tempDir.resolve("enterprise").resolve("report-templates"));
    }

    private static CeReportTemplateFileServiceImpl newService(CeReportTemplateFileMapper mapper, String templateRoot) {
        CeReportTemplateFileServiceImpl service = new CeReportTemplateFileServiceImpl(mapper);
        ReflectionTestUtils.setField(service, "reportTemplateRoot", templateRoot);
        return service;
    }

    private static CeReportTemplateFile record(Long id, String fileName, String filePath, boolean enabledFlag) {
        CeReportTemplateFile record = new CeReportTemplateFile();
        record.setId(id);
        record.setFileName(fileName);
        record.setFilePath(filePath);
        record.setEnabledFlag(enabledFlag);
        return record;
    }
}
