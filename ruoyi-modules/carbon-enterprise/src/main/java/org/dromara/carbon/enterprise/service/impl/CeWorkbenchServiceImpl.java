package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.client.CeVendorAnnouncementOpenClient;
import org.dromara.carbon.enterprise.domain.CeActivityData;
import org.dromara.carbon.enterprise.domain.CeEmissionSource;
import org.dromara.carbon.enterprise.domain.CeFactorConfirmation;
import org.dromara.carbon.enterprise.domain.CeReportTemplateFile;
import org.dromara.carbon.enterprise.domain.bo.CeActivityDataBo;
import org.dromara.carbon.enterprise.domain.sync.CeVendorAnnouncementListResponse;
import org.dromara.carbon.enterprise.domain.sync.CeVendorAnnouncementRecord;
import org.dromara.carbon.enterprise.domain.vo.CeActivityDataValidationDashboardVo;
import org.dromara.carbon.enterprise.domain.vo.CeLicenseStateVo;
import org.dromara.carbon.enterprise.domain.vo.CeWorkbenchOverviewVo;
import org.dromara.carbon.enterprise.mapper.CeActivityDataMapper;
import org.dromara.carbon.enterprise.mapper.CeEmissionSourceMapper;
import org.dromara.carbon.enterprise.mapper.CeFactorConfirmationMapper;
import org.dromara.carbon.enterprise.mapper.CeReportTemplateFileMapper;
import org.dromara.carbon.enterprise.service.ICeActivityDataService;
import org.dromara.carbon.enterprise.service.ICeLicenseStateService;
import org.dromara.carbon.enterprise.service.ICeWorkbenchService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Enterprise workbench service implementation.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class CeWorkbenchServiceImpl implements ICeWorkbenchService {

    private static final String LICENSE_VALID = "VALID";
    private static final String UNIT_TCO2E = "tCO2e";
    private static final int ANNOUNCEMENT_LIMIT = 5;

    private final ICeActivityDataService activityDataService;
    private final ICeLicenseStateService licenseStateService;
    private final CeActivityDataMapper activityDataMapper;
    private final CeEmissionSourceMapper emissionSourceMapper;
    private final CeFactorConfirmationMapper factorConfirmationMapper;
    private final CeReportTemplateFileMapper reportTemplateFileMapper;
    private final CeVendorAnnouncementOpenClient vendorAnnouncementOpenClient;

    @Override
    public CeWorkbenchOverviewVo overview() {
        CeActivityDataValidationDashboardVo dashboard = activityDataService.queryValidationDashboard(new CeActivityDataBo());
        String period = dashboard.getActivityPeriod();
        CeLicenseStateVo licenseState = licenseStateService.queryCurrent();
        List<CeActivityData> activities = listActivities(period);
        List<CeEmissionSource> sources = emissionSourceMapper.selectList(Wrappers.<CeEmissionSource>lambdaQuery());
        List<CeFactorConfirmation> factors = listLatestFactors();
        List<CeReportTemplateFile> templates = listLatestTemplates();

        BigDecimal totalEmission = totalEmission(activities);
        CeWorkbenchOverviewVo overview = new CeWorkbenchOverviewVo();
        overview.setCurrentPeriod(period);
        overview.setStatusCards(statusCards(licenseState, dashboard, totalEmission, latestFactorVersion(factors)));
        overview.setCycleStages(cycleStages(dashboard, latestTemplateName(templates)));
        overview.setScopeEmissions(scopeEmissions(activities, sources, totalEmission));
        overview.setTodoItems(todoItems(licenseState, dashboard, latestTemplateName(templates), latestFactorVersion(factors)));
        overview.setRecentActivities(recentActivities(licenseState, dashboard, factors, activities));
        overview.setSystemNotices(systemNotices(licenseState));
        return overview;
    }

    private List<CeActivityData> listActivities(String period) {
        return activityDataMapper.selectList(Wrappers.<CeActivityData>lambdaQuery()
            .eq(StringUtils.isNotBlank(period), CeActivityData::getActivityPeriod, period)
            .orderByDesc(CeActivityData::getUpdateTime)
            .orderByDesc(CeActivityData::getCreateTime)
            .orderByDesc(CeActivityData::getId));
    }

    private List<CeFactorConfirmation> listLatestFactors() {
        return factorConfirmationMapper.selectList(Wrappers.<CeFactorConfirmation>lambdaQuery()
                .orderByDesc(CeFactorConfirmation::getUpdateTime)
                .orderByDesc(CeFactorConfirmation::getCreateTime)
                .orderByDesc(CeFactorConfirmation::getId))
            .stream()
            .limit(20)
            .toList();
    }

    private List<CeReportTemplateFile> listLatestTemplates() {
        return reportTemplateFileMapper.selectList(Wrappers.<CeReportTemplateFile>lambdaQuery()
                .orderByDesc(CeReportTemplateFile::getUpdateTime)
                .orderByDesc(CeReportTemplateFile::getCreateTime)
                .orderByDesc(CeReportTemplateFile::getId))
            .stream()
            .limit(20)
            .toList();
    }

    private List<CeWorkbenchOverviewVo.MetricCard> statusCards(CeLicenseStateVo licenseState,
                                                               CeActivityDataValidationDashboardVo dashboard,
                                                               BigDecimal totalEmission,
                                                               String latestFactorVersion) {
        return List.of(
            metricCard("授权状态", licenseStatusText(licenseState), licenseNote(licenseState),
                isLicenseValid(licenseState) ? "is-success" : "is-warning", null, null),
            metricCard("当前期间", emptyToDash(dashboard.getActivityPeriod()),
                "已录入 " + number(dashboard.getSubmittedItems()) + " 项",
                null, rate(dashboard.getPassRate()), "up"),
            metricCard("碳排放总量", totalEmission.signum() > 0 ? decimal(totalEmission) : "--",
                UNIT_TCO2E, null,
                dashboard.getAbnormalItems() != null && dashboard.getAbnormalItems() > 0 ? dashboard.getAbnormalItems() + " 项异常" : null,
                dashboard.getAbnormalItems() != null && dashboard.getAbnormalItems() > 0 ? "down" : null),
            metricCard("因子库版本", emptyToDash(latestFactorVersion),
                StringUtils.isBlank(latestFactorVersion) ? "暂无同步版本" : "已从厂商同步",
                null, null, null)
        );
    }

    private List<CeWorkbenchOverviewVo.CycleStage> cycleStages(CeActivityDataValidationDashboardVo dashboard, String latestTemplateName) {
        int missingItems = defaultInt(dashboard.getMissingItems());
        int abnormalItems = defaultInt(dashboard.getAbnormalItems());
        int reportableRecordCount = Math.max(defaultInt(dashboard.getValidatedRecordCount()) - abnormalItems, 0);
        return List.of(
            cycleStage("已保存活动数据", number(dashboard.getSubmittedItems()),
                "当前期间 " + emptyToDash(dashboard.getActivityPeriod()), "done"),
            cycleStage("待补录项目", number(missingItems),
                StringUtils.isBlank(dashboard.getDueDate()) ? "暂无截止日期" : "截止 " + dashboard.getDueDate(),
                missingItems > 0 ? "warn" : "done"),
            cycleStage("审核通过率", rate(dashboard.getPassRate()),
                abnormalItems > 0 ? abnormalItems + " 项异常待复核" : "暂无异常项", "done"),
            cycleStage("报表库可用记录", number(reportableRecordCount),
                StringUtils.isBlank(latestTemplateName) ? "暂无模板记录" : latestTemplateName, "info")
        );
    }

    private List<CeWorkbenchOverviewVo.ScopeEmission> scopeEmissions(List<CeActivityData> activities,
                                                                     List<CeEmissionSource> sources,
                                                                     BigDecimal totalEmission) {
        Map<Long, String> scopeBySourceId = sources.stream()
            .filter(source -> source.getId() != null)
            .collect(LinkedHashMap::new,
                (map, source) -> map.put(source.getId(), StringUtils.isBlank(source.getBoundaryScope()) ? "未配置边界" : source.getBoundaryScope()),
                Map::putAll);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (CeActivityData activity : activities) {
            BigDecimal emission = activity.getCalculatedEmission();
            if (emission == null) {
                continue;
            }
            String scope = scopeBySourceId.getOrDefault(activity.getEmissionSourceId(), "未配置边界");
            totals.merge(scope, emission, BigDecimal::add);
        }
        return totals.entrySet().stream()
            .map(entry -> scopeEmission(entry.getKey(), entry.getValue(), totalEmission))
            .sorted(Comparator.comparing(CeWorkbenchOverviewVo.ScopeEmission::getLabel))
            .toList();
    }

    private List<CeWorkbenchOverviewVo.TodoItem> todoItems(CeLicenseStateVo licenseState,
                                                           CeActivityDataValidationDashboardVo dashboard,
                                                           String latestTemplateName,
                                                           String latestFactorVersion) {
        return List.of(
            todoItem("授权", isLicenseValid(licenseState) ? licenseNote(licenseState) : "暂无有效授权状态",
                isLicenseValid(licenseState) ? "正常" : "待处理", isLicenseValid(licenseState) ? "ok" : "warn",
                "/license-manage/license-import", "查看"),
            todoItem("报表模板", StringUtils.isBlank(latestTemplateName) ? "暂无可下载模板记录" : latestTemplateName,
                StringUtils.isBlank(latestTemplateName) ? "待同步" : "可下载", StringUtils.isBlank(latestTemplateName) ? "warn" : "info",
                "/data-management/report-template-download", StringUtils.isBlank(latestTemplateName) ? "查看" : "下载"),
            todoItem("数据录入", defaultInt(dashboard.getMissingItems()) > 0 ? dashboard.getMissingItems() + " 项未填报" : "本期暂无缺失项",
                defaultInt(dashboard.getMissingItems()) > 0 ? "未完成" : "正常", defaultInt(dashboard.getMissingItems()) > 0 ? "warn" : "ok",
                "/activity-data/emission-activity-entry", "去录入"),
            todoItem("因子库", StringUtils.isBlank(latestFactorVersion) ? "暂无因子同步记录" : latestFactorVersion + " 已同步",
                StringUtils.isBlank(latestFactorVersion) ? "待同步" : "已同步", StringUtils.isBlank(latestFactorVersion) ? "warn" : "info",
                "/data-management/factor-cache-record", "查看")
        );
    }

    private List<CeWorkbenchOverviewVo.RecentActivity> recentActivities(CeLicenseStateVo licenseState,
                                                                        CeActivityDataValidationDashboardVo dashboard,
                                                                        List<CeFactorConfirmation> factors,
                                                                        List<CeActivityData> activities) {
        return List.of(
                recentActivity(isLicenseValid(licenseState) ? "授权校验通过" : "授权状态待确认",
                    licenseState == null ? null : licenseState.getLastVerifiedTime(),
                    licenseState == null || licenseState.getValidTo() == null ? null : "有效期至 " + licenseState.getValidTo(),
                    isLicenseValid(licenseState) ? "ok" : ""),
                recentActivity(StringUtils.isBlank(latestFactorVersion(factors)) ? "暂无因子库更新" : "因子库已更新",
                    latestFactorTime(factors),
                    StringUtils.isBlank(latestFactorVersion(factors)) ? null : "当前版本 " + latestFactorVersion(factors),
                    StringUtils.isBlank(latestFactorVersion(factors)) ? "" : "ok"),
                recentActivity(defaultInt(dashboard.getSubmittedItems()) > 0 ? "录入保存" : "暂无本期录入动态",
                    latestActivityTime(activities),
                    defaultInt(dashboard.getSubmittedItems()) > 0 ? "已录入 " + dashboard.getSubmittedItems() + " 项" : null,
                    defaultInt(dashboard.getSubmittedItems()) > 0 ? "ok" : "")
            )
            .stream()
            .filter(item -> item.getTime() != null || StringUtils.isNotBlank(item.getDetail()))
            .toList();
    }

    private List<CeWorkbenchOverviewVo.SystemNotice> systemNotices(CeLicenseStateVo licenseState) {
        if (licenseState == null || StringUtils.isBlank(licenseState.getLicenseId()) || StringUtils.isBlank(licenseState.getInstallId())) {
            return List.of();
        }
        try {
            CeVendorAnnouncementListResponse response = vendorAnnouncementOpenClient.listAnnouncements(
                licenseState.getLicenseId(), licenseState.getInstallId(), ANNOUNCEMENT_LIMIT);
            return response.getAnnouncements().stream().map(this::systemNotice).toList();
        } catch (RuntimeException e) {
            log.error("获取厂商公告失败，licenseId={}", licenseState.getLicenseId(), e);
            return List.of(systemNoticeUnavailable());
        }
    }

    private CeWorkbenchOverviewVo.SystemNotice systemNoticeUnavailable() {
        CeWorkbenchOverviewVo.SystemNotice notice = new CeWorkbenchOverviewVo.SystemNotice();
        notice.setNoticeId(-1L);
        notice.setNoticeTitle("厂商公告暂不可用");
        notice.setNoticeType("warning");
        notice.setNoticeContent("暂时无法获取厂商公告，请稍后刷新或联系管理员检查厂商开放接口。");
        notice.setStatus("0");
        notice.setCreateTime(new Date());
        return notice;
    }

    private CeWorkbenchOverviewVo.SystemNotice systemNotice(CeVendorAnnouncementRecord record) {
        CeWorkbenchOverviewVo.SystemNotice notice = new CeWorkbenchOverviewVo.SystemNotice();
        notice.setNoticeId(record.getNoticeId());
        notice.setNoticeTitle(record.getNoticeTitle());
        notice.setNoticeType(record.getNoticeType());
        notice.setNoticeContent(record.getNoticeContent());
        notice.setStatus(record.getStatus());
        notice.setRemark(record.getRemark());
        notice.setCreateTime(record.getCreateTime());
        return notice;
    }

    private BigDecimal totalEmission(List<CeActivityData> activities) {
        return activities.stream()
            .map(CeActivityData::getCalculatedEmission)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CeWorkbenchOverviewVo.ScopeEmission scopeEmission(String label, BigDecimal value, BigDecimal totalEmission) {
        CeWorkbenchOverviewVo.ScopeEmission item = new CeWorkbenchOverviewVo.ScopeEmission();
        item.setLabel(label);
        item.setValue(value);
        item.setUnit(UNIT_TCO2E);
        item.setPercent(totalEmission.signum() > 0
            ? value.multiply(BigDecimal.valueOf(100)).divide(totalEmission, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
        return item;
    }

    private CeWorkbenchOverviewVo.MetricCard metricCard(String label, String value, String note, String valueClass, String badge, String badgeTone) {
        CeWorkbenchOverviewVo.MetricCard card = new CeWorkbenchOverviewVo.MetricCard();
        card.setLabel(label);
        card.setValue(value);
        card.setNote(note);
        card.setValueClass(valueClass);
        card.setBadge(badge);
        card.setBadgeTone(badgeTone);
        return card;
    }

    private CeWorkbenchOverviewVo.CycleStage cycleStage(String label, String value, String detail, String tone) {
        CeWorkbenchOverviewVo.CycleStage stage = new CeWorkbenchOverviewVo.CycleStage();
        stage.setLabel(label);
        stage.setValue(value);
        stage.setDetail(detail);
        stage.setTone(tone);
        return stage;
    }

    private CeWorkbenchOverviewVo.TodoItem todoItem(String type, String content, String status, String tone, String path, String action) {
        CeWorkbenchOverviewVo.TodoItem item = new CeWorkbenchOverviewVo.TodoItem();
        item.setType(type);
        item.setContent(content);
        item.setStatus(status);
        item.setTone(tone);
        item.setPath(path);
        item.setAction(action);
        return item;
    }

    private CeWorkbenchOverviewVo.RecentActivity recentActivity(String title, Date time, String detail, String tone) {
        CeWorkbenchOverviewVo.RecentActivity item = new CeWorkbenchOverviewVo.RecentActivity();
        item.setTitle(title);
        item.setTime(time);
        item.setDetail(detail);
        item.setTone(tone);
        return item;
    }

    private String latestFactorVersion(List<CeFactorConfirmation> factors) {
        return factors.stream()
            .map(CeFactorConfirmation::getFactorVersionCode)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(null);
    }

    private Date latestFactorTime(List<CeFactorConfirmation> factors) {
        return factors.stream()
            .map(factor -> factor.getUpdateTime() != null ? factor.getUpdateTime() : factor.getCreateTime())
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private String latestTemplateName(List<CeReportTemplateFile> templates) {
        return templates.stream()
            .map(template -> StringUtils.isNotBlank(template.getTemplateName()) ? template.getTemplateName() : template.getFileName())
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(null);
    }

    private Date latestActivityTime(List<CeActivityData> activities) {
        return activities.stream()
            .map(activity -> activity.getUpdateTime() != null ? activity.getUpdateTime() : activity.getCreateTime())
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private boolean isLicenseValid(CeLicenseStateVo licenseState) {
        return licenseState != null && LICENSE_VALID.equals(licenseState.getLicenseStatus());
    }

    private String licenseStatusText(CeLicenseStateVo licenseState) {
        if (licenseState == null || StringUtils.isBlank(licenseState.getLicenseStatus())) {
            return "未导入";
        }
        if (LICENSE_VALID.equals(licenseState.getLicenseStatus())) {
            return "有效";
        }
        if ("EXPIRED".equals(licenseState.getLicenseStatus())) {
            return "已过期";
        }
        return licenseState.getLicenseStatus();
    }

    private String licenseNote(CeLicenseStateVo licenseState) {
        if (licenseState == null || licenseState.getValidTo() == null) {
            return "暂无授权到期信息";
        }
        return licenseState.getValidTo() + " 到期";
    }

    private String rate(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        return value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private String decimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String number(Number value) {
        return value == null ? "--" : String.valueOf(value);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String emptyToDash(String value) {
        return StringUtils.isBlank(value) ? "--" : value;
    }
}
