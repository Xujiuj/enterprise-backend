package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.domain.vo.CeEmissionSourceVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local emission source mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeEmissionSourceMapper extends BaseMapperPlus<CeEmissionSource, CeEmissionSourceVo> {
}
