package org.dromara.carbon.enterprise.license;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.impl.CeLicenseStateServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@SuppressWarnings({"rawtypes", "unchecked"})
class CeLicenseStateServiceTest {

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
}
