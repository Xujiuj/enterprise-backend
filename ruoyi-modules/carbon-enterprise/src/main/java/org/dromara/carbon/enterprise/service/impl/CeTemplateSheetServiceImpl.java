package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateSheetVo;
import org.dromara.carbon.enterprise.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.service.ICeTemplateSheetService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Enterprise source workbook sheet inventory service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeTemplateSheetServiceImpl implements ICeTemplateSheetService {

    private final CeTemplateSheetMapper templateSheetMapper;

    @Override
    public List<CeTemplateSheetVo> listSheets(Long templateVersionId) {
        LambdaQueryWrapper<CeTemplateSheet> wrapper = new LambdaQueryWrapper<CeTemplateSheet>()
            .eq(templateVersionId != null, CeTemplateSheet::getTemplateVersionId, templateVersionId)
            .orderByAsc(CeTemplateSheet::getId);
        return templateSheetMapper.selectVoList(wrapper);
    }

    @Override
    public CeTemplateSheetVo getById(Long id) {
        return templateSheetMapper.selectVoById(id);
    }
}
