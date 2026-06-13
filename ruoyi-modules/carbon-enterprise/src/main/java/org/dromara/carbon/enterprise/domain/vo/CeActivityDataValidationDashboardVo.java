package org.dromara.carbon.enterprise.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Enterprise activity-data validation dashboard.
 */
@Data
public class CeActivityDataValidationDashboardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String activityPeriod;

    private String dueDate;

    private Integer expectedItems = 0;

    private Integer validatedRecordCount = 0;

    private Integer submittedItems = 0;

    private Integer abnormalItems = 0;

    private Integer missingItems = 0;

    private Integer draftItems = 0;

    private BigDecimal accuracyRate = BigDecimal.ZERO;

    private BigDecimal passRate = BigDecimal.ZERO;

    private List<SubmissionStatus> submissions = new ArrayList<>();

    private List<ValidationIssue> issues = new ArrayList<>();

    @Data
    public static class SubmissionStatus implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String department;

        private String responsiblePerson;

        private String facilityName;

        private String moduleName;

        private Integer expectedCount;

        private Integer submittedCount;

        private Integer missingCount;

        private Integer warningCount;

        private Long emissionSourceId;

        private String emissionSourceCode;

        private String emissionSourceName;

        private String activityPeriod;

        private String dueDate;

        private String submissionStatus;

        private Date submittedTime;

        private BigDecimal accuracyRate;
    }

    @Data
    public static class ValidationIssue implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String ruleCode;

        private String ruleName;

        private String severity;

        private Long emissionSourceId;

        private String emissionSourceCode;

        private String objectName;

        private String activityPeriod;

        private String description;

        private String suggestion;

        private String issueStatus;
    }
}
