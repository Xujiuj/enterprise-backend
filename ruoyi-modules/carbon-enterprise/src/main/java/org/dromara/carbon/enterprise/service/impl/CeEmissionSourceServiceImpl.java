package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.domain.bo.CeEmissionSourceBo;
import org.dromara.carbon.enterprise.domain.vo.CeEmissionSourceVo;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.service.ICeEmissionSourceService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local emission source service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeEmissionSourceServiceImpl implements ICeEmissionSourceService {

    private final CeEmissionSourceMapper emissionSourceMapper;

    @Override
    public TableDataInfo<CeEmissionSourceVo> queryPageList(CeEmissionSourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeEmissionSource> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeEmissionSource::getCreateTime)
            .orderByDesc(CeEmissionSource::getId);
        IPage<CeEmissionSourceVo> page = emissionSourceMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeEmissionSourceVo> queryList(CeEmissionSourceBo bo) {
        return emissionSourceMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeEmissionSource::getSourceCode)
            .orderByAsc(CeEmissionSource::getId));
    }

    @Override
    public CeEmissionSourceVo queryById(Long id) {
        return emissionSourceMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeEmissionSourceBo bo) {
        CeEmissionSource add = MapstructUtils.convert(bo, CeEmissionSource.class);
        if (add.getBoundaryScope() == null) {
            add.setBoundaryScope("enterprise_local");
        }
        if (add.getEnabledFlag() == null) {
            add.setEnabledFlag(Boolean.TRUE);
        }
        boolean flag = emissionSourceMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeEmissionSourceBo bo) {
        CeEmissionSource update = MapstructUtils.convert(bo, CeEmissionSource.class);
        return emissionSourceMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return emissionSourceMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeEmissionSource> buildQueryWrapper(CeEmissionSourceBo bo) {
        return new LambdaQueryWrapper<CeEmissionSource>()
            .like(StringUtils.isNotBlank(bo.getSourceCode()), CeEmissionSource::getSourceCode, bo.getSourceCode())
            .like(StringUtils.isNotBlank(bo.getSourceName()), CeEmissionSource::getSourceName, bo.getSourceName())
            .eq(StringUtils.isNotBlank(bo.getSourceCategoryCode()), CeEmissionSource::getSourceCategoryCode, bo.getSourceCategoryCode())
            .like(StringUtils.isNotBlank(bo.getSourceCategoryName()), CeEmissionSource::getSourceCategoryName, bo.getSourceCategoryName())
            .like(StringUtils.isNotBlank(bo.getFacilityName()), CeEmissionSource::getFacilityName, bo.getFacilityName())
            .eq(StringUtils.isNotBlank(bo.getBoundaryScope()), CeEmissionSource::getBoundaryScope, bo.getBoundaryScope())
            .eq(bo.getEnabledFlag() != null, CeEmissionSource::getEnabledFlag, bo.getEnabledFlag());
    }
}
