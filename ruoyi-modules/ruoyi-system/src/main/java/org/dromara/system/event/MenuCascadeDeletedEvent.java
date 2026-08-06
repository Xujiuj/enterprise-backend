package org.dromara.system.event;

import java.util.Map;
import java.util.Set;

/**
 * Published before a resolved menu tree is deleted.
 *
 * <p>Listeners can remove module-specific resources in the same transaction
 * without making the system module depend on those modules.</p>
 */
public record MenuCascadeDeletedEvent(
    Set<Long> menuIds,
    Map<Long, Long> parentIds,
    Set<Long> roleIds
) {
}
