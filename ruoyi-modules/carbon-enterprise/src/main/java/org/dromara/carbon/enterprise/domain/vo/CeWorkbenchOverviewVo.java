package org.dromara.carbon.enterprise.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Enterprise home workbench overview returned by the backend.
 */
@Data
public class CeWorkbenchOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String currentPeriod;

    private List<MetricCard> statusCards = new ArrayList<>();

    private List<CycleStage> cycleStages = new ArrayList<>();

    private List<ScopeEmission> scopeEmissions = new ArrayList<>();

    private List<TodoItem> todoItems = new ArrayList<>();

    private List<RecentActivity> recentActivities = new ArrayList<>();

    private List<SystemNotice> systemNotices = new ArrayList<>();

    @Data
    public static class MetricCard implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String label;

        private String value;

        private String note;

        private String valueClass;

        private String badge;

        private String badgeTone;
    }

    @Data
    public static class CycleStage implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String label;

        private String value;

        private String detail;

        private String tone;
    }

    @Data
    public static class ScopeEmission implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String label;

        private BigDecimal value;

        private String unit;

        private BigDecimal percent;
    }

    @Data
    public static class TodoItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String type;

        private String content;

        private String status;

        private String tone;

        private String path;

        private String action;
    }

    @Data
    public static class RecentActivity implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;

        private Date time;

        private String detail;

        private String tone;
    }

    @Data
    public static class SystemNotice implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long noticeId;

        private String noticeTitle;

        private String noticeType;

        private String noticeContent;

        private String status;

        private String remark;

        private Date createTime;
    }
}
