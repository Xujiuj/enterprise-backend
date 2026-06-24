package org.dromara.carbon.enterprise.service;

import org.dromara.carbon.enterprise.domain.bo.CeOptionQueryBo;
import org.dromara.carbon.enterprise.domain.vo.CeOptionVo;

import java.util.List;

/**
 * Enterprise controlled options.
 */
public interface ICeOptionService {

    List<CeOptionVo> listOptions(String optionCode, CeOptionQueryBo query);
}
