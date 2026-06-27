package org.dromara.carbon.enterprise.activity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.activity.domain.CeSheet656ResolvedRow;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.shared.service.ICeSheet656DerivedFieldResolver;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Enterprise-local derived field resolver for sheet_656 activity data.
 * Resolves derived fields from ce_emission_source table.
 */
@RequiredArgsConstructor
@Service
public class CeSheet656DerivedFieldResolverImpl implements ICeSheet656DerivedFieldResolver {

    private final CeEmissionSourceMapper emissionSourceMapper;

    @Override
    public Optional<CeSheet656ResolvedRow> resolve(String emissionSourceCode) {
        if (StringUtils.isBlank(emissionSourceCode)) {
            return Optional.empty();
        }

        CeEmissionSource source = emissionSourceMapper.selectOne(
            Wrappers.<CeEmissionSource>lambdaQuery()
                .eq(CeEmissionSource::getSourceIdentificationCode, emissionSourceCode)
                .eq(CeEmissionSource::getEnabledFlag, Boolean.TRUE)
                .last("LIMIT 1"),
            false
        );

        if (source == null) {
            return Optional.empty();
        }

        CeSheet656ResolvedRow row = new CeSheet656ResolvedRow();
        row.setEmissionSourceCode(emissionSourceCode);
        row.setCompanyCode(source.getCompanyCode());
        row.setCompanyName(source.getCompanyName());
        row.setFactoryName(source.getFactoryName());
        row.setEmissionSourceCategoryCode(source.getSourceCategoryKey());
        row.setScope(source.getScopeName());
        row.setScopeSubcategory(source.getScopeSubcategory());
        row.setEmissionSourceIdentity(source.getSourceIdentificationName());
        row.setEmissionSourceName(source.getEmissionSourceName());
        row.setUnit(source.getSourceUnit());
        row.setEmissionFactorCode(source.getFactorKey());
        return Optional.of(row);
    }
}
