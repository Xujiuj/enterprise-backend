package org.dromara.carbon.enterprise.emission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.emission.domain.bo.CeEmissionSourceBo;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.emission.service.impl.CeEmissionSourceServiceImpl;
import org.dromara.carbon.enterprise.shared.support.CeEnterpriseDataScopeSupport;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.validate.AddGroup;
import jakarta.validation.Validation;
import org.dromara.system.domain.SysDept;
import org.dromara.system.mapper.SysDeptMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeEmissionSourceServiceTest {

    private CeEmissionSourceMapper emissionSourceMapper;
    private CeDimensionProjectionMapper dimensionProjectionMapper;
    private CeEmissionSourceCategoryMapper emissionSourceCategoryMapper;
    private SysDeptMapper sysDeptMapper;
    private CeEmissionSourceServiceImpl service;

    @BeforeAll
    static void initializeLambdaCache() {
        initializeEntityLambdaCache(CeEmissionSourceCategoryMapper.class, CeEmissionSourceCategory.class);
        initializeEntityLambdaCache(CeEmissionSourceMapper.class, CeEmissionSource.class);
        initializeEntityLambdaCache(SysDeptMapper.class, SysDept.class);
    }

    @BeforeEach
    void setUp() {
        emissionSourceMapper = mock(CeEmissionSourceMapper.class);
        dimensionProjectionMapper = mock(CeDimensionProjectionMapper.class);
        emissionSourceCategoryMapper = mock(CeEmissionSourceCategoryMapper.class);
        sysDeptMapper = mock(SysDeptMapper.class);
        when(sysDeptMapper.selectCount(any())).thenReturn(1L);
        when(sysDeptMapper.selectList(any())).thenReturn(java.util.List.of());
        when(emissionSourceMapper.selectList(any())).thenReturn(java.util.List.of());
        when(emissionSourceMapper.selectCount(any())).thenReturn(0L);
        when(dimensionProjectionMapper.selectByDimensionCode("ef-factor")).thenReturn(java.util.List.of(defaultFactor()));
        CeEnterpriseDataScopeSupport dataScopeSupport = mock(CeEnterpriseDataScopeSupport.class);
        when(dataScopeSupport.unrestricted()).thenReturn(true);
        when(dataScopeSupport.canAccessDept(any())).thenReturn(true);
        service = new CeEmissionSourceServiceImpl(
            emissionSourceMapper,
            dimensionProjectionMapper,
            emissionSourceCategoryMapper,
            sysDeptMapper,
            dataScopeSupport
        ) {
            @Override
            protected CeEmissionSource toEntity(CeEmissionSourceBo bo) {
                CeEmissionSource source = new CeEmissionSource();
                source.setCompanyCode(bo.getCompanyCode());
                source.setCompanyName(bo.getCompanyName());
                source.setFactoryCode(bo.getFactoryCode());
                source.setFactoryName(bo.getFactoryName());
                source.setSourceCategoryKey(bo.getSourceCategoryKey());
                source.setScopeName(bo.getScopeName());
                source.setScopeSubcategory(bo.getScopeSubcategory());
                source.setSourceIdentificationCode(bo.getSourceIdentificationCode());
                source.setSourceIdentificationName(bo.getSourceIdentificationName());
                source.setEmissionSourceName(bo.getEmissionSourceName());
                source.setResponsibleDept(bo.getResponsibleDept());
                source.setDataSource(bo.getDataSource());
                source.setFactorKey(bo.getFactorKey());
                source.setSourceUnit(bo.getSourceUnit());
                source.setEnabledFlag(bo.getEnabledFlag());
                return source;
            }
        };
    }

    @Test
    void validatesCompanyCodeAgainstCompanyFactoryCompanyCode() {
        when(sysDeptMapper.selectList(any())).thenReturn(java.util.List.of(companyNode(), factoryNode("11", "Factory AA")));
        CeEmissionSourceCategory category = new CeEmissionSourceCategory();
        category.setCategorySk("101");
        category.setGhgScope("Scope 1");
        category.setGhgScopeCategory("Fixed combustion");
        when(emissionSourceCategoryMapper.selectList(any())).thenReturn(java.util.List.of(category));
        when(emissionSourceMapper.insert(any(CeEmissionSource.class))).thenReturn(1);

        service.insertByBo(validBo());

        ArgumentCaptor<CeEmissionSource> sourceCaptor = ArgumentCaptor.forClass(CeEmissionSource.class);
        verify(emissionSourceMapper).insert(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getCompanyName()).isEqualTo("Company A");
        assertThat(sourceCaptor.getValue().getScopeName()).isEqualTo("Scope 1");
        assertThat(sourceCaptor.getValue().getScopeSubcategory()).isEqualTo("Fixed combustion");
        assertThat(sourceCaptor.getValue().getSourceUnit()).isNull();
    }

    @Test
    void generatesSourceIdentificationCodeFromFactoryCodeAndSequence() {
        when(sysDeptMapper.selectList(any())).thenReturn(java.util.List.of(companyNode(), factoryNode("F001", "AA")));
        CeEmissionSourceCategory category = new CeEmissionSourceCategory();
        category.setCategorySk("101");
        category.setGhgScope("Scope 1");
        category.setGhgScopeCategory("Fixed combustion");
        when(emissionSourceCategoryMapper.selectList(any())).thenReturn(java.util.List.of(category));
        CeEmissionSource existing = new CeEmissionSource();
        existing.setSourceIdentificationCode("F001009");
        when(emissionSourceMapper.selectList(any())).thenReturn(java.util.List.of(existing));
        when(emissionSourceMapper.insert(any(CeEmissionSource.class))).thenReturn(1);

        CeEmissionSourceBo bo = validBo();
        bo.setFactoryName("AA");
        bo.setSourceIdentificationCode("USER_INPUT_SHOULD_NOT_WIN");
        service.insertByBo(bo);

        ArgumentCaptor<CeEmissionSource> sourceCaptor = ArgumentCaptor.forClass(CeEmissionSource.class);
        verify(emissionSourceMapper).insert(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getFactoryCode()).isEqualTo("F001");
        assertThat(sourceCaptor.getValue().getSourceIdentificationCode()).isEqualTo("F001010");
    }

    @Test
    void fillsFactorKeyAndUnitFromEfFactorByEmissionSourceName() {
        when(sysDeptMapper.selectList(any())).thenReturn(java.util.List.of(companyNode(), factoryNode("F001", "AA")));
        CeEmissionSourceCategory category = new CeEmissionSourceCategory();
        category.setCategorySk("101");
        category.setGhgScope("Scope 1");
        category.setGhgScopeCategory("Fixed combustion");
        when(emissionSourceCategoryMapper.selectList(any())).thenReturn(java.util.List.of(category));
        CeDimensionRecordVo factor = new CeDimensionRecordVo();
        factor.setRecordCode("EF-201-GAS");
        factor.setRecordName("天然气");
        factor.setSourceUnit("m3");
        factor.setFactorUnit("kgCO2e/m3");
        when(dimensionProjectionMapper.selectByDimensionCode("ef-factor")).thenReturn(java.util.List.of(factor));
        when(emissionSourceMapper.insert(any(CeEmissionSource.class))).thenReturn(1);

        CeEmissionSourceBo bo = validBo();
        bo.setEmissionSourceName("天然气");
        bo.setFactorKey(null);
        bo.setSourceUnit(null);
        service.insertByBo(bo);

        ArgumentCaptor<CeEmissionSource> sourceCaptor = ArgumentCaptor.forClass(CeEmissionSource.class);
        verify(emissionSourceMapper).insert(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getFactorKey()).isEqualTo("EF-201-GAS");
        assertThat(sourceCaptor.getValue().getSourceUnit()).isEqualTo("m3");
    }

    @Test
    void rejectsEmissionSourceOutsideEfFactorDimension() {
        when(sysDeptMapper.selectList(any())).thenReturn(java.util.List.of(companyNode(), factoryNode("F001", "AA")));
        CeEmissionSourceCategory category = new CeEmissionSourceCategory();
        category.setCategorySk("101");
        category.setGhgScope("Scope 1");
        category.setGhgScopeCategory("Fixed combustion");
        when(emissionSourceCategoryMapper.selectList(any())).thenReturn(java.util.List.of(category));
        when(dimensionProjectionMapper.selectByDimensionCode("ef-factor")).thenReturn(java.util.List.of());

        CeEmissionSourceBo bo = validBo();
        bo.setFactorKey(null);
        bo.setEmissionSourceName("Unknown Source");

        assertThatThrownBy(() -> service.insertByBo(bo))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("排放源必须来自201排放因子表");
    }

    @Test
    void queryListOrdersLatestSourceIdentificationCodeFirst() {
        service.queryList(new CeEmissionSourceBo());

        ArgumentCaptor<LambdaQueryWrapper<CeEmissionSource>> wrapperCaptor = ArgumentCaptor.captor();
        verify(emissionSourceMapper).selectVoList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
            .contains("sourceIdentificationCode DESC")
            .contains("id DESC")
            .doesNotContain("sourceIdentificationCode ASC");
    }

    @Test
    void sourceAEmissionSourceIdentificationDoesNotRequireSourceUnit() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(validBo(), AddGroup.class);

            assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("sourceUnit");
        }
    }

    @Test
    void sourceAEmissionSourceIdentificationRequiresManualNameAndEfFactorSource() {
        CeEmissionSourceBo bo = validBo();
        bo.setSourceIdentificationName(null);
        bo.setEmissionSourceName(null);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(bo, AddGroup.class);

            assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("sourceIdentificationName", "emissionSourceName");
        }
    }

    private CeEmissionSourceBo validBo() {
        CeEmissionSourceBo bo = new CeEmissionSourceBo();
        bo.setCompanyCode("1");
        bo.setFactoryName("AA");
        bo.setSourceCategoryKey("101");
        bo.setSourceIdentificationCode("111");
        bo.setSourceIdentificationName("444");
        bo.setEmissionSourceName("333");
        bo.setResponsibleDept("Carbon Dept");
        bo.setDataSource("22");
        bo.setFactorKey("1");
        bo.setEnabledFlag(Boolean.TRUE);
        return bo;
    }

    private CeDimensionRecordVo defaultFactor() {
        CeDimensionRecordVo factor = new CeDimensionRecordVo();
        factor.setRecordCode("1");
        factor.setRecordName("333");
        return factor;
    }

    private SysDept companyNode() {
        SysDept dept = new SysDept();
        dept.setDeptId(1L);
        dept.setParentId(100L);
        dept.setDeptCategory("1");
        dept.setDeptName("Company A");
        dept.setStatus("0");
        dept.setDelFlag("0");
        return dept;
    }

    private SysDept factoryNode(String code, String name) {
        SysDept dept = new SysDept();
        dept.setDeptId(2L);
        dept.setParentId(1L);
        dept.setDeptCategory("1");
        dept.setFactoryCode(code);
        dept.setDeptName(name);
        dept.setStatus("0");
        dept.setDelFlag("0");
        return dept;
    }

    private static void initializeEntityLambdaCache(Class<?> mapperType, Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(mapperType.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, entityType);
        LambdaUtils.installCache(tableInfo);
    }
}
