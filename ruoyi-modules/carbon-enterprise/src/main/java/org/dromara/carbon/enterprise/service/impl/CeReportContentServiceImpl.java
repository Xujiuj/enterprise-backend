package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeReportContent;
import org.dromara.carbon.enterprise.domain.vo.CeReportContentVo;
import org.dromara.carbon.enterprise.mapper.CeReportContentMapper;
import org.dromara.carbon.enterprise.service.ICeReportContentService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Enterprise local report content catalog service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeReportContentServiceImpl implements ICeReportContentService {

    private final CeReportContentMapper reportContentMapper;

    @Override
    public List<CeReportContentVo> listContent() {
        return reportContentMapper.selectVoList(new LambdaQueryWrapper<CeReportContent>()
            .orderByAsc(CeReportContent::getDisplayOrder)
            .orderByAsc(CeReportContent::getId));
    }
}
