package org.dromara.carbon.enterprise.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.greenpower.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.activity.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataVo;
import org.dromara.carbon.enterprise.activity.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.greenpower.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.intensity.mapper.CeIntensityDenominatorFactMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.activity.service.impl.CeActivityDataServiceImpl;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeActivityDataValidationDashboardServiceTest {

    private CeActivityDataMapper activityDataMapper;
    private CeEmissionSourceMapper emissionSourceMapper;
    private CeGreenPowerCertificateMapper greenPowerCertificateMapper;
    private CeIntensityDenominatorFactMapper denominatorFactMapper;
    private CeTemplateSheetMapper templateSheetMapper;
    private CeTemplateFieldMapper templateFieldMapper;
    private CeActivityDataServiceImpl service;

    @BeforeAll
    static void initializeLambdaCache() {
        if (TableInfoHelper.getTableInfo(CeActivityData.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, CeActivityDataMapper.class.getName());
        assistant.setCurrentNamespace(CeActivityDataMapper.class.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, CeActivityData.class);
        LambdaUtils.installCache(tableInfo);
    }

    @BeforeEach
    void setUp() {
        activityDataMapper = mock(CeActivityDataMapper.class);
        emissionSourceMapper = mock(CeEmissionSourceMapper.class);
        greenPowerCertificateMapper = mock(CeGreenPowerCertificateMapper.class);
        denominatorFactMapper = mock(CeIntensityDenominatorFactMapper.class);
        templateSheetMapper = mock(CeTemplateSheetMapper.class);
        templateFieldMapper = mock(CeTemplateFieldMapper.class);
        CeCaptureRowMapper captureRowMapper = mock(CeCaptureRowMapper.class);
        CeCaptureCellMapper captureCellMapper = mock(CeCaptureCellMapper.class);
        CeCaptureBatchMapper captureBatchMapper = mock(CeCaptureBatchMapper.class);

        service = new CeActivityDataServiceImpl(
            activityDataMapper,
            emissionSourceMapper,
            greenPowerCertificateMapper,
            denominatorFactMapper,
            templateSheetMapper,
            templateFieldMapper,
            captureRowMapper,
            captureCellMapper,
            captureBatchMapper
        );
    }

    @Test
    void buildsValidationDashboardFromEnterpriseActivityData() {
        when(emissionSourceMapper.selectList(any())).thenReturn(List.of(
            emissionSource(1L, "ES-001", "Purchased electricity"),
            emissionSource(2L, "ES-002", "Natural gas")
        ));
        when(activityDataMapper.selectList(any())).thenReturn(List.of(
            activity("ES-001", "submitted", BigDecimal.TEN, "kWh", "EF-001"),
            activity("ES-002", "draft", BigDecimal.ONE, "m3", null)
        ));
        when(greenPowerCertificateMapper.selectList(any())).thenReturn(List.of(invalidVoidedGreenCertificate()));
        when(denominatorFactMapper.selectList(any())).thenReturn(List.of(invalidDenominatorFact()));
        when(templateSheetMapper.selectList(any())).thenReturn(List.of());
        when(templateFieldMapper.selectList(any())).thenReturn(List.of());

        CeActivityDataBo query = new CeActivityDataBo();
        query.setActivityYear(2026);
        query.setActivityMonth(1);

        CeActivityDataValidationDashboardVo dashboard = service.queryValidationDashboard(query);

        assertEquals(2026, dashboard.getActivityYear());
        assertEquals(1, dashboard.getActivityMonth());
        assertEquals("2026-02-05", dashboard.getDueDate());
        assertEquals(2, dashboard.getExpectedItems());
        assertEquals(4, dashboard.getValidatedRecordCount());
        assertEquals(1, dashboard.getSubmittedItems());
        assertEquals(1, dashboard.getDraftItems());
        assertEquals(0, dashboard.getMissingItems());
        assertEquals(3, dashboard.getAbnormalItems());
        assertEquals(new BigDecimal("100.0"), dashboard.getAccuracyRate());
        assertEquals(new BigDecimal("25.0"), dashboard.getPassRate());
        assertEquals(1, dashboard.getSubmissions().size());
        assertEquals(2, dashboard.getSubmissions().get(0).getExpectedCount());
        assertEquals(1, dashboard.getSubmissions().get(0).getSubmittedCount());
        assertEquals(0, dashboard.getSubmissions().get(0).getMissingCount());
        assertEquals(1, dashboard.getSubmissions().get(0).getWarningCount());
        assertEquals("draft", dashboard.getSubmissions().get(0).getSubmissionStatus());
        assertEquals(5, dashboard.getIssues().size());
        assertEquals("UNSUBMITTED_ACTIVITY_DATA", dashboard.getIssues().get(0).getRuleCode());
        assertEquals("活动数据未提交", dashboard.getIssues().get(0).getRuleName());
        assertEquals("活动数据仍处于草稿状态。", dashboard.getIssues().get(0).getDescription());
        assertEquals("请复核草稿数据并提交。", dashboard.getIssues().get(0).getSuggestion());
    }

    @Test
    void listsNewestActivityDataFirstWhenNoExplicitSortFieldExists() {
        when(activityDataMapper.selectVoPage(any(), any())).thenReturn(new Page<CeActivityDataVo>(1, 10));

        service.queryPageList(new CeActivityDataBo(), new PageQuery(10, 1));

        ArgumentCaptor<LambdaQueryWrapper<CeActivityData>> wrapperCaptor = ArgumentCaptor.captor();
        verify(activityDataMapper).selectVoPage(any(), wrapperCaptor.capture());
        String orderSegment = wrapperCaptor.getValue().getExpression().getOrderBy().getSqlSegment();
        Assertions.assertTrue(orderSegment.contains("create_time DESC"), orderSegment);
        Assertions.assertTrue(orderSegment.contains("id DESC"), orderSegment);
    }

    private CeEmissionSource emissionSource(Long id, String code, String name) {
        CeEmissionSource source = new CeEmissionSource();
        source.setId(id);
        source.setSourceIdentificationCode(code);
        source.setSourceIdentificationName(name);
        source.setEmissionSourceName(name);
        source.setFactoryName("Factory A");
        source.setEnabledFlag(true);
        return source;
    }

    private CeActivityData activity(String sourceCode, String status, BigDecimal value, String unit, String factorKey) {
        CeActivityData activity = new CeActivityData();
        activity.setSourceIdentificationCode(sourceCode);
        activity.setActivityYear(2026);
        activity.setActivityMonth(1);
        activity.setActivityValue(value);
        activity.setActivityUnit(unit);
        activity.setFactorKey(factorKey);
        activity.setDataStatus(status);
        return activity;
    }

    private CeGreenPowerCertificate invalidVoidedGreenCertificate() {
        CeGreenPowerCertificate certificate = new CeGreenPowerCertificate();
        certificate.setCertificateCode("GEC-001");
        certificate.setElectricityType("GEC");
        certificate.setActivityYear(2026);
        certificate.setActivityMonth(1);
        certificate.setQuantityKwh(BigDecimal.ZERO);
        certificate.setProofStatus("voided");
        return certificate;
    }

    private CeIntensityDenominatorFact invalidDenominatorFact() {
        CeIntensityDenominatorFact fact = new CeIntensityDenominatorFact();
        fact.setFactoryCode("FAC-001");
        fact.setDenominatorMetricName("Output");
        fact.setFactYear(2026);
        fact.setFactMonth(1);
        fact.setDenominatorType("");
        fact.setDenominatorValue(BigDecimal.ZERO);
        return fact;
    }
}
