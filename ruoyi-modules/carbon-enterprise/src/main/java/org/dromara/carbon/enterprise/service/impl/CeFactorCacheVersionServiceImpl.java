package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeFactorCacheVersion;
import org.dromara.carbon.enterprise.domain.bo.CeFactorCacheVersionBo;
import org.dromara.carbon.enterprise.domain.vo.CeFactorCacheVersionVo;
import org.dromara.carbon.enterprise.mapper.CeFactorCacheVersionMapper;
import org.dromara.carbon.enterprise.service.ICeFactorCacheVersionService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local factor cache version service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeFactorCacheVersionServiceImpl implements ICeFactorCacheVersionService {

    private final CeFactorCacheVersionMapper factorCacheVersionMapper;

    @Override
    public TableDataInfo<CeFactorCacheVersionVo> queryPageList(CeFactorCacheVersionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeFactorCacheVersion> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeFactorCacheVersion::getSyncedTime)
            .orderByDesc(CeFactorCacheVersion::getId);
        IPage<CeFactorCacheVersionVo> page = factorCacheVersionMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeFactorCacheVersionVo> queryList(CeFactorCacheVersionBo bo) {
        return factorCacheVersionMapper.selectVoList(buildQueryWrapper(bo)
            .orderByDesc(CeFactorCacheVersion::getSyncedTime)
            .orderByDesc(CeFactorCacheVersion::getId));
    }

    @Override
    public CeFactorCacheVersionVo queryById(Long id) {
        return factorCacheVersionMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeFactorCacheVersionBo bo) {
        CeFactorCacheVersion add = MapstructUtils.convert(bo, CeFactorCacheVersion.class);
        boolean flag = factorCacheVersionMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeFactorCacheVersionBo bo) {
        CeFactorCacheVersion update = MapstructUtils.convert(bo, CeFactorCacheVersion.class);
        return factorCacheVersionMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return factorCacheVersionMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeFactorCacheVersion> buildQueryWrapper(CeFactorCacheVersionBo bo) {
        return new LambdaQueryWrapper<CeFactorCacheVersion>()
            .eq(StringUtils.isNotBlank(bo.getVendorVersionId()), CeFactorCacheVersion::getVendorVersionId, bo.getVendorVersionId())
            .eq(StringUtils.isNotBlank(bo.getLicenseId()), CeFactorCacheVersion::getLicenseId, bo.getLicenseId())
            .eq(StringUtils.isNotBlank(bo.getVersionCode()), CeFactorCacheVersion::getVersionCode, bo.getVersionCode())
            .eq(bo.getFrozenFlag() != null, CeFactorCacheVersion::getFrozenFlag, bo.getFrozenFlag());
    }
}
