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
import org.dromara.carbon.enterprise.factor.domain.CeFactorConfirmation;
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
import org.dromara.system.domain.SysDept;
import org.dromara.system.mapper.SysDeptMapper;
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
    private CeFactorCacheRecordMapper factorCacheRecordMapper;
    private CeFactorConfirmationMapper factorConfirmationMapper;
    private SysDeptMapper sysDeptMapper;
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
        initializeEntityLambdaCache(CeFactorConfirmationMapper.class, CeFactorConfirmation.class);
        initializeEntityLambdaCache(CeGreenPowerCertificateMapper.class, CeGreenPowerCertificate.class);
        initializeEntityLambdaCache(SysDeptMapper.class, SysDept.class);
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
        factorCacheRecordMapper = mock(CeFactorCacheRecordMapper.class);
        factorConfirmationMapper = mock(CeFactorConfirmationMapper.class);
        sysDeptMapper = mock(SysDeptMapper.class);
        when(dimensionProjectionMapper.selectByDimensionCode(any())).thenReturn(List.of());
        service = new CeOptionServiceImpl(
            activityDataMapper,
            companyFactoryMapper,
            emissionSourceMapper,
            mock(CeEmissionSourceCategoryMapper.class),
            greenPowerCertificateMapper,
            denominatorFactMapper,
            factorCacheRecordMapper,
            factorConfirmationMapper,
            intensityMetricMapper,
            mock(CeReportTemplateFileMapper.class),
            mock(CeCaptureBatchMapper.class),
            mock(CeLicenseStateMapper.class),
            dimensionProjectionMapper,
            sysDeptMapper
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
    void responsibleDeptOptionsComeFromSystemDeptAndHistoricalBusinessRows() {
        SysDept dept = new SysDept();
        dept.setDeptId(10L);
        dept.setParentId(1L);
        dept.setDeptName("Production");
        dept.setStatus("0");
        CeActivityData activity = new CeActivityData();
        activity.setResponsibleDept("EHS");
        CeEmissionSource source = new CeEmissionSource();
        source.setResponsibleDept("Production");
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(dept));
        when(activityDataMapper.selectObjs(any())).thenReturn(List.of(activity.getResponsibleDept()));
        when(emissionSourceMapper.selectObjs(any())).thenReturn(List.of(source.getResponsibleDept()));

        var options = service.listOptions("responsible-dept", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("EHS", "Production");
        assertThat(options.get(1).getRecord())
            .containsEntry("deptId", 10L)
            .containsEntry("deptName", "Production");
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
            dimensionProjectionMapper,
            sysDeptMapper
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
    void emissionSourceNameOptionsComeFromEmissionSourceRecordsForEfFactorAutofill() {
        CeEmissionSource source = emissionSource("ES-001", "柴油燃烧", "A公司", "一厂", "cat-a");
        source.setSourceUnit("t");
        source.setFactorKey("EF-201-DIESEL");
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of(source));
        when(factorCacheRecordMapper.selectList(any())).thenReturn(List.of());

        var options = service.listOptions("emission-source-name", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("柴油燃烧");
        assertThat(options.get(0).getRecord())
            .containsEntry("sourceIdentificationCode", "ES-001")
            .containsEntry("emissionSourceName", "柴油燃烧")
            .containsEntry("sourceUnit", "t")
            .containsEntry("factorKey", "EF-201-DIESEL");
    }

    @Test
    void activityEntryEmissionSourceNameOptionsCarryMasterRecordForEarlyValidation() {
        CeEmissionSource source = emissionSource("ES-001", "柴油燃烧", "A公司", "一厂", "cat-a");
        source.setSourceUnit("t");
        source.setFactorKey("EF-201-DIESEL");
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of(source));
        when(factorCacheRecordMapper.selectList(any())).thenReturn(List.of());

        var options = service.listOptions("activity-entry-emission-source-name", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("柴油燃烧");
        assertThat(options.get(0).getRecord())
            .containsEntry("sourceIdentificationCode", "ES-001")
            .containsEntry("companyName", "A公司")
            .containsEntry("factoryName", "一厂")
            .containsEntry("sourceUnit", "t")
            .containsEntry("factorKey", "EF-201-DIESEL");
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
    void factorKeyOptionsComeFromCacheAndConfirmationTables() {
        when(dimensionProjectionMapper.selectByDimensionCode("ef-factor")).thenReturn(List.of());
        CeFactorCacheRecord cache = new CeFactorCacheRecord();
        cache.setFactorKey("KEY-DIESEL");
        cache.setFactorCode("FC-DIESEL");
        cache.setEmissionSourceName("Diesel combustion");
        cache.setFactorName("Diesel factor");
        cache.setFactorUnit("kgCO2e/t");
        cache.setEnabledFlag(true);
        CeFactorConfirmation confirmation = new CeFactorConfirmation();
        confirmation.setFactorCode("FC-ELECTRICITY");
        confirmation.setFactorName("Electricity factor");
        confirmation.setFactorUnit("kgCO2e/MWh");
        confirmation.setConfirmationStatus("confirmed");
        when(factorCacheRecordMapper.selectList(any())).thenReturn(List.of(cache));
        when(factorConfirmationMapper.selectList(any())).thenReturn(List.of(confirmation));

        var options = service.listOptions("factor-key", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("FC-ELECTRICITY", "KEY-DIESEL");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("FC-ELECTRICITY / Electricity factor (kgCO2e/MWh)", "KEY-DIESEL / Diesel combustion (kgCO2e/t)");
        assertThat(options.get(1).getRecord())
            .containsEntry("factorKey", "KEY-DIESEL")
            .containsEntry("factorCode", "FC-DIESEL")
            .containsEntry("emissionSourceName", "Diesel combustion")
            .containsEntry("factorUnit", "kgCO2e/t");
    }

    @Test
    void factorKeyOptionsIncludeEfFactorDimensionRecords() {
        CeDimensionRecordVo factor = new CeDimensionRecordVo();
        factor.setRecordCode("EF-201-DIESEL");
        factor.setRecordName("Diesel combustion");
        factor.setEmissionSourceNameEn("Diesel combustion EN");
        factor.setFuelMaterialCategory("Diesel");
        factor.setSourceUnit("t");
        factor.setApplicableScope("Scope 1");
        factor.setFactorSource("Source A");
        factor.setFactorUnit("kgCO2e/t");
        when(dimensionProjectionMapper.selectByDimensionCode("ef-factor")).thenReturn(List.of(factor));
        when(factorCacheRecordMapper.selectList(any())).thenReturn(List.of());
        when(factorConfirmationMapper.selectList(any())).thenReturn(List.of());

        var options = service.listOptions("factor-key", null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("EF-201-DIESEL");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("EF-201-DIESEL / Diesel combustion (kgCO2e/t)");
        assertThat(options.get(0).getRecord())
            .containsEntry("factorKey", "EF-201-DIESEL")
            .containsEntry("factorCode", "EF-201-DIESEL")
            .containsEntry("factorName", "Diesel combustion")
            .containsEntry("emissionSourceName", "Diesel combustion")
            .containsEntry("emissionSourceNameEn", "Diesel combustion EN")
            .containsEntry("fuelMaterialCategory", "Diesel")
            .containsEntry("sourceUnit", "t")
            .containsEntry("applicableScope", "Scope 1")
            .containsEntry("factorSource", "Source A")
            .containsEntry("factorUnit", "kgCO2e/t");
    }

    @Test
    void dimensionFieldOptionsUseFieldValueLabelsExceptStatus() {
        CeDimensionRecordVo company = new CeDimensionRecordVo();
        company.setIndustrySectionCode("C");
        company.setIndustrySectionName("制造业");
        CeDimensionRecordVo energy = new CeDimensionRecordVo();
        energy.setIndustrySectionCode("D");
        energy.setIndustrySectionName("电力、热力、燃气及水生产和供应业");
        when(dimensionProjectionMapper.selectByDimensionCode("company")).thenReturn(List.of(company));
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setField("industrySectionCode");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .contains("C / 制造业");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .contains("C");
        assertThat(options.stream().filter(option -> "C".equals(String.valueOf(option.getValue()))).findFirst().orElseThrow().getRecord())
            .containsEntry("industrySectionCode", "C")
            .containsEntry("industrySectionName", "制造业");
    }

    @Test
    void companyIndustryNameOptionsCarryPairedCodeAndName() {
        CeDimensionRecordVo company = new CeDimensionRecordVo();
        company.setIndustryDivisionCode("26");
        company.setIndustryDivisionName("化学原料和化学制品制造业");
        when(dimensionProjectionMapper.selectByDimensionCode("company")).thenReturn(List.of(company));
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setField("industryDivisionName");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .contains("化学原料和化学制品制造业 / 26");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .contains("化学原料和化学制品制造业");
        assertThat(options.stream()
            .filter(option -> "化学原料和化学制品制造业".equals(String.valueOf(option.getValue())))
            .findFirst()
            .orElseThrow()
            .getRecord())
            .containsEntry("industryDivisionCode", "26")
            .containsEntry("industryDivisionName", "化学原料和化学制品制造业");
    }

    @Test
    void companyIndustryOptionsFallBackToGbIndustryClassification() {
        when(dimensionProjectionMapper.selectByDimensionCode("company")).thenReturn(List.of());
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setField("industryClassCode");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .contains("2614 / 有机化学原料制造", "3011 / 水泥制造", "4411 / 火力发电", "8052 / 足浴服务", "8053 / 养生保健服务");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .hasSizeGreaterThan(1300)
            .contains("2614", "3011", "4411", "8052", "8053");
        assertThat(options)
            .anySatisfy(option -> assertThat(option.getRecord())
                .containsEntry("source", "gbt4754-2017")
                .containsEntry("industryClassCode", "2614")
                .containsEntry("industryGroupCode", "261")
                .containsEntry("industryDivisionCode", "26")
                .containsEntry("industrySectionCode", "C")
                .containsEntry("industryClassName", "有机化学原料制造"));
    }

    @Test
    void companyIndustryDivisionOptionsCarryParentSectionCode() {
        when(dimensionProjectionMapper.selectByDimensionCode("company")).thenReturn(List.of());
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setField("industryDivisionCode");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .hasSizeGreaterThanOrEqualTo(97)
            .contains("26", "44");
        assertThat(options.stream()
            .filter(option -> "26".equals(String.valueOf(option.getValue())))
            .findFirst()
            .orElseThrow()
            .getRecord())
            .containsEntry("industrySectionCode", "C")
            .containsEntry("industrySectionName", "制造业")
            .containsEntry("industryDivisionCode", "26")
            .containsEntry("industryDivisionName", "化学原料和化学制品制造业");
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
            .contains("110000", "310000");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .contains("110000 / 北京市", "310000 / 上海市");
        assertThat(options.stream().filter(option -> "110000".equals(String.valueOf(option.getValue()))).findFirst().orElseThrow().getRecord())
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
            .contains("北京市");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .contains("北京市 / 110000");
        assertThat(options.stream().filter(option -> "北京市".equals(String.valueOf(option.getValue()))).findFirst().orElseThrow().getRecord())
            .containsEntry("provinceCode", "110000")
            .containsEntry("provinceName", "北京市");
    }

    @Test
    void dimensionFieldOptionsAcceptLegacyFieldNameParameter() {
        CeDimensionRecordVo company = new CeDimensionRecordVo();
        company.setIndustrySectionCode("C");
        when(dimensionProjectionMapper.selectByDimensionCode("company")).thenReturn(List.of(company));
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("company");
        query.setFieldName("industrySectionCode");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .contains("C");
    }

    @Test
    void adminDivisionCodeOptionsFallBackToRecordCode() {
        CeDimensionRecordVo beijing = new CeDimensionRecordVo();
        beijing.setRecordCode("110000");
        beijing.setRecordName("Beijing");
        when(dimensionProjectionMapper.selectByDimensionCode("admin-division")).thenReturn(List.of(beijing));
        CeOptionQueryBo query = new CeOptionQueryBo();
        query.setDimensionCode("admin-division");
        query.setField("divisionCode");

        var options = service.listOptions("dimension-field", query);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("110000");
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
