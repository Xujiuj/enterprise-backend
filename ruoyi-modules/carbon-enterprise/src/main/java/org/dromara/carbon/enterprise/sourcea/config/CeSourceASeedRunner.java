package org.dromara.carbon.enterprise.sourcea.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.sourcea.domain.CeSourceAImportResult;
import org.dromara.carbon.enterprise.shared.service.ICeSourceAImportService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs Source(A) reference-data initialization only when explicitly enabled.
 *
 * <p>The Excel files are an operational seed input, not an application runtime data source:
 * normal pages and imports read/write enterprise SQL Server after this one-shot load.</p>
 */
@Slf4j
@Order(20)
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(CeSourceASeedProperties.class)
@ConditionalOnProperty(prefix = "carbon.enterprise.source-a.seed", name = "enabled", havingValue = "true")
public class CeSourceASeedRunner implements ApplicationRunner {

    private final CeSourceASeedProperties properties;
    private final ICeSourceAImportService sourceAImportService;

    @Override
    public void run(ApplicationArguments args) {
        if (StringUtils.isBlank(properties.getDirectory())) {
            log.warn("[SourceASeed] skipped: carbon.enterprise.source-a.seed.directory is blank");
            return;
        }
        CeSourceAImportResult result = sourceAImportService.importDirectory(properties.getDirectory());
        log.info("[SourceASeed] imported={}, mode={}, workbooks={}, sheets={}, sourceRows={}, tableRows={}, warnings={}, errors={}",
            result.isImported(), result.getSourceMode(), result.getWorkbookCount(), result.getSheetCount(),
            result.getSourceRowCount(), result.getTableRows(), result.getWarnings(), result.getErrors());
        if (!result.isImported()) {
            throw new IllegalStateException("Source(A) seed failed: " + result.getErrors());
        }
    }
}
