package org.dromara.carbon.enterprise.emission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.dimension.domain.CeCompanyFactory;
import org.dromara.carbon.enterprise.dimension.mapper.CeCompanyFactoryMapper;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.emission.domain.bo.CeEmissionSourceBo;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.emission.service.impl.CeEmissionSourceServiceImpl;
import org.dromara.common.core.validate.AddGroup;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeEmissionSourceServiceTest {

    private CeEmissionSourceMapper emissionSourceMapper;
    private CeCompanyFactoryMapper companyFactoryMapper;
    private CeEmissionSourceCategoryMapper emissionSourceCategoryMapper;
    private CeEmissionSourceServiceImpl service;

    @BeforeAll
    static void initializeLambdaCache() {
        initializeEntityLambdaCache(CeCompanyFactoryMapper.class, CeCompanyFactory.class);
        initializeEntityLambdaCache(CeEmissionSourceCategoryMapper.class, CeEmissionSourceCategory.class);
    }

    @BeforeEach
    void setUp() {
        emissionSourceMapper = mock(CeEmissionSourceMapper.class);
        companyFactoryMapper = mock(CeCompanyFactoryMapper.class);
        emissionSourceCategoryMapper = mock(CeEmissionSourceCategoryMapper.class);
        service = new CeEmissionSourceServiceImpl(emissionSourceMapper, companyFactoryMapper, emissionSourceCategoryMapper) {
            @Override
            protected CeEmissionSource toEntity(CeEmissionSourceBo bo) {
                CeEmissionSource source = new CeEmissionSource();
                source.setCompanyCode(bo.getCompanyCode());
                source.setCompanyName(bo.getCompanyName());
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
        CeCompanyFactory company = new CeCompanyFactory();
        company.setCompanyCode("1");
        company.setCompanyName("Company A");
        company.setFactoryCode("11");
        company.setFactoryName("Factory AA");
        when(companyFactoryMapper.selectList(any())).thenReturn(java.util.List.of(company));
        CeEmissionSourceCategory category = new CeEmissionSourceCategory();
        category.setCategorySk("101");
        category.setGhgScope("Scope 1");
        category.setGhgScopeCategory("Fixed combustion");
        when(emissionSourceCategoryMapper.selectList(any())).thenReturn(java.util.List.of(category));
        when(emissionSourceMapper.insert(any(CeEmissionSource.class))).thenReturn(1);

        service.insertByBo(validBo());

        ArgumentCaptor<LambdaQueryWrapper<CeCompanyFactory>> wrapperCaptor = ArgumentCaptor.captor();
        verify(companyFactoryMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("companyCode");
        assertThat(wrapperCaptor.getValue().getSqlSegment()).doesNotContain("factoryCode");
        ArgumentCaptor<CeEmissionSource> sourceCaptor = ArgumentCaptor.forClass(CeEmissionSource.class);
        verify(emissionSourceMapper).insert(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getCompanyName()).isEqualTo("Company A");
        assertThat(sourceCaptor.getValue().getScopeName()).isEqualTo("Scope 1");
        assertThat(sourceCaptor.getValue().getScopeSubcategory()).isEqualTo("Fixed combustion");
        assertThat(sourceCaptor.getValue().getSourceUnit()).isNull();
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
