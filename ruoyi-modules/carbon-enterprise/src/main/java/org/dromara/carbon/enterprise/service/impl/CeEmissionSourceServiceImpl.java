package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeCompanyFactory;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.domain.CeEmissionSourceCategory;
import org.dromara.carbon.enterprise.domain.bo.CeEmissionSourceBo;
import org.dromara.carbon.enterprise.domain.vo.CeEmissionSourceVo;
import org.dromara.carbon.enterprise.mapper.CeCompanyFactoryMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceCategoryMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.service.ICeEmissionSourceService;
import org.dromara.common.core.exception.ServiceException;
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
    private final CeCompanyFactoryMapper companyFactoryMapper;
    private final CeEmissionSourceCategoryMapper emissionSourceCategoryMapper;

    @Override
    public TableDataInfo<CeEmissionSourceVo> queryPageList(CeEmissionSourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeEmissionSource> wrapper = buildQueryWrapper(bo)
            .orderByAsc(CeEmissionSource::getCompanyCode)
            .orderByAsc(CeEmissionSource::getSourceIdentificationCode)
            .orderByAsc(CeEmissionSource::getId);
        IPage<CeEmissionSourceVo> page = emissionSourceMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeEmissionSourceVo> queryList(CeEmissionSourceBo bo) {
        return emissionSourceMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeEmissionSource::getSourceIdentificationCode)
            .orderByAsc(CeEmissionSource::getId));
    }

    @Override
    public CeEmissionSourceVo queryById(Long id) {
        return emissionSourceMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeEmissionSourceBo bo) {
        validateForeignKeys(bo);
        CeEmissionSource add = MapstructUtils.convert(bo, CeEmissionSource.class);
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
        validateForeignKeys(bo);
        CeEmissionSource update = MapstructUtils.convert(bo, CeEmissionSource.class);
        return emissionSourceMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return emissionSourceMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeEmissionSource> buildQueryWrapper(CeEmissionSourceBo bo) {
        return new LambdaQueryWrapper<CeEmissionSource>()
            .like(StringUtils.isNotBlank(bo.getCompanyCode()), CeEmissionSource::getCompanyCode, bo.getCompanyCode())
            .like(StringUtils.isNotBlank(bo.getCompanyName()), CeEmissionSource::getCompanyName, bo.getCompanyName())
            .like(StringUtils.isNotBlank(bo.getFactoryName()), CeEmissionSource::getFactoryName, bo.getFactoryName())
            .eq(StringUtils.isNotBlank(bo.getSourceCategoryKey()), CeEmissionSource::getSourceCategoryKey, bo.getSourceCategoryKey())
            .like(StringUtils.isNotBlank(bo.getScopeName()), CeEmissionSource::getScopeName, bo.getScopeName())
            .like(StringUtils.isNotBlank(bo.getScopeSubcategory()), CeEmissionSource::getScopeSubcategory, bo.getScopeSubcategory())
            .like(StringUtils.isNotBlank(bo.getSourceIdentificationCode()), CeEmissionSource::getSourceIdentificationCode, bo.getSourceIdentificationCode())
            .like(StringUtils.isNotBlank(bo.getSourceIdentificationName()), CeEmissionSource::getSourceIdentificationName, bo.getSourceIdentificationName())
            .like(StringUtils.isNotBlank(bo.getEmissionSourceName()), CeEmissionSource::getEmissionSourceName, bo.getEmissionSourceName())
            .like(StringUtils.isNotBlank(bo.getResponsibleDept()), CeEmissionSource::getResponsibleDept, bo.getResponsibleDept())
            .like(StringUtils.isNotBlank(bo.getDataSource()), CeEmissionSource::getDataSource, bo.getDataSource())
            .eq(StringUtils.isNotBlank(bo.getFactorKey()), CeEmissionSource::getFactorKey, bo.getFactorKey())
            .eq(bo.getEnabledFlag() != null, CeEmissionSource::getEnabledFlag, bo.getEnabledFlag());
    }

    private void validateForeignKeys(CeEmissionSourceBo bo) {
        if (StringUtils.isNotBlank(bo.getCompanyCode())) {
            Long count = companyFactoryMapper.selectCount(
                new LambdaQueryWrapper<CeCompanyFactory>()
                    .eq(CeCompanyFactory::getFactoryCode, bo.getCompanyCode()));
            if (count == null || count == 0) {
                throw new ServiceException("公司编号不存在：" + bo.getCompanyCode());
            }
        }
        if (StringUtils.isNotBlank(bo.getSourceCategoryKey())) {
            Long count = emissionSourceCategoryMapper.selectCount(
                new LambdaQueryWrapper<CeEmissionSourceCategory>()
                    .eq(CeEmissionSourceCategory::getCategorySk, bo.getSourceCategoryKey()));
            if (count == null || count == 0) {
                throw new ServiceException("排放源分类不存在：" + bo.getSourceCategoryKey());
            }
        }
    }
}
