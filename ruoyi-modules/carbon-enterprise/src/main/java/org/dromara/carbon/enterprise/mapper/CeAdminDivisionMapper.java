package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeAdminDivision;
import org.dromara.carbon.enterprise.domain.vo.CeAdminDivisionVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 行政区划 Mapper接口.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeAdminDivisionMapper extends BaseMapperPlus<CeAdminDivision, CeAdminDivisionVo> {
}
