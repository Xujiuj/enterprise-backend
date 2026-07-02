package org.dromara.carbon.enterprise.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.activity.domain.CeActivityData;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.activity.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.emission.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.greenpower.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.intensity.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.template.domain.CeTemplateField;
import org.dromara.carbon.enterprise.template.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.activity.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.activity.domain.vo.CeActivityDataVo;
import org.dromara.carbon.enterprise.activity.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.activity.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.emission.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.greenpower.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.intensity.mapper.CeIntensityDenominatorFactMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.template.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.shared.service.ICeActivityDataService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enterprise local activity data service implementation.
 */
@RequiredArgsConstructor
@Service
public class CeActivityDataServiceImpl implements ICeActivityDataService {

    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_LOCKED = "locked";
    private static final String STATUS_DRAFT = "draft";
    private static final String TARGET_TABLE_CODE = "emission_activity";
    private static final String FIELD_SOURCE_CODE = "sourceIdentificationCode";
    private static final String FIELD_YEAR = "activityYear";
    private static final String FIELD_MONTH = "activityMonth";
    private static final String FIELD_DEPARTMENT = "responsibleDept";
    private static final String MODULE_ACTIVITY = "活动数据";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final String FREQUENCY_MONTHLY = "monthly";
    private static final String FREQUENCY_DAILY = "daily";
    private static final String FREQUENCY_QUARTERLY = "quarterly";

    private final CeActivityDataMapper activityDataMapper;
    private final CeEmissionSourceMapper emissionSourceMapper;
    private final CeGreenPowerCertificateMapper greenPowerCertificateMapper;
    private final CeIntensityDenominatorFactMapper denominatorFactMapper;
    private final CeTemplateSheetMapper templateSheetMapper;
    private final CeTemplateFieldMapper templateFieldMapper;
    private final CeCaptureRowMapper captureRowMapper;
    private final CeCaptureCellMapper captureCellMapper;
    private final CeCaptureBatchMapper captureBatchMapper;

    @Override
    public TableDataInfo<CeActivityDataVo> queryPageList(CeActivityDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CeActivityData> wrapper = applyDefaultListOrder(buildQueryWrapper(bo));
        IPage<CeActivityDataVo> page = activityDataMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeActivityDataVo> queryList(CeActivityDataBo bo) {
        return activityDataMapper.selectVoList(applyDefaultListOrder(buildQueryWrapper(bo)));
    }

    private LambdaQueryWrapper<CeActivityData> applyDefaultListOrder(LambdaQueryWrapper<CeActivityData> wrapper) {
        return wrapper
            .orderByDesc(CeActivityData::getCreateTime)
            .orderByDesc(CeActivityData::getId);
    }

    @Override
    public CeActivityDataValidationDashboardVo queryValidationDashboard(CeActivityDataBo bo) {
        ActivityPeriod period = resolvePeriod(bo);
        String dueDate = resolveDueDate(period);
        List<CeEmissionSource> sources = listEnabledEmissionSources();
        Map<String, CeEmissionSource> sourceByCode = sources.stream()
            .filter(source -> StringUtils.isNotBlank(source.getSourceIdentificationCode()))
            .collect(Collectors.toMap(source -> source.getSourceIdentificationCode().trim(), Function.identity(), (left, right) -> left));
        List<CeActivityData> activities = listActivities(period).stream()
            .filter(activity -> isActivityExpectedInPeriod(activity, sourceByCode, period))
            .toList();
        List<CeGreenPowerCertificate> greenCertificates = listGreenPowerCertificates(period);
        List<CeIntensityDenominatorFact> denominatorFacts = listDenominatorFacts(period);
        Map<String, List<CeActivityData>> activitiesBySourceCode = activities.stream()
            .filter(activity -> StringUtils.isNotBlank(activity.getSourceIdentificationCode()))
            .collect(Collectors.groupingBy(activity -> activity.getSourceIdentificationCode().trim()));
        Map<String, CaptureSubmissionMeta> captureMetaBySourceCode = loadCaptureMetaBySourceCode(period.label());

        List<CeEmissionSource> expectedSources = sources.stream()
            .filter(source -> isSourceExpectedInPeriod(source, period))
            .collect(Collectors.toCollection(ArrayList::new));
        for (CeActivityData activity : activities) {
            String sourceCode = activity.getSourceIdentificationCode();
            if (StringUtils.isNotBlank(sourceCode) && !sourceByCode.containsKey(sourceCode.trim())) {
                CeEmissionSource source = new CeEmissionSource();
                source.setSourceIdentificationCode(sourceCode.trim());
                source.setSourceIdentificationName(activity.getSourceIdentificationName());
                source.setEmissionSourceName(activity.getEmissionSourceName());
                source.setFactoryName(activity.getFactoryName());
                expectedSources.add(source);
                sourceByCode.put(sourceCode.trim(), source);
            }
        }

        CeActivityDataValidationDashboardVo dashboard = new CeActivityDataValidationDashboardVo();
        dashboard.setActivityYear(period.year());
        dashboard.setActivityMonth(period.month());
        dashboard.setDueDate(dueDate);

        int submittedCount = 0;
        int draftCount = 0;
        int missingCount = 0;
        int accurateSubmittedCount = 0;
        int passedRecordCount = 0;

        List<CeActivityDataValidationDashboardVo.ValidationIssue> issues = new ArrayList<>();
        Map<String, SubmissionAggregate> submissionAggregates = new LinkedHashMap<>();

        for (CeEmissionSource source : expectedSources) {
            String sourceCode = source.getSourceIdentificationCode();
            CeActivityData activity = chooseLatestActivity(StringUtils.isBlank(sourceCode) ? null : activitiesBySourceCode.get(sourceCode.trim()));
            CaptureSubmissionMeta captureMeta = StringUtils.isBlank(sourceCode) ? null : captureMetaBySourceCode.get(sourceCode.trim());
            List<CeActivityDataValidationDashboardVo.ValidationIssue> sourceIssues = buildIssues(source, activity, period);
            issues.addAll(sourceIssues);

            String submissionStatus = resolveSubmissionStatus(activity);
            if (STATUS_SUBMITTED.equals(submissionStatus)) {
                submittedCount++;
            } else if (STATUS_DRAFT.equals(submissionStatus)) {
                draftCount++;
            } else {
                missingCount++;
            }

            BigDecimal accuracyRate = resolveSubmissionAccuracy(submissionStatus, sourceIssues);
            if (STATUS_SUBMITTED.equals(submissionStatus) && ONE_HUNDRED.compareTo(accuracyRate) == 0) {
                accurateSubmittedCount++;
            }
            if (sourceIssues.isEmpty()) {
                passedRecordCount++;
            }

            SubmissionAggregate aggregate = submissionAggregate(
                submissionAggregates,
                resolveDepartment(captureMeta),
                resolveResponsiblePerson(captureMeta),
                StringUtils.isBlank(source.getFactoryName()) ? "--" : source.getFactoryName(),
                period,
                dueDate
            );
            aggregate.addActivity(source, submissionStatus, resolveSubmittedTime(activity, captureMeta), sourceIssues);
        }

        List<CeActivityDataValidationDashboardVo.ValidationIssue> greenCertificateIssues = buildGreenCertificateIssues(greenCertificates, period);
        issues.addAll(greenCertificateIssues);
        passedRecordCount += Math.max(0, greenCertificates.size() - countInvalidGreenCertificateRecords(greenCertificates));

        List<CeActivityDataValidationDashboardVo.ValidationIssue> denominatorIssues = buildDenominatorIssues(denominatorFacts, period);
        issues.addAll(denominatorIssues);
        passedRecordCount += Math.max(0, denominatorFacts.size() - countInvalidDenominatorRecords(denominatorFacts));

        int validatedRecordCount = expectedSources.size() + greenCertificates.size() + denominatorFacts.size();
        int abnormalItems = (int) issues.stream().filter(issue -> "abnormal".equals(issue.getIssueStatus())).count();

        dashboard.setExpectedItems(expectedSources.size());
        dashboard.setValidatedRecordCount(validatedRecordCount);
        dashboard.setSubmittedItems(submittedCount);
        dashboard.setDraftItems(draftCount);
        dashboard.setMissingItems(missingCount);
        dashboard.setAbnormalItems(abnormalItems);
        dashboard.setAccuracyRate(resolveOverallAccuracy(submittedCount, accurateSubmittedCount));
        dashboard.setPassRate(resolvePassRate(validatedRecordCount, passedRecordCount));
        dashboard.setSubmissions(submissionAggregates.values().stream().map(SubmissionAggregate::toVo).toList());
        dashboard.setIssues(issues);
        return dashboard;
    }

    @Override
    public CeActivityDataVo queryById(Long id) {
        return activityDataMapper.selectVoById(id);
    }

    @Override
    public Boolean insertByBo(CeActivityDataBo bo) {
        CeActivityData add = MapstructUtils.convert(bo, CeActivityData.class);
        if (add.getDataStatus() == null) {
            add.setDataStatus(STATUS_DRAFT);
        }
        boolean flag = activityDataMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CeActivityDataBo bo) {
        CeActivityData update = MapstructUtils.convert(bo, CeActivityData.class);
        return activityDataMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return activityDataMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean updateStatusByIds(Collection<Long> ids, String dataStatus) {
        if (ids == null || ids.isEmpty() || StringUtils.isBlank(dataStatus)) {
            return false;
        }
        CeActivityData update = new CeActivityData();
        update.setDataStatus(dataStatus);
        return activityDataMapper.update(update, new LambdaQueryWrapper<CeActivityData>()
            .in(CeActivityData::getId, ids)) > 0;
    }

    private LambdaQueryWrapper<CeActivityData> buildQueryWrapper(CeActivityDataBo bo) {
        return new LambdaQueryWrapper<CeActivityData>()
            .eq(bo.getBatchId() != null, CeActivityData::getBatchId, bo.getBatchId())
            .eq(StringUtils.isNotBlank(bo.getSourceSheetCode()), CeActivityData::getSourceSheetCode, bo.getSourceSheetCode())
            .like(StringUtils.isNotBlank(bo.getSourceIdentificationCode()), CeActivityData::getSourceIdentificationCode, bo.getSourceIdentificationCode())
            .like(StringUtils.isNotBlank(bo.getCompanyCode()), CeActivityData::getCompanyCode, bo.getCompanyCode())
            .like(StringUtils.isNotBlank(bo.getCompanyName()), CeActivityData::getCompanyName, bo.getCompanyName())
            .like(StringUtils.isNotBlank(bo.getFactoryName()), CeActivityData::getFactoryName, bo.getFactoryName())
            .eq(StringUtils.isNotBlank(bo.getSourceCategoryKey()), CeActivityData::getSourceCategoryKey, bo.getSourceCategoryKey())
            .like(StringUtils.isNotBlank(bo.getScopeName()), CeActivityData::getScopeName, bo.getScopeName())
            .like(StringUtils.isNotBlank(bo.getScopeSubcategory()), CeActivityData::getScopeSubcategory, bo.getScopeSubcategory())
            .like(StringUtils.isNotBlank(bo.getSourceIdentificationName()), CeActivityData::getSourceIdentificationName, bo.getSourceIdentificationName())
            .like(StringUtils.isNotBlank(bo.getEmissionSourceName()), CeActivityData::getEmissionSourceName, bo.getEmissionSourceName())
            .eq(StringUtils.isNotBlank(bo.getActivityUnit()), CeActivityData::getActivityUnit, bo.getActivityUnit())
            .like(StringUtils.isNotBlank(bo.getResponsibleDept()), CeActivityData::getResponsibleDept, bo.getResponsibleDept())
            .eq(StringUtils.isNotBlank(bo.getDataSource()), CeActivityData::getDataSource, bo.getDataSource())
            .eq(bo.getActivityYear() != null, CeActivityData::getActivityYear, bo.getActivityYear())
            .eq(bo.getActivityMonth() != null, CeActivityData::getActivityMonth, bo.getActivityMonth())
            .eq(StringUtils.isNotBlank(bo.getDataStatus()), CeActivityData::getDataStatus, bo.getDataStatus());
    }

    private ActivityPeriod resolvePeriod(CeActivityDataBo bo) {
        if (bo != null && bo.getActivityYear() != null) {
            return new ActivityPeriod(bo.getActivityYear(), bo.getActivityMonth());
        }
        return activityDataMapper.selectList(new LambdaQueryWrapper<CeActivityData>()
                .isNotNull(CeActivityData::getActivityYear)
                .orderByDesc(CeActivityData::getActivityYear)
                .orderByDesc(CeActivityData::getActivityMonth)
                .orderByDesc(CeActivityData::getId))
            .stream()
            .findFirst()
            .map(activity -> new ActivityPeriod(activity.getActivityYear(), activity.getActivityMonth()))
            .orElseGet(() -> {
                YearMonth now = YearMonth.now();
                return new ActivityPeriod(now.getYear(), now.getMonthValue());
            });
    }

    private String resolveDueDate(ActivityPeriod period) {
        try {
            if (period.month() == null) {
                return LocalDate.of(period.year(), 12, 31).toString();
            }
            return YearMonth.of(period.year(), period.month()).plusMonths(1).atDay(5).toString();
        } catch (RuntimeException ex) {
            return LocalDate.now().toString();
        }
    }

    private List<CeEmissionSource> listEnabledEmissionSources() {
        return emissionSourceMapper.selectList(new LambdaQueryWrapper<CeEmissionSource>()
            .eq(CeEmissionSource::getEnabledFlag, true)
            .orderByAsc(CeEmissionSource::getSourceIdentificationCode)
            .orderByAsc(CeEmissionSource::getId));
    }

    private boolean isActivityExpectedInPeriod(CeActivityData activity, Map<String, CeEmissionSource> sourceByCode, ActivityPeriod period) {
        if (activity == null || StringUtils.isBlank(activity.getSourceIdentificationCode())) {
            return true;
        }
        CeEmissionSource source = sourceByCode.get(activity.getSourceIdentificationCode().trim());
        return source == null || isSourceExpectedInPeriod(source, period);
    }

    private boolean isSourceExpectedInPeriod(CeEmissionSource source, ActivityPeriod period) {
        String frequency = normalizedFrequency(source == null ? null : source.getDataFrequency());
        if (FREQUENCY_QUARTERLY.equals(frequency)) {
            return period.month() == null || isQuarterEndMonth(period.month());
        }
        return FREQUENCY_MONTHLY.equals(frequency) || FREQUENCY_DAILY.equals(frequency);
    }

    private boolean isQuarterEndMonth(Integer month) {
        return month != null && month >= 1 && month <= 12 && month % 3 == 0;
    }

    private String normalizedFrequency(String frequency) {
        if (StringUtils.isBlank(frequency)) {
            return FREQUENCY_MONTHLY;
        }
        String normalized = frequency.trim().toLowerCase(Locale.ROOT);
        if (FREQUENCY_DAILY.equals(normalized) || FREQUENCY_MONTHLY.equals(normalized) || FREQUENCY_QUARTERLY.equals(normalized)) {
            return normalized;
        }
        return FREQUENCY_MONTHLY;
    }

    private List<CeActivityData> listActivities(ActivityPeriod period) {
        return activityDataMapper.selectList(new LambdaQueryWrapper<CeActivityData>()
            .eq(CeActivityData::getActivityYear, period.year())
            .eq(period.month() != null, CeActivityData::getActivityMonth, period.month())
            .orderByDesc(CeActivityData::getUpdateTime)
            .orderByDesc(CeActivityData::getCreateTime)
            .orderByDesc(CeActivityData::getId));
    }

    private List<CeGreenPowerCertificate> listGreenPowerCertificates(ActivityPeriod period) {
        return greenPowerCertificateMapper.selectList(new LambdaQueryWrapper<CeGreenPowerCertificate>()
            .eq(CeGreenPowerCertificate::getActivityYear, period.year())
            .eq(period.month() != null, CeGreenPowerCertificate::getActivityMonth, period.month())
            .orderByDesc(CeGreenPowerCertificate::getUpdateTime)
            .orderByDesc(CeGreenPowerCertificate::getCreateTime)
            .orderByDesc(CeGreenPowerCertificate::getId));
    }

    private List<CeIntensityDenominatorFact> listDenominatorFacts(ActivityPeriod period) {
        return denominatorFactMapper.selectList(new LambdaQueryWrapper<CeIntensityDenominatorFact>()
            .eq(CeIntensityDenominatorFact::getFactYear, period.year())
            .eq(period.month() != null, CeIntensityDenominatorFact::getFactMonth, period.month())
            .orderByAsc(CeIntensityDenominatorFact::getFactoryCode)
            .orderByAsc(CeIntensityDenominatorFact::getId));
    }

    private CeActivityData chooseLatestActivity(List<CeActivityData> activities) {
        if (activities == null || activities.isEmpty()) {
            return null;
        }
        return activities.stream()
            .max(Comparator
                .comparing((CeActivityData activity) -> isSubmitted(activity) ? 1 : 0)
                .thenComparing(CeActivityData::getUpdateTime, Comparator.nullsFirst(Date::compareTo))
                .thenComparing(CeActivityData::getCreateTime, Comparator.nullsFirst(Date::compareTo))
                .thenComparing(CeActivityData::getId, Comparator.nullsFirst(Long::compareTo)))
            .orElse(activities.get(0));
    }

    private String resolveSubmissionStatus(CeActivityData activity) {
        if (activity == null) {
            return "missing";
        }
        if (isSubmitted(activity)) {
            return STATUS_SUBMITTED;
        }
        return STATUS_DRAFT;
    }

    private boolean isSubmitted(CeActivityData activity) {
        return activity != null && (STATUS_SUBMITTED.equals(activity.getDataStatus()) || STATUS_LOCKED.equals(activity.getDataStatus()));
    }

    private List<CeActivityDataValidationDashboardVo.ValidationIssue> buildIssues(CeEmissionSource source, CeActivityData activity,
                                                                                  ActivityPeriod period) {
        List<CeActivityDataValidationDashboardVo.ValidationIssue> issues = new ArrayList<>();
        if (activity == null) {
            issues.add(issue("MISSING_ACTIVITY_DATA", "活动数据缺失", "ERROR", source, period,
                "已启用的排放源在所选核算期间没有活动数据。", "请补充该期间的活动数据。", "missing"));
            return issues;
        }
        if (!isSubmitted(activity)) {
            issues.add(issue("UNSUBMITTED_ACTIVITY_DATA", "活动数据未提交", "WARNING", source, period,
                "活动数据仍处于草稿状态。", "请复核草稿数据并提交。", "pending"));
        }
        if (activity.getActivityValue() == null || BigDecimal.ZERO.compareTo(activity.getActivityValue()) >= 0) {
            issues.add(issue("INVALID_ACTIVITY_VALUE", "活动数据值无效", "ERROR", source, period,
                "活动数据值为空，或小于等于 0。", "请核对原始读数后重新保存。", "abnormal"));
        }
        if (StringUtils.isBlank(activity.getActivityUnit())) {
            issues.add(issue("MISSING_ACTIVITY_UNIT", "活动数据单位缺失", "ERROR", source, period,
                "活动数据缺少计量单位。", "请核对排放源单位后重新保存。", "abnormal"));
        }
        if (isSubmitted(activity) && StringUtils.isBlank(activity.getFactorKey())) {
            issues.add(issue("MISSING_FACTOR_CONFIRMATION", "排放因子未确认", "WARNING", source, period,
                "已提交的活动数据未关联已确认的排放因子。", "请确认排放因子并重新计算。", "pending"));
        }
        return issues;
    }

    private CeActivityDataValidationDashboardVo.ValidationIssue issue(String ruleCode, String ruleName, String severity,
                                                                      CeEmissionSource source, ActivityPeriod period, String description,
                                                                      String suggestion, String status) {
        CeActivityDataValidationDashboardVo.ValidationIssue issue = issueForObject(
            ruleCode,
            ruleName,
            severity,
            source.getSourceIdentificationCode() + " / " + sourceName(source),
            period,
            description,
            suggestion,
            status
        );
        issue.setSourceIdentificationCode(source.getSourceIdentificationCode());
        issue.setSourceIdentificationName(source.getSourceIdentificationName());
        return issue;
    }

    private List<CeActivityDataValidationDashboardVo.ValidationIssue> buildGreenCertificateIssues(
        List<CeGreenPowerCertificate> certificates, ActivityPeriod period) {
        List<CeActivityDataValidationDashboardVo.ValidationIssue> issues = new ArrayList<>();
        for (CeGreenPowerCertificate certificate : certificates) {
            String objectName = certificate.getCertificateCode() + " / " + certificate.getElectricityType();
            if (certificate.getQuantityKwh() == null || BigDecimal.ZERO.compareTo(certificate.getQuantityKwh()) >= 0) {
                issues.add(issueForObject("INVALID_GREEN_POWER_AMOUNT", "绿电绿证数量无效", "ERROR", objectName, period,
                    "绿电绿证数量为空，或小于等于 0。", "请核对证书数量后重新保存。", "abnormal"));
            }
            if ("voided".equals(certificate.getProofStatus())) {
                issues.add(issueForObject("VOIDED_GREEN_POWER_PROOF", "绿电绿证凭证已作废", "ERROR", objectName, period,
                    "绿电绿证凭证状态为已作废。", "请更换有效证书或移除对应抵扣。", "abnormal"));
            } else if (!"verified".equals(certificate.getProofStatus())) {
                issues.add(issueForObject("PENDING_GREEN_POWER_PROOF", "绿电绿证凭证待核验", "WARNING", objectName, period,
                    "绿电绿证凭证尚未完成核验。", "请完成证书核验。", "pending"));
            }
        }
        return issues;
    }

    private long countInvalidGreenCertificateRecords(List<CeGreenPowerCertificate> certificates) {
        return certificates.stream()
            .filter(certificate -> certificate.getQuantityKwh() == null
                || BigDecimal.ZERO.compareTo(certificate.getQuantityKwh()) >= 0
                || !"verified".equals(certificate.getProofStatus()))
            .count();
    }

    private List<CeActivityDataValidationDashboardVo.ValidationIssue> buildDenominatorIssues(List<CeIntensityDenominatorFact> facts,
                                                                                              ActivityPeriod period) {
        List<CeActivityDataValidationDashboardVo.ValidationIssue> issues = new ArrayList<>();
        for (CeIntensityDenominatorFact fact : facts) {
            String objectName = fact.getFactoryCode() + " / " + fact.getDenominatorMetricName();
            if (StringUtils.isBlank(fact.getDenominatorType())) {
                issues.add(issueForObject("MISSING_DENOMINATOR_CODE", "分母类型缺失", "WARNING", objectName, period,
                    "强度分母事实缺少分母类型。", "请补充分母类型。", "pending"));
            }
            if (fact.getDenominatorValue() == null || BigDecimal.ZERO.compareTo(fact.getDenominatorValue()) >= 0) {
                issues.add(issueForObject("INVALID_DENOMINATOR_VALUE", "分母数值无效", "ERROR", objectName, period,
                    "分母数值为空，或小于等于 0。", "请核对分母来源数据。", "abnormal"));
            }
        }
        return issues;
    }

    private long countInvalidDenominatorRecords(List<CeIntensityDenominatorFact> facts) {
        return facts.stream()
            .filter(fact -> StringUtils.isBlank(fact.getDenominatorType())
                || fact.getDenominatorValue() == null
                || BigDecimal.ZERO.compareTo(fact.getDenominatorValue()) >= 0)
            .count();
    }

    private CeActivityDataValidationDashboardVo.ValidationIssue issueForObject(String ruleCode, String ruleName, String severity,
                                                                               String objectName, ActivityPeriod period, String description,
                                                                               String suggestion, String status) {
        CeActivityDataValidationDashboardVo.ValidationIssue issue = new CeActivityDataValidationDashboardVo.ValidationIssue();
        issue.setRuleCode(ruleCode);
        issue.setRuleName(ruleName);
        issue.setSeverity(severity);
        issue.setObjectName(objectName);
        issue.setActivityYear(period.year());
        issue.setActivityMonth(period.month());
        issue.setDescription(description);
        issue.setSuggestion(suggestion);
        issue.setIssueStatus(status);
        return issue;
    }

    private BigDecimal resolveSubmissionAccuracy(String submissionStatus,
                                                 List<CeActivityDataValidationDashboardVo.ValidationIssue> issues) {
        if ("missing".equals(submissionStatus)) {
            return null;
        }
        long errorCount = issues.stream().filter(issue -> "ERROR".equals(issue.getSeverity())).count();
        long warningCount = issues.stream().filter(issue -> "WARNING".equals(issue.getSeverity())).count();
        return ONE_HUNDRED
            .subtract(BigDecimal.valueOf(errorCount * 40L))
            .subtract(BigDecimal.valueOf(warningCount * 10L))
            .max(BigDecimal.ZERO)
            .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveOverallAccuracy(int submittedCount, int accurateSubmittedCount) {
        if (submittedCount == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(accurateSubmittedCount)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(submittedCount), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal resolvePassRate(int validatedRecordCount, int passedRecordCount) {
        if (validatedRecordCount == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(passedRecordCount)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(validatedRecordCount), 1, RoundingMode.HALF_UP);
    }

    private Date resolveSubmittedTime(CeActivityData activity, CaptureSubmissionMeta captureMeta) {
        if (captureMeta != null && captureMeta.submittedTime() != null) {
            return captureMeta.submittedTime();
        }
        if (activity != null && activity.getUpdateTime() != null) {
            return activity.getUpdateTime();
        }
        return activity == null ? null : activity.getCreateTime();
    }

    private String resolveDepartment(CaptureSubmissionMeta captureMeta) {
        return captureMeta == null || StringUtils.isBlank(captureMeta.department()) ? "未分配部门" : captureMeta.department();
    }

    private String resolveResponsiblePerson(CaptureSubmissionMeta captureMeta) {
        return captureMeta == null || StringUtils.isBlank(captureMeta.submittedBy()) ? "--" : captureMeta.submittedBy();
    }

    private Map<String, CaptureSubmissionMeta> loadCaptureMetaBySourceCode(String period) {
        CeTemplateSheet sheet = resolveEmissionActivity();
        if (sheet == null || sheet.getId() == null) {
            return Map.of();
        }
        List<CeTemplateField> fields = templateFieldMapper.selectList(new LambdaQueryWrapper<CeTemplateField>()
            .eq(CeTemplateField::getSheetId, sheet.getId())
            .in(CeTemplateField::getBusinessFieldCode, List.of(FIELD_SOURCE_CODE, FIELD_YEAR, FIELD_MONTH, FIELD_DEPARTMENT)));
        Map<Long, String> fieldCodeById = fields.stream()
            .filter(field -> field.getId() != null && StringUtils.isNotBlank(field.getBusinessFieldCode()))
            .collect(Collectors.toMap(CeTemplateField::getId, CeTemplateField::getBusinessFieldCode, (left, right) -> left));
        if (fieldCodeById.isEmpty()) {
            return Map.of();
        }
        Set<Long> periodFieldIds = fieldCodeById.entrySet().stream()
            .filter(entry -> FIELD_YEAR.equals(entry.getValue()) || FIELD_MONTH.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (periodFieldIds.isEmpty()) {
            return Map.of();
        }

        List<CeCaptureCell> periodCells = captureCellMapper.selectList(new LambdaQueryWrapper<CeCaptureCell>()
            .in(CeCaptureCell::getFieldId, periodFieldIds));
        Map<Long, Map<String, String>> periodValuesByRowId = new HashMap<>();
        for (CeCaptureCell cell : periodCells) {
            if (cell.getRowId() == null || cell.getFieldId() == null) {
                continue;
            }
            String fieldCode = fieldCodeById.get(cell.getFieldId());
            if (StringUtils.isBlank(fieldCode)) {
                continue;
            }
            periodValuesByRowId.computeIfAbsent(cell.getRowId(), ignored -> new HashMap<>())
                .put(fieldCode, cell.getTextValue());
        }
        Set<Long> matchingRowIds = periodValuesByRowId.entrySet().stream()
            .filter(entry -> period.equals(toPeriod(entry.getValue().get(FIELD_YEAR), entry.getValue().get(FIELD_MONTH))))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (matchingRowIds.isEmpty()) {
            return Map.of();
        }

        List<CeCaptureRow> rows = captureRowMapper.selectList(new LambdaQueryWrapper<CeCaptureRow>()
            .eq(CeCaptureRow::getSheetId, sheet.getId())
            .in(CeCaptureRow::getId, matchingRowIds));
        Map<Long, CeCaptureRow> rowById = rows.stream()
            .filter(row -> row.getId() != null)
            .collect(Collectors.toMap(CeCaptureRow::getId, Function.identity(), (left, right) -> left));
        if (rowById.isEmpty()) {
            return Map.of();
        }
        matchingRowIds = rowById.keySet();
        List<CeCaptureCell> cells = captureCellMapper.selectList(new LambdaQueryWrapper<CeCaptureCell>()
            .in(CeCaptureCell::getRowId, matchingRowIds)
            .in(CeCaptureCell::getFieldId, fieldCodeById.keySet()));
        Map<Long, Map<String, String>> valuesByRowId = new HashMap<>();
        for (CeCaptureCell cell : cells) {
            if (cell.getRowId() == null || cell.getFieldId() == null) {
                continue;
            }
            String fieldCode = fieldCodeById.get(cell.getFieldId());
            if (StringUtils.isBlank(fieldCode)) {
                continue;
            }
            valuesByRowId.computeIfAbsent(cell.getRowId(), ignored -> new HashMap<>())
                .put(fieldCode, cell.getTextValue());
        }
        Set<Long> batchIds = rows.stream().map(CeCaptureRow::getBatchId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CeCaptureBatch> batchById = batchIds.isEmpty()
            ? Map.of()
            : captureBatchMapper.selectBatchIds(batchIds).stream()
                .collect(Collectors.toMap(CeCaptureBatch::getId, Function.identity(), (left, right) -> left));

        Map<String, CaptureSubmissionMeta> result = new HashMap<>();
        for (Long rowId : matchingRowIds) {
            Map<String, String> values = valuesByRowId.get(rowId);
            if (values == null) {
                continue;
            }
            String sourceCode = values.get(FIELD_SOURCE_CODE);
            if (StringUtils.isBlank(sourceCode)) {
                continue;
            }
            CeCaptureRow row = rowById.get(rowId);
            CeCaptureBatch batch = row == null ? null : batchById.get(row.getBatchId());
            CaptureSubmissionMeta meta = new CaptureSubmissionMeta(
                values.get(FIELD_DEPARTMENT),
                batch == null ? null : batch.getSubmittedBy(),
                batch == null ? null : batch.getSubmittedTime()
            );
            result.merge(sourceCode.trim(), meta, this::newerMeta);
        }
        return result;
    }

    private CeTemplateSheet resolveEmissionActivity() {
        return templateSheetMapper.selectList(new LambdaQueryWrapper<CeTemplateSheet>()
                .eq(CeTemplateSheet::getTargetTableCode, TARGET_TABLE_CODE)
                .orderByDesc(CeTemplateSheet::getTemplateVersionId)
                .orderByDesc(CeTemplateSheet::getId))
            .stream()
            .findFirst()
            .orElse(null);
    }

    private String toPeriod(String year, String month) {
        if (StringUtils.isBlank(year) || StringUtils.isBlank(month)) {
            return "";
        }
        try {
            int monthValue = Integer.parseInt(month.trim());
            return year.trim() + "-" + String.format("%02d", monthValue);
        } catch (NumberFormatException ex) {
            return year.trim() + "-" + month.trim();
        }
    }

    private CaptureSubmissionMeta newerMeta(CaptureSubmissionMeta left, CaptureSubmissionMeta right) {
        Date leftTime = Optional.ofNullable(left.submittedTime()).orElse(new Date(0));
        Date rightTime = Optional.ofNullable(right.submittedTime()).orElse(new Date(0));
        return rightTime.after(leftTime) ? right : left;
    }

    private SubmissionAggregate submissionAggregate(Map<String, SubmissionAggregate> aggregates, String department,
                                                     String responsiblePerson, String factoryName, ActivityPeriod period,
                                                     String dueDate) {
        String key = department + "|" + responsiblePerson + "|" + factoryName;
        return aggregates.computeIfAbsent(key, ignored -> new SubmissionAggregate(department, responsiblePerson, factoryName,
            period, dueDate));
    }

    private static String sourceName(CeEmissionSource source) {
        if (source == null) {
            return "--";
        }
        if (StringUtils.isNotBlank(source.getEmissionSourceName())) {
            return source.getEmissionSourceName();
        }
        if (StringUtils.isNotBlank(source.getSourceIdentificationName())) {
            return source.getSourceIdentificationName();
        }
        return "--";
    }

    private static class SubmissionAggregate {

        private final String department;
        private final String responsiblePerson;
        private final String factoryName;
        private final ActivityPeriod period;
        private final String dueDate;
        private int expectedCount;
        private int submittedCount;
        private int missingCount;
        private int warningCount;
        private Date latestSubmittedTime;
        private CeEmissionSource firstSource;

        SubmissionAggregate(String department, String responsiblePerson, String factoryName, ActivityPeriod period, String dueDate) {
            this.department = department;
            this.responsiblePerson = responsiblePerson;
            this.factoryName = factoryName;
            this.period = period;
            this.dueDate = dueDate;
        }

        void addActivity(CeEmissionSource source, String status, Date submittedTime,
                         List<CeActivityDataValidationDashboardVo.ValidationIssue> issues) {
            expectedCount++;
            if (firstSource == null) {
                firstSource = source;
            }
            if (STATUS_SUBMITTED.equals(status)) {
                submittedCount++;
            } else if ("missing".equals(status)) {
                missingCount++;
            }
            if (issues.stream().anyMatch(issue -> "WARNING".equals(issue.getSeverity()))) {
                warningCount++;
            }
            if (submittedTime != null && (latestSubmittedTime == null || submittedTime.after(latestSubmittedTime))) {
                latestSubmittedTime = submittedTime;
            }
        }

        CeActivityDataValidationDashboardVo.SubmissionStatus toVo() {
            CeActivityDataValidationDashboardVo.SubmissionStatus submission = new CeActivityDataValidationDashboardVo.SubmissionStatus();
            submission.setDepartment(department);
            submission.setResponsiblePerson(responsiblePerson);
            submission.setFactoryName(factoryName);
            submission.setModuleName(MODULE_ACTIVITY);
            submission.setExpectedCount(expectedCount);
            submission.setSubmittedCount(submittedCount);
            submission.setMissingCount(missingCount);
            submission.setWarningCount(warningCount);
            submission.setSourceIdentificationCode(firstSource == null ? null : firstSource.getSourceIdentificationCode());
            submission.setSourceIdentificationName(firstSource == null ? null : firstSource.getSourceIdentificationName());
            submission.setEmissionSourceName(firstSource == null ? null : sourceName(firstSource));
            submission.setActivityYear(period.year());
            submission.setActivityMonth(period.month());
            submission.setDueDate(dueDate);
            submission.setSubmissionStatus(resolveStatus());
            submission.setSubmittedTime(latestSubmittedTime);
            submission.setAccuracyRate(resolveAccuracy());
            return submission;
        }

        private String resolveStatus() {
            if (missingCount > 0) {
                return "missing";
            }
            if (submittedCount < expectedCount || warningCount > 0) {
                return STATUS_DRAFT;
            }
            return STATUS_SUBMITTED;
        }

        private BigDecimal resolveAccuracy() {
            if (expectedCount == 0) {
                return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(submittedCount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(expectedCount), 1, RoundingMode.HALF_UP);
        }
    }

    private record CaptureSubmissionMeta(String department, String submittedBy, Date submittedTime) {
    }

    private record ActivityPeriod(Integer year, Integer month) {

        String label() {
            if (year == null) {
                return "";
            }
            if (month == null) {
                return String.valueOf(year);
            }
            return year + "-" + String.format("%02d", month);
        }
    }
}
