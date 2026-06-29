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
 * Resolves derived fields from ce_emission_source table.
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
            .eq(CeEmissionSource::getEmissionSourceName, emissionSourceName.trim())
            .eq(CeEmissionSource::getEnabledFlag, Boolean.TRUE)
            .orderByAsc(CeEmissionSource::getSourceIdentificationCode);
        if (StringUtils.isNotBlank(sourceIdentificationName)) {
            wrapper.eq(CeEmissionSource::getSourceIdentificationName, sourceIdentificationName.trim());
        }

        return emissionSourceMapper.selectList(wrapper)
            .stream()
            .map(this::toResolvedRow)
            .toList();
    }

    private CeEmissionActivityResolvedRow toResolvedRow(CeEmissionSource source) {
        CeEmissionActivityResolvedRow row = new CeEmissionActivityResolvedRow();
        row.setEmissionSourceCode(source.getSourceIdentificationCode());
        row.setCompanyCode(source.getCompanyCode());
        row.setCompanyName(source.getCompanyName());
        row.setFactoryName(source.getFactoryName());
        row.setEmissionSourceCategoryCode(source.getSourceCategoryKey());
        row.setScope(source.getScopeName());
        row.setScopeSubcategory(source.getScopeSubcategory());
        row.setEmissionSourceIdentity(source.getSourceIdentificationName());
        row.setEmissionSourceName(source.getEmissionSourceName());
        row.setUnit(resolveActivityUnit(source));
        row.setEmissionFactorCode(source.getFactorKey());
        return row;
    }

    private String resolveActivityUnit(CeEmissionSource source) {
        String sourceUnit = normalize(source.getSourceUnit());
        if (StringUtils.isNotBlank(sourceUnit)) {
            return sourceUnit;
        }
        for (CeDimensionRecordVo factor : dimensionProjectionMapper.selectByDimensionCode("ef-factor")) {
            if (matchesFactor(source, factor)) {
                String factorSourceUnit = normalize(factor.getSourceUnit());
                if (StringUtils.isNotBlank(factorSourceUnit)) {
                    return factorSourceUnit;
                }
                String factorUnit = normalize(factor.getFactorUnit());
                if (StringUtils.isNotBlank(factorUnit)) {
                    return factorUnit;
                }
            }
        }
        return sourceUnit;
    }

    private boolean matchesFactor(CeEmissionSource source, CeDimensionRecordVo factor) {
        String factorKey = normalize(source.getFactorKey());
        if (StringUtils.isNotBlank(factorKey) && factorKey.equals(normalize(factor.getRecordCode()))) {
            return true;
        }
        String sourceName = normalize(source.getEmissionSourceName());
        return StringUtils.isNotBlank(sourceName) && sourceName.equals(normalize(factor.getRecordName()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
