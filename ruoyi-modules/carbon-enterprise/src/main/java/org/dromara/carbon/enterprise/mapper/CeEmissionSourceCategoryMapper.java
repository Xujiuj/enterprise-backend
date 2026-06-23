package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.domain.vo.CeEmissionSourceCategoryVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise emission source category mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeEmissionSourceCategoryMapper extends BaseMapperPlus<CeEmissionSourceCategory, CeEmissionSourceCategoryVo> {
}
