package org.dromara.carbon.enterprise.shared.option.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

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

    private Map<String, Object> record;

    public CeOptionVo(String label, Object value) {
        this.label = label;
        this.value = value;
    }
}
