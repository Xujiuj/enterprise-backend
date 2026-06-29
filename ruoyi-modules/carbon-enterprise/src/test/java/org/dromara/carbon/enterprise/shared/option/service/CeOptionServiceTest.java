package org.dromara.carbon.enterprise.shared.option.service;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.carbon.enterprise.dimension.domain.CeCompanyFactory;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.factor.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.greenpower.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityMetric;
import org.dromara.carbon.enterprise.shared.option.domain.bo.CeOptionQueryBo;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.activity.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeCompanyFactoryMapper;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.factor.mapper.CeFactorConfirmationMapper;
import org.dromara.carbon.enterprise.greenpower.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.intensity.mapper.CeIntensityDenominatorFactMapper;
import org.dromara.carbon.enterprise.intensity.mapper.CeIntensityMetricMapper;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.report.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.shared.option.service.impl.CeOptionServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeOptionServiceTest {

    private CeIntensityMetricMapper intensityMetricMapper;
    private CeIntensityDenominatorFactMapper denominatorFactMapper;
    private CeActivityDataMapper activityDataMapper;
    private CeCompanyFactoryMapper companyFactoryMapper;
    private CeEmissionSourceMapper emissionSourceMapper;
    private CeGreenPowerCertificateMapper greenPowerCertificateMapper;
    private CeDimensionProjectionMapper dimensionProjectionMapper;
    private CeOptionServiceImpl service;

    @BeforeAll
    static void initializeLambdaCache() {
        initializeEntityLambdaCache(CeActivityDataMapper.class, CeActivityData.class);
        initializeEntityLambdaCache(CeCompanyFactoryMapper.class, CeCompanyFactory.class);
        initializeEntityLambdaCache(CeIntensityMetricMapper.class, CeIntensityMetric.class);
        initializeEntityLambdaCache(CeIntensityDenominatorFactMapper.class, CeIntensityDenominatorFact.class);
        initializeEntityLambdaCache(CeEmissionSourceMapper.class, CeEmissionSource.class);
        initializeEntityLambdaCache(CeEmissionSourceCategoryMapper.class, org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory.class);
        initializeEntityLambdaCache(CeFactorCacheRecordMapper.class, CeFactorCacheRecord.class);
        initializeEntityLambdaCache(CeGreenPowerCertificateMapper.class, CeGreenPowerCertificate.class);
    }

    @BeforeEach
    void setUp() {
        intensityMetricMapper = mock(CeIntensityMetricMapper.class);
        denominatorFactMapper = mock(CeIntensityDenominatorFactMapper.class);
        activityDataMapper = mock(CeActivityDataMapper.class);
        companyFactoryMapper = mock(CeCompanyFactoryMapper.class);
        emissionSourceMapper = mock(CeEmissionSourceMapper.class);
        greenPowerCertificateMapper = mock(CeGreenPowerCertificateMapper.class);
        dimensionProjectionMapper = mock(CeDimensionProjectionMapper.class);
        service = new CeOptionServiceImpl(
            activityDataMapper,
            companyFactoryMapper,
            emissionSourceMapper,
            mock(CeEmissionSourceCategoryMapper.class),
            greenPowerCertificateMapper,
            denominatorFactMapper,
            mock(CeFactorCacheRecordMapper.class),
            mock(CeFactorConfirmationMapper.class),
            intensityMetricMapper,
            mock(CeReportTemplateFileMapper.class),
            mock(CeCaptureBatchMapper.class),
            mock(CeLicenseStateMapper.class),
            dimensionProjectionMapper
        );
    }

    @Test
    void factoryCodeOptionsDoNotQueryNonPersistentEmissionSourceFactoryCode() {
        when(companyFactoryMapper.selectList(any())).thenReturn(List.of());
        when(activityDataMapper.selectList(any())).thenReturn(List.of());
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of());
        when(greenPowerCertificateMapper.selectList(any())).thenReturn(List.of());

        var options = service.listOptions("factory-code", null);

        assertThat(options).isEmpty();
    }

    @Test
    void companyCodeOptionsCarryCompanyRecordForAutofill() {
        CeCompanyFactory factory = new CeCompanyFactory();
        factory.setCompanyCode("C001");
        factory.setCompanyName("A公司");
        factory.setFactoryCode("F001");
        factory.setFactoryName("一厂");
        when(companyFactoryMapper.selectList(any())).thenReturn(List.of(factory));
        when(activityDataMapper.selectList(any())).thenReturn(List.of());
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of());

        var options = service.listOptions("company-code", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("C001");
        assertThat(options.get(0).getRecord())
            .containsEntry("companyCode", "C001")
            .containsEntry("companyName", "A公司")
            .containsEntry("factoryCode", "F001")
            .containsEntry("factoryName", "一厂");
    }

    @Test
    void factoryNameOptionsCarryCompanyFactoryRecordForAutofill() {
        CeCompanyFactory factory = new CeCompanyFactory();
        factory.setCompanyCode("C001");
        factory.setCompanyName("Company A");
        factory.setFactoryCode("F001");
        factory.setFactoryName("Factory One");
        when(companyFactoryMapper.selectList(any())).thenReturn(List.of(factory));
        when(activityDataMapper.selectList(any())).thenReturn(List.of());
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of());
        when(greenPowerCertificateMapper.selectList(any())).thenReturn(List.of());

        var options = service.listOptions("factory-name", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("Factory One");
        assertThat(options.get(0).getRecord())
            .containsEntry("companyCode", "C001")
            .containsEntry("companyName", "Company A")
            .containsEntry("factoryCode", "F001")
            .containsEntry("factoryName", "Factory One");
    }

    @Test
    void sourceCategoryOptionsCarryScopeFieldsForAutofill() {
        var categoryMapper = mock(CeEmissionSourceCategoryMapper.class);
        service = new CeOptionServiceImpl(
            activityDataMapper,
            companyFactoryMapper,
            emissionSourceMapper,
            categoryMapper,
            greenPowerCertificateMapper,
            denominatorFactMapper,
            mock(CeFactorCacheRecordMapper.class),
            mock(CeFactorConfirmationMapper.class),
            intensityMetricMapper,
            mock(CeReportTemplateFileMapper.class),
            mock(CeCaptureBatchMapper.class),
            mock(CeLicenseStateMapper.class),
            dimensionProjectionMapper
        );
        var category = new org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory();
        category.setCategorySk("CAT-1");
        category.setGhgScope("范围1");
        category.setGhgScopeCategory("固定燃烧");
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(activityDataMapper.selectList(any())).thenReturn(List.of());
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of());
        when(greenPowerCertificateMapper.selectList(any())).thenReturn(List.of());

        var options = service.listOptions("source-category-key", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("CAT-1");
        assertThat(options.get(0).getRecord())
            .containsEntry("sourceCategoryKey", "CAT-1")
            .containsEntry("scopeName", "范围1")
            .containsEntry("scopeSubcategory", "固定燃烧");
    }

    @Test
    void emissionSourceCodeOptionsCarrySourceRecordForAutofill() {
        CeEmissionSource source = emissionSource("ES-001", "柴油", "A公司", "一厂", "cat-a");
        source.setResponsibleDept("生产部");
        source.setDataSource("台账");
        source.setFactorKey(null);
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of(source));

        var options = service.listOptions("emission-source-code", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("ES-001");
        assertThat(options.get(0).getRecord())
            .containsEntry("companyCode", "A公司-CODE")
            .containsEntry("companyName", "A公司")
            .containsEntry("factoryName", "一厂")
            .containsEntry("sourceCategoryKey", "cat-a")
            .containsEntry("sourceIdentificationCode", "ES-001")
            .containsEntry("sourceIdentificationName", "柴油识别")
            .containsEntry("emissionSourceName", "柴油")
            .containsEntry("responsibleDept", "生产部")
            .containsEntry("dataSource", "台账");
    }

    @Test
    void intensityRuleCodeOptionsComeFromEnterpriseBusinessTables() {
        when(intensityMetricMapper.selectObjs(any())).thenReturn(List.of("RULE-A", "RULE-B"));
        when(denominatorFactMapper.selectObjs(any())).thenReturn(List.of("RULE-B", "RULE-C"));

        var options = service.listOptions("intensity-rule-code", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("RULE-A", "RULE-B", "RULE-C");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("RULE-A", "RULE-B", "RULE-C");
    }

    @Test
    void intensityTargetOptionsComeFromEnterpriseTargetProjection() {
        CeDimensionRecordVo revenueTarget = new CeDimensionRecordVo();
        revenueTarget.setRecordCode("manufacturing");
        revenueTarget.setRecordName("2026");
        revenueTarget.setTargetValue("1.25");
        revenueTarget.setUnitName("tCO2e/万元");
        CeDimensionRecordVo outputTarget = new CeDimensionRecordVo();
        outputTarget.setRecordCode("manufacturing");
        outputTarget.setRecordName("2027");
        outputTarget.setTargetValue("1.10");
        outputTarget.setUnitName("tCO2e/万元");
        when(dimensionProjectionMapper.selectByDimensionCode("intensity-target")).thenReturn(List.of(revenueTarget, outputTarget));

        var options = service.listOptions("intensity-target-code", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("manufacturing:2026", "manufacturing:2027");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly(
                "manufacturing / 2026 / 1.25 / tCO2e/万元",
                "manufacturing / 2027 / 1.10 / tCO2e/万元"
            );
    }

    @Test
    void denominatorUnitOptionsComeFromEnterpriseBusinessTables() {
        when(intensityMetricMapper.selectObjs(any())).thenReturn(List.of("tCO2e/万元", "tCO2e/吨"));
        when(denominatorFactMapper.selectObjs(any())).thenReturn(List.of("tCO2e/吨", "MWh"));

        var options = service.listOptions("denominator-unit", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("MWh", "tCO2e/万元", "tCO2e/吨");
    }

    @Test
    void dimensionFieldOptionsUseFieldValueLabelsExceptStatus() {
        CeDimensionRecordVo company = new CeDimensionRecordVo();
        company.setIndustrySectionCode("C");
        company.setIndustrySectionName("制造业");
        when(dimensionProjectionMapper.selectByDimensionCode("company")).thenReturn(List.of(company));
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setField("industrySectionCode");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("C");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("C");
        assertThat(options.get(0).getRecord())
            .containsEntry("industrySectionCode", "C")
            .containsEntry("industrySectionName", "制造业");
    }

    @Test
    void companyProvinceCodeOptionsComeFromAdminDivisionRecords() {
        CeDimensionRecordVo beijing = new CeDimensionRecordVo();
        beijing.setRecordCode("110000");
        beijing.setRecordName("北京市");
        beijing.setDivisionCode("SHOULD-NOT-REQUIRE-PROJECTION-ALIAS");
        CeDimensionRecordVo shanghai = new CeDimensionRecordVo();
        shanghai.setRecordCode("310000");
        shanghai.setRecordName("上海市");
        when(dimensionProjectionMapper.selectByDimensionCode("admin-division")).thenReturn(List.of(beijing, shanghai));
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setField("provinceCode");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("110000", "310000");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("110000 / 北京市", "310000 / 上海市");
        assertThat(options.get(0).getRecord())
            .containsEntry("provinceCode", "110000")
            .containsEntry("provinceName", "北京市");
    }

    @Test
    void companyProvinceNameOptionsComeFromAdminDivisionRecords() {
        CeDimensionRecordVo beijing = new CeDimensionRecordVo();
        beijing.setRecordCode("110000");
        beijing.setRecordName("北京市");
        when(dimensionProjectionMapper.selectByDimensionCode("admin-division")).thenReturn(List.of(beijing));
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setField("provinceName");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("北京市");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("北京市 / 110000");
        assertThat(options.get(0).getRecord())
            .containsEntry("provinceCode", "110000")
            .containsEntry("provinceName", "北京市");
    }

    @Test
    void activityEntryLeafOptionsKeepEachEmissionSourceCodeWhenNamesRepeat() {
        CeEmissionSource largerCode = emissionSource("ES-002", "柴油", "A公司", "一厂", "cat-a");
        largerCode.setResponsibleDept("生产部");
        CeEmissionSource smallerCode = emissionSource("ES-001", "柴油", "A公司", "一厂", "cat-a");
        smallerCode.setResponsibleDept("运营部");
        CeEmissionSource other = emissionSource("ES-003", "天然气", "A公司", "二厂", "cat-b");
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of(largerCode, smallerCode, other));

        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setCompanyName("A公司");
        var options = service.listOptions("activity-entry-source-leaf", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("柴油", "柴油", "天然气");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("ES-001", "ES-002", "ES-003");
        assertThat(options.get(0).getRecord())
            .containsEntry("sourceIdentificationCode", "ES-001")
            .containsEntry("responsibleDept", "运营部");
        assertThat(options.get(1).getRecord())
            .containsEntry("sourceIdentificationCode", "ES-002")
            .containsEntry("responsibleDept", "生产部");
    }

    private CeEmissionSource emissionSource(String code, String name, String companyName, String factoryName, String categoryKey) {
        CeEmissionSource source = new CeEmissionSource();
        source.setCompanyCode(companyName + "-CODE");
        source.setCompanyName(companyName);
        source.setFactoryName(factoryName);
        source.setSourceCategoryKey(categoryKey);
        source.setScopeName("范围1");
        source.setScopeSubcategory("子类1");
        source.setSourceIdentificationCode(code);
        source.setSourceIdentificationName(name + "识别");
        source.setEmissionSourceName(name);
        source.setFactorKey("factor-" + code);
        source.setEnabledFlag(true);
        return source;
    }

    private static void initializeEntityLambdaCache(Class<?> mapperType, Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, mapperType.getName());
        assistant.setCurrentNamespace(mapperType.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, entityType);
        LambdaUtils.installCache(tableInfo);
    }
}
