package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeFactorConfirmation;
import org.dromara.carbon.enterprise.domain.vo.CeFactorConfirmationVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local emission factor confirmation mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeFactorConfirmationMapper extends BaseMapperPlus<CeFactorConfirmation, CeFactorConfirmationVo> {
}
