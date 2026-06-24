package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.sourcea.CeSourceAImportResult;
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
