package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.domain.bo.CeCaptureCellBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureCellVo;
import org.dromara.carbon.enterprise.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.service.ICeCaptureCellService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

/**
 * Enterprise local data capture cell service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeCaptureCellServiceImpl implements ICeCaptureCellService {

    private final CeCaptureCellMapper captureCellMapper;

    @Override
    public TableDataInfo<CeCaptureCellVo> selectPageCellList(CeCaptureCellBo cell, PageQuery pageQuery) {
        LambdaQueryWrapper<CeCaptureCell> wrapper = buildQueryWrapper(cell)
            .orderByAsc(CeCaptureCell::getRowId)
            .orderByAsc(CeCaptureCell::getFieldId)
            .orderByAsc(CeCaptureCell::getId);
        IPage<CeCaptureCellVo> page = captureCellMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public CeCaptureCellVo selectCellById(Long id) {
        return captureCellMapper.selectVoById(id);
    }

    private LambdaQueryWrapper<CeCaptureCell> buildQueryWrapper(CeCaptureCellBo cell) {
        return new LambdaQueryWrapper<CeCaptureCell>()
            .eq(cell.getRowId() != null, CeCaptureCell::getRowId, cell.getRowId())
            .eq(cell.getFieldId() != null, CeCaptureCell::getFieldId, cell.getFieldId())
            .eq(cell.getValueStatus() != null, CeCaptureCell::getValueStatus, cell.getValueStatus());
    }
}
