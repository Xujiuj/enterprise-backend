package org.dromara.carbon.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Enterprise local license runtime state.
 */
@Data
@TableName("ce_license_state")
public class CeLicenseState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String licenseId;

    private String customerId;

    private String installId;

    private String keyId;

    private String algorithm;

    private String schemaVersion;

    private Date validFrom;

    private Date validTo;

    private Date lastVerifiedTime;

    private Date maxObservedTime;

    private String licenseStatus;
}
