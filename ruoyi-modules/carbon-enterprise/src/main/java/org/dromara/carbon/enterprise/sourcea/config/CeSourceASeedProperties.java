package org.dromara.carbon.enterprise.sourcea.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Explicit one-shot Source(A) seed configuration.
 */
@Data
@ConfigurationProperties(prefix = "carbon.enterprise.source-a.seed")
public class CeSourceASeedProperties {

    /**
     * Disabled by default so production startup does not re-import reference workbooks accidentally.
     */
    private boolean enabled;

    /**
     * Directory that contains the Source(A) reference workbooks.
     */
    private String directory;
}
