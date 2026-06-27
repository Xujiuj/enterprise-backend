package org.dromara.carbon.enterprise.shared.service;

import org.dromara.carbon.enterprise.shared.option.domain.bo.CeOptionQueryBo;
import org.dromara.carbon.enterprise.shared.option.domain.vo.CeOptionVo;

import java.util.List;

/**
 * Enterprise controlled options.
 */
public interface ICeOptionService {

    List<CeOptionVo> listOptions(String optionCode, CeOptionQueryBo query);
}
