package org.dromara.carbon.enterprise.controller;

import org.dromara.carbon.enterprise.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.service.ICeActivityDataService;
import org.dromara.common.core.domain.R;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("dev")
class CeActivityDataControllerTest {

    @Test
    void rejectsRawActivityDataWrites() {
        ICeActivityDataService service = mock(ICeActivityDataService.class);
        CeActivityDataController controller = new CeActivityDataController(service);

        assertRawWriteRejected(controller.add(new CeActivityDataBo()));
        assertRawWriteRejected(controller.edit(new CeActivityDataBo()));
        assertRawWriteRejected(controller.remove(new Long[] {1L}));
        verifyNoInteractions(service);
    }

    private void assertRawWriteRejected(R<Void> response) {
        assertThat(response.getCode()).isEqualTo(R.FAIL);
        assertThat(response.getMsg()).contains("sheet_656");
    }
}
