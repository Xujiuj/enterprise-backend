package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.domain.bo.CeGreenPowerCertificateBo;
import org.dromara.carbon.enterprise.domain.vo.CeGreenPowerCertificateVo;
import org.dromara.carbon.enterprise.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.service.ICeGreenPowerCertificateService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Enterprise local green electricity and certificate proof service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeGreenPowerCertificateServiceImpl implements ICeGreenPowerCertificateService {

    private final CeGreenPowerCertificateMapper greenPowerCertificateMapper;

    @Override
    public TableDataInfo<CeGreenPowerCertificateVo> queryPageList(CeGreenPowerCertificateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeGreenPowerCertificate> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeGreenPowerCertificate::getCreateTime)
            .orderByDesc(CeGreenPowerCertificate::getId);
        IPage<CeGreenPowerCertificateVo> page = greenPowerCertificateMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeGreenPowerCertificateVo> queryList(CeGreenPowerCertificateBo bo) {
        return greenPowerCertificateMapper.selectVoList(buildQueryWrapper(bo)
            .orderByAsc(CeGreenPowerCertificate::getEnergyPeriod)
            .orderByAsc(CeGreenPowerCertificate::getCertificateCode));
    }

    @Override
    public CeGreenPowerCertificateVo queryById(Long id) {
        return greenPowerCertificateMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeGreenPowerCertificateBo bo) {
        CeGreenPowerCertificate add = MapstructUtils.convert(bo, CeGreenPowerCertificate.class);
        if (add.getEnergyUnit() == null) {
            add.setEnergyUnit("MWh");
        }
        if (add.getProofStatus() == null) {
            add.setProofStatus("draft");
        }
        boolean flag = greenPowerCertificateMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeGreenPowerCertificateBo bo) {
        CeGreenPowerCertificate update = MapstructUtils.convert(bo, CeGreenPowerCertificate.class);
        return greenPowerCertificateMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return greenPowerCertificateMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<CeGreenPowerCertificate> buildQueryWrapper(CeGreenPowerCertificateBo bo) {
        return new LambdaQueryWrapper<CeGreenPowerCertificate>()
            .like(StringUtils.isNotBlank(bo.getCertificateCode()), CeGreenPowerCertificate::getCertificateCode, bo.getCertificateCode())
            .eq(StringUtils.isNotBlank(bo.getCertificateType()), CeGreenPowerCertificate::getCertificateType, bo.getCertificateType())
            .eq(StringUtils.isNotBlank(bo.getEnergyPeriod()), CeGreenPowerCertificate::getEnergyPeriod, bo.getEnergyPeriod())
            .like(StringUtils.isNotBlank(bo.getIssuingOrg()), CeGreenPowerCertificate::getIssuingOrg, bo.getIssuingOrg())
            .eq(StringUtils.isNotBlank(bo.getOffsetSourceCode()), CeGreenPowerCertificate::getOffsetSourceCode, bo.getOffsetSourceCode())
            .eq(StringUtils.isNotBlank(bo.getProofStatus()), CeGreenPowerCertificate::getProofStatus, bo.getProofStatus());
    }
}
