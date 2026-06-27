package org.dromara.carbon.enterprise.dimension.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.dimension.domain.CeBaseYear;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeBaseYearVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 基准年 Mapper接口.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeBaseYearMapper extends BaseMapperPlus<CeBaseYear, CeBaseYearVo> {
}
