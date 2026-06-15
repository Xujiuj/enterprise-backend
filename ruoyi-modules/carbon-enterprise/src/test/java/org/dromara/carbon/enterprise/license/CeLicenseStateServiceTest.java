package org.dromara.carbon.enterprise.license;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.impl.CeLicenseStateServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@SuppressWarnings({"rawtypes", "unchecked"})
class CeLicenseStateServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        if (TableInfoHelper.getTableInfo(CeLicenseState.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, CeLicenseStateMapper.class.getName());
        assistant.setCurrentNamespace(CeLicenseStateMapper.class.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, CeLicenseState.class);
        LambdaUtils.installCache(tableInfo);
    }

    @Test
    void currentLicenseStateUsesLatestObservedState() {
        CeLicenseStateMapper mapper = mock(CeLicenseStateMapper.class);
        CeLicenseStateVo expected = new CeLicenseStateVo();
        IPage<CeLicenseStateVo> page = new Page<>(1, 1, false);
        page.setRecords(List.of(expected));
        when(mapper.selectVoPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        CeLicenseStateServiceImpl service = new CeLicenseStateServiceImpl(mapper);

        CeLicenseStateVo actual = service.queryCurrent();

        assertSame(expected, actual);
        verify(mapper).selectVoPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void currentLicenseStateAllowsNoExistingState() {
        CeLicenseStateMapper mapper = mock(CeLicenseStateMapper.class);
        when(mapper.selectVoPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>(1, 1, false));

        CeLicenseStateServiceImpl service = new CeLicenseStateServiceImpl(mapper);

        assertNull(service.queryCurrent());
    }

    @Test
    void expiresOnlyExpiredValidLicenseRows() {
        CeLicenseStateMapper mapper = mock(CeLicenseStateMapper.class);
        when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(2);
        CeLicenseStateServiceImpl service = new CeLicenseStateServiceImpl(mapper);
        Date now = Date.from(Instant.parse("2026-06-14T00:00:00Z"));

        int expired = service.expireValidLicenses(now);

        assertEquals(2, expired);
        ArgumentCaptor<CeLicenseState> stateCaptor = ArgumentCaptor.forClass(CeLicenseState.class);
        ArgumentCaptor<LambdaUpdateWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper).update(stateCaptor.capture(), wrapperCaptor.capture());
        CeLicenseState updateEntity = stateCaptor.getValue();
        assertNull(updateEntity.getId());
        assertNull(updateEntity.getLicenseId());
        assertNull(updateEntity.getCustomerId());
        assertNull(updateEntity.getPackageId());
        assertNull(updateEntity.getPackageName());
        assertNull(updateEntity.getInstallId());
        assertNull(updateEntity.getKeyId());
        assertNull(updateEntity.getAlgorithm());
        assertNull(updateEntity.getSchemaVersion());
        assertNull(updateEntity.getValidFrom());
        assertNull(updateEntity.getValidTo());
        assertNull(updateEntity.getLastVerifiedTime());
        assertNull(updateEntity.getMaxObservedTime());
        assertNull(updateEntity.getFeatureCodes());
        assertNull(updateEntity.getPayloadDigest());
        assertNull(updateEntity.getCurrentSummary());
        assertEquals("EXPIRED", updateEntity.getLicenseStatus());

        LambdaUpdateWrapper wrapper = wrapperCaptor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        Collection<Object> paramValues = wrapper.getParamNameValuePairs().values();
        assertTrue(paramValues.contains("VALID"));
        assertTrue(paramValues.contains(now));
        assertTrue(sqlSegment.contains("license_status"));
        assertTrue(sqlSegment.contains("="));
        assertTrue(sqlSegment.contains("valid_to"));
        assertTrue(sqlSegment.contains("<"));
        assertEquals(
            List.of("expired-valid"),
            List.of(
                representativeRow("expired-valid", "VALID", Date.from(Instant.parse("2026-06-13T23:59:59Z"))),
                representativeRow("unexpired-valid", "VALID", Date.from(Instant.parse("2026-06-14T00:00:00Z"))),
                representativeRow("future-valid", "VALID", Date.from(Instant.parse("2026-06-14T00:00:01Z"))),
                representativeRow("expired-expired", "EXPIRED", Date.from(Instant.parse("2026-06-13T23:59:59Z"))),
                representativeRow("expired-revoked", "REVOKED", Date.from(Instant.parse("2026-06-13T23:59:59Z"))),
                representativeRow("expired-invalid", "INVALID", Date.from(Instant.parse("2026-06-13T23:59:59Z")))
            ).stream()
                .filter(row -> matchesExpireValidLicensesWrapper(row, wrapper))
                .map(RepresentativeLicenseRow::name)
                .toList()
        );
    }

    @Test
    void expireValidLicensesRejectsNullEvaluationTime() {
        CeLicenseStateMapper mapper = mock(CeLicenseStateMapper.class);
        CeLicenseStateServiceImpl service = new CeLicenseStateServiceImpl(mapper);

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> service.expireValidLicenses(null));

        assertEquals("evaluationTime cannot be null", exception.getMessage());
        verify(mapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    private static RepresentativeLicenseRow representativeRow(String name, String status, Date validTo) {
        return new RepresentativeLicenseRow(name, status, validTo);
    }

    private static boolean matchesExpireValidLicensesWrapper(RepresentativeLicenseRow row, LambdaUpdateWrapper wrapper) {
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        Object statusParam = null;
        Object validToParam = null;
        for (Object value : params.values()) {
            if ("VALID".equals(value)) {
                statusParam = value;
            } else if (value instanceof Date) {
                validToParam = value;
            }
        }
        assertEquals("VALID", statusParam);
        assertEquals(Date.from(Instant.parse("2026-06-14T00:00:00Z")), validToParam);
        return statusParam.equals(row.licenseStatus())
            && row.validTo().before((Date) validToParam);
    }

    private record RepresentativeLicenseRow(String name, String licenseStatus, Date validTo) {
    }
}
