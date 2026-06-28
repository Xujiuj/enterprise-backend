package org.dromara.carbon.enterprise.license.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class CeLicenseStateTest {

    @Test
    void idUsesDatabaseIdentityGeneration() throws NoSuchFieldException {
        TableId tableId = CeLicenseState.class.getDeclaredField("id").getAnnotation(TableId.class);

        assertEquals(IdType.AUTO, tableId.type());
    }
}
