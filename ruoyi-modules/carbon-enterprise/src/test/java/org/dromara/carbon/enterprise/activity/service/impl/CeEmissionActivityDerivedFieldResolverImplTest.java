package org.dromara.carbon.enterprise.activity.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
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
class CeEmissionActivityDerivedFieldResolverImplTest {

    private CeEmissionSourceMapper emissionSourceMapper;
    private CeDimensionProjectionMapper dimensionProjectionMapper;
    private CeEmissionActivityDerivedFieldResolverImpl resolver;

    @BeforeAll
    static void initializeLambdaCache() {
        if (TableInfoHelper.getTableInfo(CeEmissionSource.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, CeEmissionSourceMapper.class.getName());
        assistant.setCurrentNamespace(CeEmissionSourceMapper.class.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, CeEmissionSource.class);
        LambdaUtils.installCache(tableInfo);
    }

    @BeforeEach
    void setUp() {
        emissionSourceMapper = mock(CeEmissionSourceMapper.class);
        dimensionProjectionMapper = mock(CeDimensionProjectionMapper.class);
        resolver = new CeEmissionActivityDerivedFieldResolverImpl(emissionSourceMapper, dimensionProjectionMapper);
    }

    @Test
    void resolvesActivityUnitFromLinkedEfFactorWhenSourceUnitIsEmpty() {
        CeEmissionSource source = new CeEmissionSource();
        source.setSourceIdentificationCode("111");
        source.setEmissionSourceName("333");
        source.setSourceUnit(null);
        source.setFactorKey("1");
        source.setEnabledFlag(true);
        CeDimensionRecordVo factor = new CeDimensionRecordVo();
        factor.setRecordCode("1");
        factor.setRecordName("333");
        factor.setFactorUnit("kg");
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of(source));
        when(dimensionProjectionMapper.selectByDimensionCode("ef-factor")).thenReturn(List.of(factor));

        var row = resolver.resolve("111");

        assertThat(row).isPresent();
        assertThat(row.orElseThrow().getUnit()).isEqualTo("kg");
        assertThat(row.orElseThrow().getEmissionFactorCode()).isEqualTo("1");
    }
}
