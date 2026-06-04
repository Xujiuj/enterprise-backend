package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeTemplateVersion;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateVersionVo;
import org.dromara.carbon.enterprise.mapper.CeTemplateVersionMapper;
import org.dromara.carbon.enterprise.service.ICeTemplateVersionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Enterprise Excel template version service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeTemplateVersionServiceImpl implements ICeTemplateVersionService {

    private final CeTemplateVersionMapper templateVersionMapper;

    @Override
    public List<CeTemplateVersionVo> listVersions() {
        return templateVersionMapper.selectVoList(
            Wrappers.lambdaQuery(CeTemplateVersion.class)
                .orderByDesc(CeTemplateVersion::getImportedTime)
                .orderByDesc(CeTemplateVersion::getId)
        );
    }

    @Override
    public CeTemplateVersionVo getById(Long id) {
        return templateVersionMapper.selectVoById(id);
    }
}
