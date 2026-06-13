package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeGreenPowerCertificateBo;
import org.dromara.carbon.enterprise.domain.vo.CeGreenPowerCertificateVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local green electricity and certificate proof service.
 */
public interface ICeGreenPowerCertificateService {

    TableDataInfo<CeGreenPowerCertificateVo> queryPageList(CeGreenPowerCertificateBo bo, PageQuery pageQuery);

    List<CeGreenPowerCertificateVo> queryList(CeGreenPowerCertificateBo bo);

    CeGreenPowerCertificateVo queryById(Long id);

    Boolean insertByBo(CeGreenPowerCertificateBo bo);

    Boolean updateByBo(CeGreenPowerCertificateBo bo);

    Boolean deleteByIds(Collection<Long> ids);
}
