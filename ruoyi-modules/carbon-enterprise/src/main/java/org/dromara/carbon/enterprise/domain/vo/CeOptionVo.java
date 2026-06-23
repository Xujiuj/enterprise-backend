package org.dromara.carbon.enterprise.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise option item derived from existing business data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CeOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String label;

    private Object value;
}
