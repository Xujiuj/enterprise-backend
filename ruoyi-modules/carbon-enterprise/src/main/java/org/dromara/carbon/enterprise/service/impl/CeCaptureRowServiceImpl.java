package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.domain.bo.CeCaptureRowBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureRowVo;
import org.dromara.carbon.enterprise.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.service.ICeCaptureRowService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

/**
 * Enterprise local data capture row service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeCaptureRowServiceImpl implements ICeCaptureRowService {

    private final CeCaptureRowMapper captureRowMapper;

    @Override
    public TableDataInfo<CeCaptureRowVo> selectPageRowList(CeCaptureRowBo row, PageQuery pageQuery) {
        LambdaQueryWrapper<CeCaptureRow> wrapper = buildQueryWrapper(row)
            .orderByDesc(CeCaptureRow::getCreateTime)
            .orderByDesc(CeCaptureRow::getId);
        IPage<CeCaptureRowVo> page = captureRowMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public CeCaptureRowVo selectRowById(Long id) {
        return captureRowMapper.selectVoById(id);
    }

    private LambdaQueryWrapper<CeCaptureRow> buildQueryWrapper(CeCaptureRowBo row) {
        return new LambdaQueryWrapper<CeCaptureRow>()
            .eq(row.getBatchId() != null, CeCaptureRow::getBatchId, row.getBatchId())
            .eq(row.getSheetId() != null, CeCaptureRow::getSheetId, row.getSheetId())
            .eq(row.getRowStatus() != null, CeCaptureRow::getRowStatus, row.getRowStatus())
            .eq(row.getValidationLevel() != null, CeCaptureRow::getValidationLevel, row.getValidationLevel());
    }
}
