package org.dromara.carbon.enterprise.activity.service;

import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldDescriptor;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityFieldValue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityResolvedRow;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationIssue;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationRequest;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityValidationResult;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityDerivedFieldResolver;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityValidationService;
import org.dromara.carbon.enterprise.activity.service.impl.CeEmissionActivityValidationServiceImpl;
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
class CeEmissionActivityValidationTest {

    private static final List<String> DERIVED_CODES = List.of(
        "sourceIdentificationCode", "companyCode", "sourceCategoryKey", "activityUnit", "factorKey"
    );

    @Test
    void rowLevelContractExposesOnlyEntryFields() {
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        List<CeEmissionActivityFieldDescriptor> fields = service.listEntryFields();

        assertEquals(List.of(
            "companyName:公司名称:false:true:false",
            "factoryName:工厂:false:true:false",
            "scopeName:范围:false:true:false",
            "scopeSubcategory:范围子类别:false:true:false",
            "sourceIdentificationName:排放源识别:false:true:false",
            "emissionSourceName:排放源名称:false:true:false",
            "activityPeriod:活动期间:false:true:false",
            "activityDate:日期:false:true:false",
            "activityValue:活动数据:false:true:false",
            "responsibleDept:负责部门:false:true:false",
            "dataSource:数据来源:false:true:false"
        ), fields.stream()
            .map(field -> field.getFieldCode() + ":" + field.getFieldName() + ":"
                + field.isSourceRequired() + ":" + field.isRowValueRequired() + ":" + field.isDerivedField())
            .toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
            fields.stream().map(CeEmissionActivityFieldDescriptor::getFieldOrder).toList());
    }

    @Test
    void rowLevelValidatorDoesNotTreatBlankF017AsRequired() {
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        CeEmissionActivityValidationResult result = service.validate(request(values -> values.put("sourceRemark", "")));

        assertTrue(result.isValid());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void validatorImplementationIsPlainClassWithNoProductionBeanRegistration() {
        assertFalse(CeEmissionActivityValidationServiceImpl.class.isAnnotationPresent(Service.class));
        assertFalse(CeEmissionActivityValidationServiceImpl.class.isAnnotationPresent(Component.class));
    }

    @Test
    void validatorUsesInjectedEnterpriseLocalResolverForDerivedFields() {
        ICeEmissionActivityValidationService service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        CeEmissionActivityValidationResult valid = service.validate(request(values -> {
        }));
        assertTrue(valid.isValid());
        assertEquals(expectedDerivedValues(), resolvedValueMap(valid));

        CeEmissionActivityValidationResult mismatch = service.validate(request(values -> {
            values.put("companyName", "伪造公司");
            values.put("factorKey", "EF-FAKE");
        }));
        assertFalse(mismatch.isValid());
        assertEquals(List.of("companyName", "factorKey"), issueColumns(mismatch, "DERIVED_FIELD_MISMATCH"));
        assertEquals(expectedDerivedValues(), resolvedValueMap(mismatch));
    }

    @Test
    void validRowPassesWithExactDerivedFieldChecks() {
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        CeEmissionActivityValidationResult result = service.validate(request(values -> {
        }));

        assertTrue(result.isValid());
        assertFalse(result.isBlocking());
        assertTrue(result.isDraftSavable());
        assertTrue(result.getIssues().isEmpty());
        assertEquals(expectedDerivedValues(), resolvedValueMap(result));
    }

    @Test
    void missingRequiredAndInvalidFieldsProduceExactBlockingErrors() {
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        CeEmissionActivityValidationResult result = service.validate(request(values -> {
            values.put("sourceIdentificationCode", "");
            values.put("activityPeriod", "2026-13");
            values.put("activityDate", "bad-date");
            values.put("activityValue", "0");
            values.put("responsibleDept", "");
        }));

        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertFalse(result.isDraftSavable());
        assertEquals(List.of(
            "REQUIRED_FIELD_MISSING:responsibleDept:负责部门",
            "INVALID_TYPE:activityPeriod:活动期间",
            "INVALID_TYPE:activityDate:日期",
            "INVALID_VALUE_DOMAIN:activityValue:活动数据",
            "DERIVED_FIELD_SERVER_FILLED:sourceIdentificationCode:排放源识别编号"
        ), issueSummaries(result));
    }

    @Test
    void sourceIdentificationDisambiguatesDuplicateEmissionSourceNames() {
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        CeEmissionActivityValidationResult disambiguated = service.validate(request(values -> {
            values.put("sourceIdentificationCode", "");
            values.put("sourceIdentificationName", "天然气锅炉");
        }));

        assertTrue(disambiguated.isValid());
        assertFalse(disambiguated.isBlocking());
        assertEquals("SRC-001", resolvedValueMap(disambiguated).get("sourceIdentificationCode"));

        CeEmissionActivityValidationResult ambiguous = service.validate(request(values -> {
            values.put("sourceIdentificationCode", "");
            values.put("sourceIdentificationName", "");
        }));

        assertFalse(ambiguous.isValid());
        assertTrue(ambiguous.isBlocking());
        assertEquals(List.of("REQUIRED_FIELD_MISSING:sourceIdentificationName:排放源识别"), issueSummaries(ambiguous));
    }

    @Test
    void weakWarningsDoNotBlockDraftSaveAndIdentifyServerFilledDerivedColumns() {
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        CeEmissionActivityValidationResult result = service.validate(request(values -> {
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
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(fakeResolver());

        CeEmissionActivityValidationResult result = service.validate(request(values -> {
            values.put("companyName", "伪造公司");
            values.put("factorKey", "EF-FAKE");
        }));

        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(List.of("companyName", "factorKey"), issueColumns(result, "DERIVED_FIELD_MISMATCH"));
        assertEquals(7, firstIssue(result, "DERIVED_FIELD_MISMATCH").getRowNumber());
        assertEquals("公司名称", firstIssue(result, "DERIVED_FIELD_MISMATCH").getFieldName());
        assertEquals(expectedDerivedValues(), resolvedValueMap(result));
    }

    @Test
    void missingMasterDataMatchBlocksRow() {
        CeEmissionActivityValidationServiceImpl service = new CeEmissionActivityValidationServiceImpl(code -> Optional.empty());

        CeEmissionActivityValidationResult result = service.validate(request(values -> {
        }));

        assertFalse(result.isValid());
        assertTrue(result.isBlocking());
        assertEquals(List.of("MASTER_DATA_NOT_FOUND:sourceIdentificationCode:排放源识别编号"), issueSummaries(result));
        assertFalse(resolvedValueMap(result).containsKey("companyCode"));
        assertEquals("2026", resolvedValueMap(result).get("activityYear"));
    }

    private List<String> issueSummaries(CeEmissionActivityValidationResult result) {
        return result.getIssues().stream()
            .map(issue -> issue.getCode() + ":" + issue.getFieldCode() + ":" + issue.getFieldName())
            .toList();
    }

    private List<String> issueColumns(CeEmissionActivityValidationResult result, String issueCode) {
        return result.getIssues().stream()
            .filter(issue -> issueCode.equals(issue.getCode()))
            .map(CeEmissionActivityValidationIssue::getFieldCode)
            .toList();
    }

    private CeEmissionActivityValidationIssue firstIssue(CeEmissionActivityValidationResult result, String issueCode) {
        return result.getIssues().stream()
            .filter(issue -> issueCode.equals(issue.getCode()))
            .findFirst()
            .orElseThrow();
    }

    private Map<String, String> resolvedValueMap(CeEmissionActivityValidationResult result) {
        return result.getResolvedDerivedFieldValues().stream()
            .collect(Collectors.toMap(CeEmissionActivityFieldValue::getFieldCode, CeEmissionActivityFieldValue::getValue,
                (left, right) -> right, LinkedHashMap::new));
    }

    private Map<String, String> expectedDerivedValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("activityYear", "2026");
        values.put("activityMonth", "6");
        values.put("sourceIdentificationCode", "SRC-001");
        values.put("companyCode", "COMP-001");
        values.put("companyName", "测试公司");
        values.put("factoryName", "一厂");
        values.put("sourceCategoryKey", "CAT-001");
        values.put("scopeName", "范围1");
        values.put("scopeSubcategory", "固定燃烧");
        values.put("sourceIdentificationName", "天然气锅炉");
        values.put("emissionSourceName", "天然气");
        values.put("activityUnit", "Nm3");
        values.put("factorKey", "EF-2026-001");
        return values;
    }

    private CeEmissionActivityValidationRequest request(Consumer<Map<String, String>> customizer) {
        Map<String, String> values = baseValues();
        customizer.accept(values);
        CeEmissionActivityValidationRequest request = new CeEmissionActivityValidationRequest();
        request.setRowNumber(7);
        request.setFieldValues(values.entrySet().stream()
            .map(entry -> field(entry.getKey(), entry.getValue()))
            .toList());
        return request;
    }

    private Map<String, String> baseValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sourceIdentificationCode", "SRC-001");
        values.putAll(expectedDerivedValues());
        values.put("activityPeriod", "2026-06");
        values.put("activityDate", "2026-06-05");
        values.put("activityValue", "12.5");
        values.put("responsibleDept", "生产部");
        values.put("dataSource", "计量台账");
        values.put("sourceRemark", "正常记录");
        return values;
    }

    private CeEmissionActivityFieldValue field(String code, String value) {
        CeEmissionActivityFieldValue fieldValue = new CeEmissionActivityFieldValue();
        fieldValue.setFieldCode(code);
        fieldValue.setValue(value);
        return fieldValue;
    }

    private ICeEmissionActivityDerivedFieldResolver fakeResolver() {
        return new ICeEmissionActivityDerivedFieldResolver() {
            @Override
            public Optional<CeEmissionActivityResolvedRow> resolve(String code) {
                return "SRC-001".equals(code) ? Optional.of(resolvedRow()) : Optional.empty();
            }

            @Override
            public List<CeEmissionActivityResolvedRow> resolveByEntryFields(String companyName, String factoryName, String scope,
                                                                            String scopeSubcategory, String sourceIdentificationName,
                                                                            String emissionSourceName) {
                if ("测试公司".equals(companyName)
                    && "一厂".equals(factoryName)
                    && "范围1".equals(scope)
                    && "固定燃烧".equals(scopeSubcategory)
                    && "天然气".equals(emissionSourceName)) {
                    if ("天然气锅炉".equals(sourceIdentificationName)) {
                        return List.of(resolvedRow());
                    }
                    if (sourceIdentificationName == null || sourceIdentificationName.isBlank()) {
                        CeEmissionActivityResolvedRow duplicate = resolvedRow();
                        duplicate.setEmissionSourceCode("SRC-002");
                        duplicate.setEmissionSourceIdentity("备用天然气锅炉");
                        return List.of(resolvedRow(), duplicate);
                    }
                }
                return List.of();
            }

            private CeEmissionActivityResolvedRow resolvedRow() {
                CeEmissionActivityResolvedRow row = new CeEmissionActivityResolvedRow();
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
                return row;
            }
        };
    }
}
