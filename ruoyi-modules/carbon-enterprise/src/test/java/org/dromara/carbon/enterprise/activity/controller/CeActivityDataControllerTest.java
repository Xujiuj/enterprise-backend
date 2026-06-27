package org.dromara.carbon.enterprise.activity.controller;

import org.dromara.carbon.enterprise.activity.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.activity.domain.bo.CeActivityDataStatusBo;
import org.dromara.carbon.enterprise.shared.service.ICeActivityDataService;
import org.dromara.common.core.domain.R;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class CeActivityDataControllerTest {

    @Test
    void rejectsRawActivityDataAdds() {
        ICeActivityDataService service = mock(ICeActivityDataService.class);
        CeActivityDataController controller = new CeActivityDataController(service);

        assertRawWriteRejected(controller.add(new CeActivityDataBo()));
        verifyNoInteractions(service);
    }

    @Test
    void updatesDeletesAndSubmitsExistingActivityData() {
        ICeActivityDataService service = mock(ICeActivityDataService.class);
        CeActivityDataController controller = new CeActivityDataController(service);
        CeActivityDataBo bo = new CeActivityDataBo();
        bo.setId(1L);
        CeActivityDataStatusBo statusBo = new CeActivityDataStatusBo();
        statusBo.setIds(List.of(1L, 2L));
        statusBo.setDataStatus("submitted");

        when(service.updateByBo(bo)).thenReturn(true);
        when(service.deleteByIds(List.of(1L, 2L))).thenReturn(true);
        when(service.updateStatusByIds(List.of(1L, 2L), "submitted")).thenReturn(true);

        assertThat(controller.edit(bo).getCode()).isEqualTo(R.SUCCESS);
        assertThat(controller.remove(new Long[] {1L, 2L}).getCode()).isEqualTo(R.SUCCESS);
        assertThat(controller.updateStatus(statusBo).getCode()).isEqualTo(R.SUCCESS);
        verify(service).updateByBo(bo);
        verify(service).deleteByIds(List.of(1L, 2L));
        verify(service).updateStatusByIds(List.of(1L, 2L), "submitted");
    }

    @Test
    void rejectsUnsupportedActivityDataStatus() {
        ICeActivityDataService service = mock(ICeActivityDataService.class);
        CeActivityDataController controller = new CeActivityDataController(service);
        CeActivityDataStatusBo statusBo = new CeActivityDataStatusBo();
        statusBo.setIds(List.of(1L));
        statusBo.setDataStatus("invalid");

        R<Void> response = controller.updateStatus(statusBo);

        assertThat(response.getCode()).isEqualTo(R.FAIL);
        verifyNoInteractions(service);
    }

    private void assertRawWriteRejected(R<Void> response) {
        assertThat(response.getCode()).isEqualTo(R.FAIL);
        assertThat(response.getMsg()).contains("sheet_656");
    }
}
