package org.dromara.carbon.enterprise.activity;

import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldDescriptor;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656FieldValue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationIssue;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationRequest;
import org.dromara.carbon.enterprise.domain.activity.CeSheet656ValidationResult;
import org.dromara.carbon.enterprise.service.ICeSheet656DerivedFieldResolver;
import org.dromara.carbon.enterprise.service.ICeSheet656ValidationService;
import org.dromara.carbon.enterprise.service.impl.CeSheet656ValidationServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class CeSheet656ActivityValidationTest {

    private static final List<String> DERIVED_CODES = List.of(
        "f002", "f003", "f004", "f005", "f006", "f007", "f008", "f009", "f010", "f018"
    );

    @Test
    void rowLevelContractFreezesSheet656ColumnOrderNamesAndRequiredness() {
        CeSheet656ValidationServiceImpl service = new CeSheet656ValidationServiceImpl(fakeResolver());

        List<CeSheet656FieldDescriptor> fields = service.listFrozenFields();

        assertEquals(List.of(
            "f001:PK_排放源识别编号:false:true:false",
            "f002:FK_公司编号:false:false:true",
            "f003:公司名称:false:false:true",
            "f004:工厂:false:false:true",
            "f005:FK_排放源分类:false:false:true",
            "f006:范围:false:false:true",
            "f007:范围子类别:false:false:true",
            "f008:排放源识别:false:false:true",
            "f009:排放源:false:false:true",
            "f010:单位:false:false:true",
            "f011:年度:false:true:false",
            "f012:月份:false:true:false",
            "f013:日期:false:true:false",
            "f014:活动数据:false:true:false",
            "f015:负责部门:false:true:false",
            "f016:数据来源:false:true:false",
            "f017:备注:false:false:false",
            "f018:FK_排放因子:false:false:true"
        ), fields.stream()
            .map(field -> field.getSourceColumnCode() + ":" + field.getSourceColumnName() + ":"
                + field.isSourceRequired() + ":" + field.isRowValueRequired() + ":" + field.isDerivedField())
            .toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18),
            fields.stream().map(CeSheet656FieldDescriptor::getFieldOrder).toList());
    }

    @Test
    void rowLevelValidatorDoesNotTreatBlankF017AsRequired() {
        CeSheet656ValidationServiceImpl service = new CeSheet656ValidationServiceImpl(fakeResolver());

        CeSheet656ValidationResult result = service.validate(request(values -> values.put("f017", "")));

        assertTrue(result.isValid());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void validatorImplementationIsPlainClassWithNoProductionBeanRegistration() {
        assertFalse(CeSheet656ValidationServiceImpl.class.isAnnotationPresent(Service.class));
        assertFalse(CeSheet656ValidationServiceImpl.class.isAnnotationPresent(Component.class));
    }

    @Test
    void validatorUsesInjectedEnterpriseLocalResolverForDerivedFields() {
        ICeSheet656ValidationService service = new CeSheet656ValidationServiceImpl(fakeResolver());

        CeSheet656ValidationResult valid = service.validate(request(values -> {
        }));
        assertTrue(valid.isValid());
        assertEquals(expectedDerivedValues(), resolvedValueMap(valid));

        CeSheet656ValidationResult mismatch = service.validate(request(values -> {
            values.put("f003", "伪造公司");
            values.put("f018", "EF-FAKE");
        }));
        assertFalse(mismatch.isValid());
        assertEquals(List.of("f003", "f018"), issueColumns(mismatch, "DERIVED_FIELD_MISMATCH"));
        assertEquals(expectedDerivedValues(), resolvedValueMap(mismatch));
    }

    @Test
    void validRowPassesWithExactDerivedFieldChecks() {
        CeSheet656ValidationServiceImpl service = new CeSheet656ValidationServiceImpl(fakeResolver());

        CeSheet656ValidationResult result = service.validate(request(values -> {
        }));

        assertTrue(result.isValid());
        assertFalse(result.isBlocking());
        assertTrue(result.isDraftSavable());
        assertTrue(result.getIssues().isEmpty());
        assertEquals(expectedDerivedValues(), resolvedValueMap(result));
    }

    @Test
    void missingRequiredAndInvalidFieldsProduceExactBlockingErrors() {
        CeSheet656ValidationServiceImpl service = new CeSheet656ValidationServiceImpl(fakeResolver());

        CeSheet656ValidationResult result = service.validate(request(values -> {
            values.put("f001", "");
            values.put("f011", "2026");
            values.put("f012", "13");
            values.put("f013", "bad-date");
            values.put("f014", "0");
            values.put("f015", "");
        }));

        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertFalse(result.isDraftSavable());
        assertEquals(List.of(
            "REQUIRED_FIELD_MISSING:f001:PK_排放源识别编号",
            "REQUIRED_FIELD_MISSING:f015:负责部门",
            "INVALID_VALUE_DOMAIN:f012:月份",
            "INVALID_TYPE:f013:日期",
            "INVALID_VALUE_DOMAIN:f014:活动数据"
        ), issueSummaries(result));
    }

    @Test
    void weakWarningsDoNotBlockDraftSaveAndIdentifyServerFilledDerivedColumns() {
        CeSheet656ValidationServiceImpl service = new CeSheet656ValidationServiceImpl(fakeResolver());

        CeSheet656ValidationResult result = service.validate(request(values -> {
            DERIVED_CODES.forEach(code -> values.put(code, ""));
        }));

        assertTrue(result.isValid());
        assertFalse(result.isBlocking());
        assertTrue(result.isDraftSavable());
        assertEquals(DERIVED_CODES, issueColumns(result, "DERIVED_FIELD_SERVER_FILLED"));
        assertEquals(expectedDerivedValues(), resolvedValueMap(result));
        assertTrue(result.getIssues().stream().allMatch(issue -> "WARNING".equals(issue.getSeverity())));
    }

    @Test
    void clientProvidedDerivedFieldsAreNotTrusted() {
        CeSheet656ValidationServiceImpl service = new CeSheet656ValidationServiceImpl(fakeResolver());

        CeSheet656ValidationResult result = service.validate(request(values -> {
            values.put("f003", "伪造公司");
            values.put("f018", "EF-FAKE");
        }));

        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(List.of("f003", "f018"), issueColumns(result, "DERIVED_FIELD_MISMATCH"));
        assertEquals(7, firstIssue(result, "DERIVED_FIELD_MISMATCH").getRowNumber());
        assertEquals("公司名称", firstIssue(result, "DERIVED_FIELD_MISMATCH").getSourceColumnName());
        assertEquals(expectedDerivedValues(), resolvedValueMap(result));
    }

    @Test
    void missingMasterDataMatchBlocksRow() {
        CeSheet656ValidationServiceImpl service = new CeSheet656ValidationServiceImpl(code -> Optional.empty());

        CeSheet656ValidationResult result = service.validate(request(values -> {
        }));

        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(List.of("MASTER_DATA_NOT_FOUND:f001:PK_排放源识别编号"), issueSummaries(result));
        assertTrue(result.getResolvedDerivedFieldValues().isEmpty());
    }

    private List<String> issueSummaries(CeSheet656ValidationResult result) {
        return result.getIssues().stream()
            .map(issue -> issue.getCode() + ":" + issue.getSourceColumnCode() + ":" + issue.getSourceColumnName())
            .toList();
    }

    private List<String> issueColumns(CeSheet656ValidationResult result, String issueCode) {
        return result.getIssues().stream()
            .filter(issue -> issueCode.equals(issue.getCode()))
            .map(CeSheet656ValidationIssue::getSourceColumnCode)
            .toList();
    }

    private CeSheet656ValidationIssue firstIssue(CeSheet656ValidationResult result, String issueCode) {
        return result.getIssues().stream()
            .filter(issue -> issueCode.equals(issue.getCode()))
            .findFirst()
            .orElseThrow();
    }

    private Map<String, String> resolvedValueMap(CeSheet656ValidationResult result) {
        return result.getResolvedDerivedFieldValues().stream()
            .collect(Collectors.toMap(CeSheet656FieldValue::getSourceColumnCode, CeSheet656FieldValue::getValue,
                (left, right) -> right, LinkedHashMap::new));
    }

    private Map<String, String> expectedDerivedValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("f002", "COMP-001");
        values.put("f003", "测试公司");
        values.put("f004", "一厂");
        values.put("f005", "CAT-001");
        values.put("f006", "范围1");
        values.put("f007", "固定燃烧");
        values.put("f008", "天然气锅炉");
        values.put("f009", "天然气");
        values.put("f010", "Nm3");
        values.put("f018", "EF-2026-001");
        return values;
    }

    private CeSheet656ValidationRequest request(Consumer<Map<String, String>> customizer) {
        Map<String, String> values = baseValues();
        customizer.accept(values);
        CeSheet656ValidationRequest request = new CeSheet656ValidationRequest();
        request.setRowNumber(7);
        request.setFieldValues(values.entrySet().stream()
            .map(entry -> field(entry.getKey(), entry.getValue()))
            .toList());
        return request;
    }

    private Map<String, String> baseValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("f001", "SRC-001");
        values.putAll(expectedDerivedValues());
        values.put("f011", "2026");
        values.put("f012", "6");
        values.put("f013", "2026-06-05");
        values.put("f014", "12.5");
        values.put("f015", "生产部");
        values.put("f016", "计量台账");
        values.put("f017", "正常记录");
        return values;
    }

    private CeSheet656FieldValue field(String code, String value) {
        CeSheet656FieldValue fieldValue = new CeSheet656FieldValue();
        fieldValue.setSourceColumnCode(code);
        fieldValue.setValue(value);
        return fieldValue;
    }

    private ICeSheet656DerivedFieldResolver fakeResolver() {
        return code -> {
            if (!"SRC-001".equals(code)) {
                return Optional.empty();
            }
            CeSheet656ResolvedRow row = new CeSheet656ResolvedRow();
            row.setEmissionSourceCode("SRC-001");
            row.setCompanyCode("COMP-001");
            row.setCompanyName("测试公司");
            row.setFactoryName("一厂");
            row.setEmissionSourceCategoryCode("CAT-001");
            row.setScope("范围1");
            row.setScopeSubcategory("固定燃烧");
            row.setEmissionSourceIdentity("天然气锅炉");
            row.setEmissionSourceName("天然气");
            row.setUnit("Nm3");
            row.setEmissionFactorCode("EF-2026-001");
            return Optional.of(row);
        };
    }
}
