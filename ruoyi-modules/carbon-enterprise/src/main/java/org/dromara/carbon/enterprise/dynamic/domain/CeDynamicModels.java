package org.dromara.carbon.enterprise.dynamic.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transport models for Excel-driven dynamic management modules.
 */
public final class CeDynamicModels {

    private CeDynamicModels() {
    }

    @Data
    public static class WorkbookPreview implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String fileName;
        private List<SheetDefinition> sheets = new ArrayList<>();
    }

    @Data
    public static class GenerateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<SheetDefinition> sheets = new ArrayList<>();
    }

    @Data
    public static class SheetDefinition implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer sheetNo;
        private String sheetName;
        private String moduleName;
        private String moduleCode;
        private Boolean selected = true;
        private Integer rowCount = 0;
        private List<FieldDefinition> fields = new ArrayList<>();
        private List<Map<String, Object>> sampleRows = new ArrayList<>();
    }

    @Data
    public static class FieldDefinition implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String fieldCode;
        private String fieldName;
        private String dbColumn;
        private String valueType;
        private String uiType;
        private Boolean required = false;
        private Boolean searchable = false;
        private Boolean listVisible = true;
        private Boolean formVisible = true;
        private Integer sortOrder;
        private Integer maxLength = 255;
        private Integer precision = 30;
        private Integer scale = 10;
    }

    @Data
    public static class ModuleSchema implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private String moduleCode;
        private String moduleName;
        private String tableName;
        private String sheetName;
        private String permissionPrefix;
        private Long menuId;
        private String status;
        private List<FieldDefinition> fields = new ArrayList<>();
    }

    @Data
    public static class GenerateResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<ModuleSchema> modules = new ArrayList<>();
    }

    public record ParsedSheet(Integer sheetNo, String sheetName, List<String> headers,
                              List<Map<Integer, String>> rows) {
    }

    public static Map<String, Object> orderedRow() {
        return new LinkedHashMap<>();
    }
}
