package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeCompanyFactory;
import org.dromara.carbon.enterprise.domain.vo.CeCompanyFactoryVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise company and factory relation mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeCompanyFactoryMapper extends BaseMapperPlus<CeCompanyFactory, CeCompanyFactoryVo> {
}
