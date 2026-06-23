package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeActivityData;
import org.dromara.carbon.enterprise.domain.CeCaptureBatch;
import org.dromara.carbon.enterprise.domain.CeCaptureCell;
import org.dromara.carbon.enterprise.domain.CeCaptureRow;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.domain.CeGreenPowerCertificate;
import org.dromara.carbon.enterprise.domain.CeIntensityDenominatorFact;
import org.dromara.carbon.enterprise.domain.CeTemplateField;
import org.dromara.carbon.enterprise.domain.CeTemplateSheet;
import org.dromara.carbon.enterprise.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataVo;
import org.dromara.carbon.enterprise.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureBatchMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureCellMapper;
import org.dromara.carbon.enterprise.mapper.CeCaptureRowMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.mapper.CeGreenPowerCertificateMapper;
import org.dromara.carbon.enterprise.mapper.CeIntensityDenominatorFactMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateFieldMapper;
import org.dromara.carbon.enterprise.mapper.CeTemplateSheetMapper;
import org.dromara.carbon.enterprise.service.ICeActivityDataService;
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
    private static final String TARGET_TABLE_CODE = "sheet_656";
    private static final String FIELD_SOURCE_CODE = "f001";
    private static final String FIELD_YEAR = "f011";
    private static final String FIELD_MONTH = "f012";
    private static final String FIELD_DEPARTMENT = "f015";
    private static final String MODULE_ACTIVITY = "Activity data";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

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
        LambdaQueryWrapper<CeActivityData> wrapper = buildQueryWrapper(bo)
            .orderByDesc(CeActivityData::getActivityYear)
            .orderByDesc(CeActivityData::getActivityMonth)
            .orderByDesc(CeActivityData::getCreateTime)
            .orderByDesc(CeActivityData::getId);
        IPage<CeActivityDataVo> page = activityDataMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CeActivityDataVo> queryList(CeActivityDataBo bo) {
        return activityDataMapper.selectVoList(buildQueryWrapper(bo)
            .orderByDesc(CeActivityData::getActivityYear)
            .orderByDesc(CeActivityData::getActivityMonth)
            .orderByDesc(CeActivityData::getCreateTime)
            .orderByDesc(CeActivityData::getId));
    }

    @Override
    public CeActivityDataValidationDashboardVo queryValidationDashboard(CeActivityDataBo bo) {
        ActivityPeriod period = resolvePeriod(bo);
        String dueDate = resolveDueDate(period);
        List<CeEmissionSource> sources = listEnabledEmissionSources();
        List<CeActivityData> activities = listActivities(period);
        List<CeGreenPowerCertificate> greenCertificates = listGreenPowerCertificates(period);
        List<CeIntensityDenominatorFact> denominatorFacts = listDenominatorFacts(period);
        Map<String, List<CeActivityData>> activitiesBySourceCode = activities.stream()
            .filter(activity -> StringUtils.isNotBlank(activity.getSourceIdentificationCode()))
            .collect(Collectors.groupingBy(activity -> activity.getSourceIdentificationCode().trim()));
        Map<String, CeEmissionSource> sourceByCode = sources.stream()
            .filter(source -> StringUtils.isNotBlank(source.getSourceIdentificationCode()))
            .collect(Collectors.toMap(source -> source.getSourceIdentificationCode().trim(), Function.identity(), (left, right) -> left));
        Map<String, CaptureSubmissionMeta> captureMetaBySourceCode = loadCaptureMetaBySourceCode(period.label());

        List<CeEmissionSource> expectedSources = new ArrayList<>(sources);
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
            .eq(StringUtils.isNotBlank(bo.getActivityUnit()), CeActivityData::getActivityUnit, bo.getActivityUnit())
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
                .last("limit 1"))
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
            issues.add(issue("MISSING_ACTIVITY_DATA", "Activity data missing", "ERROR", source, period,
                "Enabled emission source has no activity data for the selected period.", "Record activity data for the period.", "missing"));
            return issues;
        }
        if (!isSubmitted(activity)) {
            issues.add(issue("UNSUBMITTED_ACTIVITY_DATA", "Activity data not submitted", "WARNING", source, period,
                "Activity data is still draft.", "Submit or review the draft data.", "pending"));
        }
        if (activity.getActivityValue() == null || BigDecimal.ZERO.compareTo(activity.getActivityValue()) >= 0) {
            issues.add(issue("INVALID_ACTIVITY_VALUE", "Invalid activity value", "ERROR", source, period,
                "Activity value is empty or less than or equal to 0.", "Check the original reading and save again.", "abnormal"));
        }
        if (StringUtils.isBlank(activity.getActivityUnit())) {
            issues.add(issue("MISSING_ACTIVITY_UNIT", "Activity unit missing", "ERROR", source, period,
                "Activity data is missing a unit.", "Check the emission source unit and save again.", "abnormal"));
        }
        if (isSubmitted(activity) && StringUtils.isBlank(activity.getFactorKey())) {
            issues.add(issue("MISSING_FACTOR_CONFIRMATION", "Factor not confirmed", "WARNING", source, period,
                "Submitted activity data is not linked to a confirmed emission factor.", "Confirm the factor and recalculate.", "pending"));
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
                issues.add(issueForObject("INVALID_GREEN_POWER_AMOUNT", "Invalid green power quantity", "ERROR", objectName, period,
                    "Green power certificate quantity is empty or less than or equal to 0.", "Check certificate quantity and save again.", "abnormal"));
            }
            if ("voided".equals(certificate.getProofStatus())) {
                issues.add(issueForObject("VOIDED_GREEN_POWER_PROOF", "Green power proof voided", "ERROR", objectName, period,
                    "Green power certificate proof status is voided.", "Replace the certificate or remove the offset.", "abnormal"));
            } else if (!"verified".equals(certificate.getProofStatus())) {
                issues.add(issueForObject("PENDING_GREEN_POWER_PROOF", "Green power proof pending", "WARNING", objectName, period,
                    "Green power certificate proof is not verified.", "Complete certificate verification.", "pending"));
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
                issues.add(issueForObject("MISSING_DENOMINATOR_CODE", "Denominator type missing", "WARNING", objectName, period,
                    "Denominator fact is missing a denominator type.", "Complete denominator type.", "pending"));
            }
            if (fact.getDenominatorValue() == null || BigDecimal.ZERO.compareTo(fact.getDenominatorValue()) >= 0) {
                issues.add(issueForObject("INVALID_DENOMINATOR_VALUE", "Invalid denominator value", "ERROR", objectName, period,
                    "Denominator value is empty or less than or equal to 0.", "Check denominator source data.", "abnormal"));
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
        return captureMeta == null || StringUtils.isBlank(captureMeta.department()) ? "Unassigned department" : captureMeta.department();
    }

    private String resolveResponsiblePerson(CaptureSubmissionMeta captureMeta) {
        return captureMeta == null || StringUtils.isBlank(captureMeta.submittedBy()) ? "--" : captureMeta.submittedBy();
    }

    private Map<String, CaptureSubmissionMeta> loadCaptureMetaBySourceCode(String period) {
        CeTemplateSheet sheet = resolveSheet656();
        if (sheet == null || sheet.getId() == null) {
            return Map.of();
        }
        List<CeTemplateField> fields = templateFieldMapper.selectList(new LambdaQueryWrapper<CeTemplateField>()
            .eq(CeTemplateField::getSheetId, sheet.getId())
            .in(CeTemplateField::getTargetColumnCode, List.of(FIELD_SOURCE_CODE, FIELD_YEAR, FIELD_MONTH, FIELD_DEPARTMENT)));
        Map<Long, String> fieldCodeById = fields.stream()
            .filter(field -> field.getId() != null && StringUtils.isNotBlank(field.getTargetColumnCode()))
            .collect(Collectors.toMap(CeTemplateField::getId, CeTemplateField::getTargetColumnCode, (left, right) -> left));
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

    private CeTemplateSheet resolveSheet656() {
        return templateSheetMapper.selectList(new LambdaQueryWrapper<CeTemplateSheet>()
                .eq(CeTemplateSheet::getTargetTableCode, TARGET_TABLE_CODE)
                .orderByDesc(CeTemplateSheet::getTemplateVersionId)
                .orderByDesc(CeTemplateSheet::getId)
                .last("limit 1"))
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
