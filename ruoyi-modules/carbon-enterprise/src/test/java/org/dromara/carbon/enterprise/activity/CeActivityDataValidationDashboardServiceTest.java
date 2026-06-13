package org.dromara.carbon.enterprise.activity;

import org.dromara.carbon.enterprise.domain.CeActivityData;
import org.dromara.carbon.enterprise.domain.CeDimensionRecord;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.mapper.CeDimensionRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.service.impl.CeActivityDataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeActivityDataValidationDashboardServiceTest {

    private CeActivityDataMapper activityDataMapper;
    private CeEmissionSourceMapper emissionSourceMapper;
    private CeGreenPowerCertificateMapper greenPowerCertificateMapper;
    private CeDimensionRecordMapper dimensionRecordMapper;
    private CeTemplateSheetMapper templateSheetMapper;
    private CeTemplateFieldMapper templateFieldMapper;
    private CeActivityDataServiceImpl service;

    @BeforeEach
    void setUp() {
        activityDataMapper = mock(CeActivityDataMapper.class);
        emissionSourceMapper = mock(CeEmissionSourceMapper.class);
        greenPowerCertificateMapper = mock(CeGreenPowerCertificateMapper.class);
        dimensionRecordMapper = mock(CeDimensionRecordMapper.class);
        templateSheetMapper = mock(CeTemplateSheetMapper.class);
        templateFieldMapper = mock(CeTemplateFieldMapper.class);
        CeCaptureRowMapper captureRowMapper = mock(CeCaptureRowMapper.class);
        CeCaptureCellMapper captureCellMapper = mock(CeCaptureCellMapper.class);
        CeCaptureBatchMapper captureBatchMapper = mock(CeCaptureBatchMapper.class);

        service = new CeActivityDataServiceImpl(
            activityDataMapper,
            emissionSourceMapper,
            greenPowerCertificateMapper,
            dimensionRecordMapper,
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
            emissionSource(1L, "ES-001", "总部外购电"),
            emissionSource(2L, "ES-002", "生产天然气")
        ));
        when(activityDataMapper.selectList(any())).thenReturn(List.of(
            activity(1L, "submitted", BigDecimal.TEN, "kWh", 100L),
            activity(2L, "draft", BigDecimal.ONE, "m3", null)
        ));
        when(greenPowerCertificateMapper.selectList(any())).thenReturn(List.of(invalidVoidedGreenCertificate()));
        when(dimensionRecordMapper.selectList(any())).thenReturn(List.of(invalidDenominatorFact()));
        when(templateSheetMapper.selectList(any())).thenReturn(List.of());
        when(templateFieldMapper.selectList(any())).thenReturn(List.of());

        CeActivityDataBo query = new CeActivityDataBo();
        query.setActivityPeriod("2026-01");

        CeActivityDataValidationDashboardVo dashboard = service.queryValidationDashboard(query);

        assertEquals("2026-01", dashboard.getActivityPeriod());
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
    }

    private CeEmissionSource emissionSource(Long id, String code, String name) {
        CeEmissionSource source = new CeEmissionSource();
        source.setId(id);
        source.setSourceCode(code);
        source.setSourceName(name);
        source.setFacilityName("一厂");
        source.setEnabledFlag(true);
        return source;
    }

    private CeActivityData activity(Long sourceId, String status, BigDecimal value, String unit, Long factorId) {
        CeActivityData activity = new CeActivityData();
        activity.setEmissionSourceId(sourceId);
        activity.setActivityPeriod("2026-01");
        activity.setActivityValue(value);
        activity.setActivityUnit(unit);
        activity.setFactorConfirmationId(factorId);
        activity.setDataStatus(status);
        return activity;
    }

    private CeGreenPowerCertificate invalidVoidedGreenCertificate() {
        CeGreenPowerCertificate certificate = new CeGreenPowerCertificate();
        certificate.setCertificateCode("GEC-001");
        certificate.setCertificateType("GEC");
        certificate.setEnergyPeriod("2026-01");
        certificate.setEnergyAmount(BigDecimal.ZERO);
        certificate.setProofStatus("voided");
        return certificate;
    }

    private CeDimensionRecord invalidDenominatorFact() {
        CeDimensionRecord fact = new CeDimensionRecord();
        fact.setDimensionCode("denominator-fact");
        fact.setRecordCode("DEN-001");
        fact.setRecordName("产量");
        fact.setField01("2026-01");
        fact.setField02("");
        fact.setField03("0");
        return fact;
    }
}
