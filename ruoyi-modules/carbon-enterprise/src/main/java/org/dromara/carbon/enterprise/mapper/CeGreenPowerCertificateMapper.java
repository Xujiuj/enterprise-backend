package org.dromara.carbon.enterprise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.domain.vo.CeGreenPowerCertificateVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * Enterprise local green electricity and certificate proof mapper.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeGreenPowerCertificateMapper extends BaseMapperPlus<CeGreenPowerCertificate, CeGreenPowerCertificateVo> {
}
