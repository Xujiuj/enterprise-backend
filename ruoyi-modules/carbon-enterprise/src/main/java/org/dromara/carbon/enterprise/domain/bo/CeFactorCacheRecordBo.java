package org.dromara.carbon.enterprise.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Enterprise local factor cache record business object.
 */
@Data
@AutoMapper(target = CeFactorCacheRecord.class, reverseConvertGenerate = false)
public class CeFactorCacheRecordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "id cannot be null", groups = { EditGroup.class })
    private Long id;

    @NotNull(message = "cacheVersionId cannot be null", groups = { AddGroup.class, EditGroup.class })
    private Long cacheVersionId;

    @NotBlank(message = "factorCode cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorCode;

    @NotBlank(message = "factorName cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorName;

    @NotBlank(message = "factorCategory cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorCategory;

    @NotNull(message = "factorValue cannot be null", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal factorValue;

    @NotBlank(message = "factorUnit cannot be blank", groups = { AddGroup.class, EditGroup.class })
    private String factorUnit;

    private String sourceRef;

    private Boolean enabledFlag;

    private Date syncedTime;
}
