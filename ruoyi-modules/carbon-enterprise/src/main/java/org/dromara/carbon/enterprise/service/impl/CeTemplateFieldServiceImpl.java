package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeTemplateField;
import org.dromara.carbon.enterprise.domain.vo.CeTemplateFieldVo;
import org.dromara.carbon.enterprise.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.service.ICeTemplateFieldService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Enterprise original field preservation inventory service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeTemplateFieldServiceImpl implements ICeTemplateFieldService {

    private final CeTemplateFieldMapper templateFieldMapper;

    @Override
    public List<CeTemplateFieldVo> listFields(Long sheetId) {
        LambdaQueryWrapper<CeTemplateField> wrapper = new LambdaQueryWrapper<CeTemplateField>()
            .eq(sheetId != null, CeTemplateField::getSheetId, sheetId)
            .orderByAsc(CeTemplateField::getSheetId)
            .orderByAsc(CeTemplateField::getFieldOrder)
            .orderByAsc(CeTemplateField::getId);
        return templateFieldMapper.selectVoList(wrapper);
    }

    @Override
    public CeTemplateFieldVo getById(Long id) {
        return templateFieldMapper.selectVoById(id);
    }
}
