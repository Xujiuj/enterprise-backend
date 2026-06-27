package org.dromara.carbon.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.report.domain.CeReportContent;
import org.dromara.carbon.enterprise.report.domain.vo.CeReportContentVo;
import org.dromara.carbon.enterprise.report.mapper.CeReportContentMapper;
import org.dromara.carbon.enterprise.shared.service.ICeReportContentService;
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
