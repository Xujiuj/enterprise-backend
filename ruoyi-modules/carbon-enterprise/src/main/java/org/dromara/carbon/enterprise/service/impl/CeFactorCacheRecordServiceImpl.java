package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.domain.bo.CeFactorCacheRecordBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheRecordVo;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheVersionMapper;
import org.dromara.carbon.enterprise.service.ICeFactorCacheRecordService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local factor cache record service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeFactorCacheRecordServiceImpl implements ICeFactorCacheRecordService {

    private final CeFactorCacheRecordMapper factorCacheRecordMapper;
    private final CeFactorCacheVersionMapper factorCacheVersionMapper;

    @Override
    public TableDataInfo<CeFactorCacheRecordVo> queryPageList(CeFactorCacheRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeFactorCacheRecord> wrapper = buildQueryWrapper(bo)
            .orderByAsc(CeFactorCacheRecord::getFactorCode)
            .orderByAsc(CeFactorCacheRecord::getId);
        IPage<CeFactorCacheRecordVo> page = factorCacheRecordMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeFactorCacheRecordVo> queryList(CeFactorCacheRecordBo bo) {
        return factorCacheRecordMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeFactorCacheRecord::getFactorCode)
            .orderByAsc(CeFactorCacheRecord::getId));
    }

    @Override
    public CeFactorCacheRecordVo queryById(Long id) {
        return factorCacheRecordMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeFactorCacheRecordBo bo) {
        validateCacheVersion(bo.getCacheVersionId());
        CeFactorCacheRecord add = toEntity(bo);
        boolean flag = factorCacheRecordMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeFactorCacheRecordBo bo) {
        validateCacheVersion(bo.getCacheVersionId());
        CeFactorCacheRecord update = toEntity(bo);
        return factorCacheRecordMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return factorCacheRecordMapper.deleteByIds(ids) > 0;
    }

    protected CeFactorCacheRecord toEntity(CeFactorCacheRecordBo bo) {
        return MapstructUtils.convert(bo, CeFactorCacheRecord.class);
    }

    private LambdaQueryWrapper<CeFactorCacheRecord> buildQueryWrapper(CeFactorCacheRecordBo bo) {
        return new LambdaQueryWrapper<CeFactorCacheRecord>()
            .eq(bo.getCacheVersionId() != null, CeFactorCacheRecord::getCacheVersionId, bo.getCacheVersionId())
            .eq(StringUtils.isNotBlank(bo.getFactorCode()), CeFactorCacheRecord::getFactorCode, bo.getFactorCode())
            .like(StringUtils.isNotBlank(bo.getFactorName()), CeFactorCacheRecord::getFactorName, bo.getFactorName())
            .eq(StringUtils.isNotBlank(bo.getFactorCategory()), CeFactorCacheRecord::getFactorCategory, bo.getFactorCategory())
            .eq(bo.getEnabledFlag() != null, CeFactorCacheRecord::getEnabledFlag, bo.getEnabledFlag());
    }

    private void validateCacheVersion(Long cacheVersionId) {
        if (cacheVersionId == null || factorCacheVersionMapper.selectById(cacheVersionId) == null) {
            throw new ServiceException("factor cache version does not exist");
        }
    }
}
