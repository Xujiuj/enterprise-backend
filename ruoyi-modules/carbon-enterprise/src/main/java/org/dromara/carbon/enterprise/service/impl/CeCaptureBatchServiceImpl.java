package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.domain.bo.CeCaptureBatchBo;
import org.dromara.carbon.enterprise.domain.vo.CeCaptureBatchVo;
import org.dromara.carbon.enterprise.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.service.ICeCaptureBatchService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

/**
 * Enterprise local data capture batch service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeCaptureBatchServiceImpl implements ICeCaptureBatchService {

    private final CeCaptureBatchMapper captureBatchMapper;

    @Override
    public TableDataInfo<CeCaptureBatchVo> selectPageBatchList(CeCaptureBatchBo batch, PageQuery pageQuery) {
        LambdaQueryWrapper<CeCaptureBatch> wrapper = buildQueryWrapper(batch)
            .orderByDesc(CeCaptureBatch::getCreateTime)
            .orderByDesc(CeCaptureBatch::getId);
        IPage<CeCaptureBatchVo> page = captureBatchMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public CeCaptureBatchVo selectBatchById(Long id) {
        return captureBatchMapper.selectVoById(id);
    }

    private LambdaQueryWrapper<CeCaptureBatch> buildQueryWrapper(CeCaptureBatchBo batch) {
        return new LambdaQueryWrapper<CeCaptureBatch>()
            .eq(batch.getTemplateVersionId() != null, CeCaptureBatch::getTemplateVersionId, batch.getTemplateVersionId())
            .eq(batch.getModuleCode() != null, CeCaptureBatch::getModuleCode, batch.getModuleCode())
            .eq(batch.getSourceMode() != null, CeCaptureBatch::getSourceMode, batch.getSourceMode())
            .eq(batch.getBatchStatus() != null, CeCaptureBatch::getBatchStatus, batch.getBatchStatus())
            .eq(batch.getValidationStatus() != null, CeCaptureBatch::getValidationStatus, batch.getValidationStatus());
    }
}
