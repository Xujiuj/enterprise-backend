package org.dromara.carbon.enterprise.service;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.domain.CeIntensityMetric;
import org.dromara.carbon.enterprise.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.mapper.CeCompanyFactoryMapper;
import org.dromara.carbon.enterprise.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorConfirmationMapper;
import org.dromara.carbon.enterprise.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.mapper.CeIntensityDenominatorFactMapper;
import org.dromara.carbon.enterprise.mapper.CeIntensityMetricMapper;
import org.dromara.carbon.enterprise.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.service.impl.CeOptionServiceImpl;
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
    private CeOptionServiceImpl service;

    @BeforeAll
    static void initializeLambdaCache() {
        initializeEntityLambdaCache(CeIntensityMetricMapper.class, CeIntensityMetric.class);
        initializeEntityLambdaCache(CeIntensityDenominatorFactMapper.class, CeIntensityDenominatorFact.class);
    }

    @BeforeEach
    void setUp() {
        intensityMetricMapper = mock(CeIntensityMetricMapper.class);
        denominatorFactMapper = mock(CeIntensityDenominatorFactMapper.class);
        service = new CeOptionServiceImpl(
            mock(CeActivityDataMapper.class),
            mock(CeCompanyFactoryMapper.class),
            mock(CeEmissionSourceMapper.class),
            mock(CeEmissionSourceCategoryMapper.class),
            mock(CeGreenPowerCertificateMapper.class),
            denominatorFactMapper,
            mock(CeFactorCacheRecordMapper.class),
            mock(CeFactorConfirmationMapper.class),
            intensityMetricMapper,
            mock(CeReportTemplateFileMapper.class),
            mock(CeCaptureBatchMapper.class),
            mock(CeDimensionProjectionMapper.class)
        );
    }

    @Test
    void intensityRuleCodeOptionsComeFromEnterpriseBusinessTables() {
        when(intensityMetricMapper.selectObjs(any())).thenReturn(List.of("RULE-A", "RULE-B"));
        when(denominatorFactMapper.selectObjs(any())).thenReturn(List.of("RULE-B", "RULE-C"));

        var options = service.listOptions("intensity-rule-code", null, null);

        assertThat(options)
            .extracting(option -> String.valueOf(option.getValue()))
            .containsExactly("RULE-A", "RULE-B", "RULE-C");
        assertThat(options)
            .extracting(option -> String.valueOf(option.getLabel()))
            .containsExactly("RULE-A", "RULE-B", "RULE-C");
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
