package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.bo.CeLicenseStateBo;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local license runtime state service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeLicenseStateServiceImpl implements ICeLicenseStateService {

    private final CeLicenseStateMapper licenseStateMapper;

    @Override
    public TableDataInfo<CeLicenseStateVo> queryPageList(CeLicenseStateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeLicenseState> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeLicenseState::getValidTo)
            .orderByDesc(CeLicenseState::getId);
        IPage<CeLicenseStateVo> page = licenseStateMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeLicenseStateVo> queryList(CeLicenseStateBo bo) {
        return licenseStateMapper.selectVoList(buildQueryWrapper(bo)
            .orderByDesc(CeLicenseState::getValidTo)
            .orderByDesc(CeLicenseState::getId));
    }

    @Override
    public CeLicenseStateVo queryById(Long id) {
        return licenseStateMapper.selectVoById(id);
    }

    @Override
    public CeLicenseStateVo queryCurrent() {
        IPage<CeLicenseStateVo> page = licenseStateMapper.selectVoPage(new Page<>(1, 1, false),
            new LambdaQueryWrapper<CeLicenseState>()
            .orderByDesc(CeLicenseState::getLastVerifiedTime)
            .orderByDesc(CeLicenseState::getId));
        return page.getRecords().stream().findFirst().orElse(null);
    }

    @Override
    public Boolean insertByBo(CeLicenseStateBo bo) {
        CeLicenseState add = MapstructUtils.convert(bo, CeLicenseState.class);
        boolean flag = licenseStateMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeLicenseStateBo bo) {
        CeLicenseState update = MapstructUtils.convert(bo, CeLicenseState.class);
        return licenseStateMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return licenseStateMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeLicenseState> buildQueryWrapper(CeLicenseStateBo bo) {
        return new LambdaQueryWrapper<CeLicenseState>()
            .eq(StringUtils.isNotBlank(bo.getLicenseId()), CeLicenseState::getLicenseId, bo.getLicenseId())
            .eq(StringUtils.isNotBlank(bo.getCustomerId()), CeLicenseState::getCustomerId, bo.getCustomerId())
            .eq(StringUtils.isNotBlank(bo.getInstallId()), CeLicenseState::getInstallId, bo.getInstallId())
            .eq(StringUtils.isNotBlank(bo.getKeyId()), CeLicenseState::getKeyId, bo.getKeyId())
            .eq(StringUtils.isNotBlank(bo.getLicenseStatus()), CeLicenseState::getLicenseStatus, bo.getLicenseStatus());
    }
}
