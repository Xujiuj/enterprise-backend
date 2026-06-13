package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeDimensionRecord;
import org.dromara.carbon.enterprise.domain.vo.CeDimensionRecordVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise dimension record mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeDimensionRecordMapper extends BaseMapperPlus<CeDimensionRecord, CeDimensionRecordVo> {
}
