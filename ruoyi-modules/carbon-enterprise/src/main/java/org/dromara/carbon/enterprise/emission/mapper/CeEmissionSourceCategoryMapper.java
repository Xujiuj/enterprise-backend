package org.dromara.carbon.enterprise.emission.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.emission.domain.vo.CeEmissionSourceCategoryVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise emission source category mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeEmissionSourceCategoryMapper extends BaseMapperPlus<CeEmissionSourceCategory, CeEmissionSourceCategoryVo> {
}
