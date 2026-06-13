package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeIntensityMetric;
import org.dromara.carbon.enterprise.domain.bo.CeIntensityMetricBo;
import org.dromara.carbon.enterprise.domain.vo.CeIntensityMetricVo;
import org.dromara.carbon.enterprise.mapper.CeIntensityMetricMapper;
import org.dromara.carbon.enterprise.service.ICeIntensityMetricService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

/**
 * Enterprise local carbon intensity metric service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeIntensityMetricServiceImpl implements ICeIntensityMetricService {

    private final CeIntensityMetricMapper intensityMetricMapper;

    @Override
    public TableDataInfo<CeIntensityMetricVo> queryPageList(CeIntensityMetricBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeIntensityMetric> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeIntensityMetric::getCreateTime)
            .orderByDesc(CeIntensityMetric::getId);
        IPage<CeIntensityMetricVo> page = intensityMetricMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeIntensityMetricVo> queryList(CeIntensityMetricBo bo) {
        return intensityMetricMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeIntensityMetric::getMetricPeriod)
            .orderByAsc(CeIntensityMetric::getMetricCode));
    }

    @Override
    public CeIntensityMetricVo queryById(Long id) {
        return intensityMetricMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeIntensityMetricBo bo) {
        CeIntensityMetric add = MapstructUtils.convert(bo, CeIntensityMetric.class);
        applyDefaults(add);
        boolean flag = intensityMetricMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeIntensityMetricBo bo) {
        CeIntensityMetric update = MapstructUtils.convert(bo, CeIntensityMetric.class);
        applyDefaults(update);
        return intensityMetricMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return intensityMetricMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeIntensityMetric> buildQueryWrapper(CeIntensityMetricBo bo) {
        return new LambdaQueryWrapper<CeIntensityMetric>()
            .like(StringUtils.isNotBlank(bo.getMetricCode()), CeIntensityMetric::getMetricCode, bo.getMetricCode())
            .like(StringUtils.isNotBlank(bo.getMetricName()), CeIntensityMetric::getMetricName, bo.getMetricName())
            .eq(StringUtils.isNotBlank(bo.getMetricPeriod()), CeIntensityMetric::getMetricPeriod, bo.getMetricPeriod())
            .eq(StringUtils.isNotBlank(bo.getMetricStatus()), CeIntensityMetric::getMetricStatus, bo.getMetricStatus());
    }

    private void applyDefaults(CeIntensityMetric metric) {
        if (metric.getMetricStatus() == null) {
            metric.setMetricStatus("draft");
        }
        if (metric.getNumeratorEmission() == null) {
            metric.setNumeratorEmission(BigDecimal.ZERO);
        }
        if (metric.getDenominatorValue() == null) {
            metric.setDenominatorValue(BigDecimal.ZERO);
        }
        if (metric.getIntensityValue() == null && metric.getDenominatorValue().compareTo(BigDecimal.ZERO) > 0) {
            metric.setIntensityValue(metric.getNumeratorEmission().divide(metric.getDenominatorValue(), 10, RoundingMode.HALF_UP));
        }
    }
}
