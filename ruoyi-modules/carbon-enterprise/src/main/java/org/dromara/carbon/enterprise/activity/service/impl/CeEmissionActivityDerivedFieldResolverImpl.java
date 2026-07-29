package org.dromara.carbon.enterprise.activity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.activity.domain.CeEmissionActivityResolvedRow;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;
import org.dromara.carbon.enterprise.dimension.mapper.CeDimensionProjectionMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.shared.service.ICeEmissionActivityDerivedFieldResolver;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Enterprise-local derived field resolver for emission_activity activity data.
 * Resolves organisation ownership from 104 and factor fields from 201.
 */
@RequiredArgsConstructor
@Service
public class CeEmissionActivityDerivedFieldResolverImpl implements ICeEmissionActivityDerivedFieldResolver {

    private final CeEmissionSourceMapper emissionSourceMapper;
    private final CeDimensionProjectionMapper dimensionProjectionMapper;

    @Override
    public Optional<CeEmissionActivityResolvedRow> resolve(String emissionSourceCode) {
        if (StringUtils.isBlank(emissionSourceCode)) {
            return Optional.empty();
        }

        CeEmissionSource source = emissionSourceMapper.selectList(
            Wrappers.<CeEmissionSource>lambdaQuery()
                .eq(CeEmissionSource::getSourceIdentificationCode, emissionSourceCode)
                .eq(CeEmissionSource::getEnabledFlag, Boolean.TRUE)
                .orderByAsc(CeEmissionSource::getId)
        ).stream().findFirst().orElse(null);

        if (source == null) {
            return Optional.empty();
        }

        return Optional.of(toResolvedRow(source));
    }

    @Override
    public List<CeEmissionActivityResolvedRow> resolveByEntryFields(String companyName, String factoryName, String scope,
                                                            String scopeSubcategory, String sourceIdentificationName,
                                                            String emissionSourceName) {
        if (StringUtils.isBlank(companyName) || StringUtils.isBlank(factoryName) || StringUtils.isBlank(scope)
            || StringUtils.isBlank(scopeSubcategory) || StringUtils.isBlank(emissionSourceName)) {
            return List.of();
        }

        var wrapper = Wrappers.<CeEmissionSource>lambdaQuery()
            .eq(CeEmissionSource::getCompanyName, companyName.trim())
            .eq(CeEmissionSource::getFactoryName, factoryName.trim())
            .eq(CeEmissionSource::getScopeName, scope.trim())
            .eq(CeEmissionSource::getScopeSubcategory, scopeSubcategory.trim())
            .eq(CeEmissionSource::getEnabledFlag, Boolean.TRUE)
            .orderByAsc(CeEmissionSource::getSourceIdentificationCode);
        return emissionSourceMapper.selectList(wrapper)
            .stream()
            .map(this::toResolvedRow)
            .filter(row -> emissionSourceName.trim().equals(normalize(row.getEmissionSourceName())))
            .filter(row -> StringUtils.isBlank(sourceIdentificationName)
                || sourceIdentificationName.trim().equals(normalize(row.getEmissionSourceIdentity())))
            .toList();
    }

    private CeEmissionActivityResolvedRow toResolvedRow(CeEmissionSource source) {
        CeEmissionActivityResolvedRow row = new CeEmissionActivityResolvedRow();
        row.setEmissionSourceCode(source.getSourceIdentificationCode());
        row.setCompanyCode(source.getCompanyCode());
        row.setCompanyName(source.getCompanyName());
        row.setFactoryCode(source.getFactoryCode());
        row.setFactoryName(source.getFactoryName());
        row.setEmissionSourceCategoryCode(source.getSourceCategoryKey());
        row.setScope(source.getScopeName());
        row.setScopeSubcategory(source.getScopeSubcategory());
        CeDimensionRecordVo factor = resolveEfFactor(source);
        row.setEmissionSourceIdentity(factor == null ? source.getSourceIdentificationName() : normalize(factor.getRecordName()));
        row.setEmissionSourceName(factor == null ? source.getEmissionSourceName()
            : StringUtils.defaultIfBlank(normalize(factor.getFuelMaterialCategory()), normalize(factor.getRecordName())));
        row.setUnit(resolveActivityUnit(source, factor));
        row.setEmissionFactorCode(source.getFactorKey());
        row.setResponsibleDept(source.getResponsibleDept());
        row.setDataSource(source.getDataSource());
        return row;
    }

    private String resolveActivityUnit(CeEmissionSource source, CeDimensionRecordVo factor) {
        if (factor != null) {
            String factorSourceUnit = normalize(factor.getSourceUnit());
            if (StringUtils.isNotBlank(factorSourceUnit)) {
                return factorSourceUnit;
            }
            String factorUnit = normalize(factor.getFactorUnit());
            if (StringUtils.isNotBlank(factorUnit)) {
                return factorUnit;
            }
        }
        String sourceUnit = normalize(source.getSourceUnit());
        if (StringUtils.isNotBlank(sourceUnit)) {
            return sourceUnit;
        }
        return sourceUnit;
    }

    private CeDimensionRecordVo resolveEfFactor(CeEmissionSource source) {
        for (CeDimensionRecordVo factor : dimensionProjectionMapper.selectByDimensionCode("ef-factor")) {
            if (matchesFactor(source, factor)) {
                return factor;
            }
        }
        return null;
    }

    private boolean matchesFactor(CeEmissionSource source, CeDimensionRecordVo factor) {
        String factorKey = normalize(source.getFactorKey());
        return StringUtils.isNotBlank(factorKey) && factorKey.equals(normalize(factor.getRecordCode()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
