# Subagent Plan: Enterprise Backend

Updated: 2026-06-05

## Overview

This plan breaks the enterprise backend missing features into bounded subagent tasks. Each task must go through implementation, spec review, quality review, fixes, and re-review before being marked complete.

## Architecture Decisions

- License enforcement consumes `ICeLicenseGateService`; no task may duplicate import verification or signature checks.
- `sheet_656` is the first activity-data validation target.
- Reporting data must stay in enterprise SQL Server and expose only approved `rpt` views to Power BI.
- Shared parent-workspace scripts are not owned by ordinary implementers.

## Phase 1: License Gate Integration

### Task EB-1: Define Protected Operation Gate Points

**Description:** Identify enterprise backend routes/services that should be blocked when License gate returns `DENY`, and document any bootstrap exemptions.

**Acceptance criteria:**
- [ ] Protected and exempt endpoint list is written in `docs/license-gate-operation-map.md`.
- [ ] Exemptions have explicit rationale.
- [ ] No code changes.

**Verification:**
- [ ] `rtk rg -n "license-gate|CeLicenseGate|SaCheckPermission" ruoyi-modules/carbon-enterprise ruoyi-admin`

**Dependencies:** None

**Files likely touched:**
- `docs/license-gate-operation-map.md`

**Estimated scope:** S

### Task EB-2: Implement License Gate Interceptor Slice

**Description:** Add the smallest backend enforcement path for protected enterprise operations using `ICeLicenseGateService`.

**Acceptance criteria:**
- [ ] Protected operation returns stable DENY response when License gate is closed.
- [ ] Gate uses stored enterprise License state only.
- [ ] Tests cover ALLOW and DENY.

**Verification:**
- [ ] `rtk mvn -pl ruoyi-modules/carbon-enterprise -am "-DskipTests=false" "-Dtest=*License*Test,*Gate*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test`

**Dependencies:** EB-1

**Files likely touched:**
- `ruoyi-modules/carbon-enterprise/src/main/java/**`
- `ruoyi-modules/carbon-enterprise/src/test/java/**`

**Estimated scope:** M

## Phase 2: Activity Validation

### Task EB-3: Implement `sheet_656` Validation Service

**Description:** Implement a service-level validator for the frozen `天然气` activity template.

**Acceptance criteria:**
- [ ] Valid row passes with derived-field checks.
- [ ] Missing/invalid required fields produce blocking errors.
- [ ] Weak warnings do not block draft save.
- [ ] Client-provided `f002-f010` and `f018` are not trusted.

**Verification:**
- [ ] `rtk mvn -pl ruoyi-modules/carbon-enterprise -am "-DskipTests=false" "-Dtest=*Activity*Validation*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test`

**Dependencies:** None

**Files likely touched:**
- `ruoyi-modules/carbon-enterprise/src/main/java/org/dromara/carbon/enterprise/**`
- `ruoyi-modules/carbon-enterprise/src/test/java/org/dromara/carbon/enterprise/**`

**Estimated scope:** M

### Task EB-4: Add Activity Import Validation API

**Description:** Add an API slice that accepts `sheet_656` rows and returns row/column-level validation results.

**Acceptance criteria:**
- [ ] Header shape is validated.
- [ ] Row errors include source row number and column name.
- [ ] Import does not call vendor services.

**Verification:**
- [ ] `rtk mvn -pl ruoyi-modules/carbon-enterprise -am "-DskipTests=false" "-Dtest=*Activity*Import*Test,*Activity*Validation*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test`

**Dependencies:** EB-3

**Files likely touched:**
- `ruoyi-modules/carbon-enterprise/src/main/java/**`
- `ruoyi-modules/carbon-enterprise/src/test/java/**`

**Estimated scope:** M

## Phase 3: Reporting Gate

### Task EB-5: Add First `rpt` Business Reporting View

**Description:** Add the first SQL Server `rpt` business view for activity reporting, gated by `rpt.v_LicenseGate`.

**Acceptance criteria:**
- [ ] View is under `rpt`.
- [ ] View does not expose raw `dbo` table access to Power BI.
- [ ] View includes License gate condition.

**Verification:**
- [ ] `rtk python tools\verify_sql_boundaries.py` from parent workspace, when available.
- [ ] SQL script review confirms `WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate)` or equivalent gate.

**Dependencies:** EB-3

**Files likely touched:**
- `script/sql/sqlserver/**`
- `docs/reporting-view-evidence.md`

**Estimated scope:** S

## Checkpoint

After EB-2, EB-4, and EB-5:

- [ ] `rtk mvn -pl ruoyi-modules/carbon-enterprise -am "-DskipTests=false" test`
- [ ] `rtk mvn -pl ruoyi-admin -am package -DskipTests`
- [ ] Parent boundary scripts pass when parent workspace is available.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Gate blocks recovery/admin bootstrap | High | EB-1 documents exemptions before implementation |
| Activity import trusts derived fields | High | EB-3 tests forged derived values |
| SQL view bypasses License gate | High | EB-5 requires explicit gate condition and boundary review |
| Tests skipped by root Maven property | Medium | Always pass `-DskipTests=false` |

## Parallelization

- EB-1 and EB-3 can run in parallel.
- EB-2 waits for EB-1.
- EB-4 waits for EB-3.
- EB-5 can start after EB-3 defines the first reporting data shape.
