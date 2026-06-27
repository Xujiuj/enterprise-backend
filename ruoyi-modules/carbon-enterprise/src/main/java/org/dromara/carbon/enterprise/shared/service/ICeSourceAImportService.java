package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.sourcea.domain.CeSourceAImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Composite Source(A) workbook import service.
 */
public interface ICeSourceAImportService {

    CeSourceAImportResult importFiles(List<MultipartFile> files);

    CeSourceAImportResult importDirectory(String sourceDirectory);

    CeSourceAImportResult validateCurrentData();
}
