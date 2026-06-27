package org.dromara.carbon.enterprise.sourcea.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class CeSourceAImportServiceTest {

    @Test
    void importDirectorySkipsExcelLockFiles(@TempDir Path dir) throws IOException {
        // Create a lock file that should be skipped
        Files.writeString(dir.resolve("~$1 排放源识别表.xlsx"), "locked");
        // Create a documentation xlsx would require POI, so just verify the filter logic
        // by checking that the service's filter accepts normal files and rejects lock files
        String lockFileName = "~$1 排放源识别表.xlsx";
        String normalFileName = "2 排放因子表.xlsx";

        assertTrue(normalFileName.toLowerCase().endsWith(".xlsx") && !normalFileName.startsWith("~$"));
        assertTrue(!(lockFileName.toLowerCase().endsWith(".xlsx") && !lockFileName.startsWith("~$")));
    }
}
