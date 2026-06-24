package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeFactorCacheRecord;
import org.dromara.carbon.enterprise.domain.CeFactorConfirmation;
import org.dromara.carbon.enterprise.domain.bo.CeFactorConfirmationBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorConfirmationVo;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheRecordMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorConfirmationMapper;
import org.dromara.carbon.enterprise.service.ICeFactorConfirmationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local emission factor confirmation service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeFactorConfirmationServiceImpl implements ICeFactorConfirmationService {

    private final CeFactorConfirmationMapper factorConfirmationMapper;
    private final CeFactorCacheRecordMapper factorCacheRecordMapper;

    @Override
    public TableDataInfo<CeFactorConfirmationVo> queryPageList(CeFactorConfirmationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeFactorConfirmation> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeFactorConfirmation::getCreateTime)
            .orderByDesc(CeFactorConfirmation::getId);
        IPage<CeFactorConfirmationVo> page = factorConfirmationMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeFactorConfirmationVo> queryList(CeFactorConfirmationBo bo) {
        return factorConfirmationMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeFactorConfirmation::getFactorCode)
            .orderByAsc(CeFactorConfirmation::getId));
    }

    @Override
    public CeFactorConfirmationVo queryById(Long id) {
        return factorConfirmationMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeFactorConfirmationBo bo) {
        validateFactorExists(bo.getFactorCode());
        CeFactorConfirmation add = MapstructUtils.convert(bo, CeFactorConfirmation.class);
        if (add.getConfirmationStatus() == null) {
            add.setConfirmationStatus("pending");
        }
        boolean flag = factorConfirmationMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeFactorConfirmationBo bo) {
        CeFactorConfirmation update = MapstructUtils.convert(bo, CeFactorConfirmation.class);
        return factorConfirmationMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return factorConfirmationMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeFactorConfirmation> buildQueryWrapper(CeFactorConfirmationBo bo) {
        return new LambdaQueryWrapper<CeFactorConfirmation>()
            .like(StringUtils.isNotBlank(bo.getFactorCode()), CeFactorConfirmation::getFactorCode, bo.getFactorCode())
            .like(StringUtils.isNotBlank(bo.getFactorName()), CeFactorConfirmation::getFactorName, bo.getFactorName())
            .eq(StringUtils.isNotBlank(bo.getFactorVersionCode()), CeFactorConfirmation::getFactorVersionCode, bo.getFactorVersionCode())
            .eq(StringUtils.isNotBlank(bo.getConfirmationStatus()), CeFactorConfirmation::getConfirmationStatus, bo.getConfirmationStatus())
            .eq(StringUtils.isNotBlank(bo.getLicenseId()), CeFactorConfirmation::getLicenseId, bo.getLicenseId());
    }

    private void validateFactorExists(String factorCode) {
        if (StringUtils.isBlank(factorCode)) {
            return;
        }
        Long count = factorCacheRecordMapper.selectCount(
            new LambdaQueryWrapper<CeFactorCacheRecord>()
                .eq(CeFactorCacheRecord::getFactorCode, factorCode));
        if (count == null || count == 0) {
            throw new ServiceException("因子编码不存在于因子缓存中：" + factorCode);
        }
    }
}
