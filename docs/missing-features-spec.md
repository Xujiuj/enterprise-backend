# Spec: Enterprise Backend Missing Features

Updated: 2026-06-05

## Objective

Finish the enterprise-side local backend for the carbon data management platform. This backend owns local enterprise runtime, local business data entry, validation, License status consumption, and reporting gates. It must not depend on vendor-side databases for enterprise business data.

Current baseline:

- License import and current-state storage exist.
- License gate service exists and reads current enterprise License state.
- `GET /enterprise/license-gate/current?expectedInstallId=...` returns `ALLOW` or `DENY`.
- Phase 0 verification passed with 24 enterprise License tests.

The remaining work is to connect the gate to real operations, implement the first activity-data validation slice, and prepare report-facing data for SQL Server / Power BI.

## Tech Stack

- Java 17
- Spring Boot 3.5.14
- MyBatis-Plus 3.5.16
- Sa-Token 1.45.0
- Maven multi-module project based on RuoYi-Vue-Plus
- Primary module for new enterprise business code: `ruoyi-modules/carbon-enterprise`

## Commands

Run from `enterprise-backend` unless noted.

```powershell
rtk mvn -pl ruoyi-modules/carbon-enterprise -am "-DskipTests=false" "-Dtest=*License*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test
rtk mvn -pl ruoyi-modules/carbon-enterprise -am "-DskipTests=false" test
rtk mvn -pl ruoyi-admin -am package -DskipTests
```

Cross-repository boundary checks are run from the original parent workspace when available:

```powershell
rtk python tools\verify_backend_module_boundaries.py
rtk python tools\verify_sql_boundaries.py
```

## Project Structure

```text
ruoyi-admin/                         Spring Boot application entrypoint
ruoyi-common/                        Shared RuoYi common modules
ruoyi-modules/carbon-enterprise/     Enterprise-owned carbon domain code
script/sql/mysql/                    MySQL development schema and seed scripts
script/sql/sqlserver/                SQL Server reporting and permission scripts
docs/                                Enterprise backend specs and task plans
```

## Current Completed Capabilities

- Enterprise License import HTTP entry.
- Enterprise License current-state query.
- Enterprise License gate service.
- License gate reasons:
  - `NO_VALID_LICENSE`
  - `CLOCK_ROLLBACK`
  - `EXPIRED`
  - `INSTALL_ID_MISMATCH`
- Gate implementation consumes stored enterprise state and does not repeat signature verification.

## Missing Features

### EB-1: Operation/Login License Enforcement

The gate is implemented but not yet wired into login or protected business operations.

Requirements:

- Reuse `ICeLicenseGateService`.
- Do not re-parse `.lic` or re-run signature verification.
- Block protected enterprise business operations when gate result is `DENY`.
- Return stable business error codes suitable for enterprise-ui display.
- Keep system/admin bootstrap paths explicitly documented if any path is exempt.

### EB-2: Activity Template Validation for `sheet_656`

The first activity template mapping is frozen for module `03-活动数据`, sheet `天然气`, target table code `sheet_656`.

Requirements:

- Preserve all 18 source columns and their order.
- Treat `f001` as the primary selected emission-source dimension.
- Recalculate or derive `f002-f010` and `f018` from enterprise master data and current factor version; do not trust client-provided values for these fields.
- Strong validation blocks save/import:
  - missing required fields
  - invalid type
  - missing master-data match
  - inconsistent derived fields
  - invalid value domain
  - `f014 <= 0`
- Weak validation returns warnings but allows draft save.
- Error responses must include row number and source column name for import flows.

### EB-3: Activity Import API

The backend does not yet expose an import flow that consumes the `sheet_656` Excel shape.

Requirements:

- Accept only the frozen 18-column header shape for the first slice.
- Reject renamed, deleted, or extra required columns unless a future spec explicitly adds an extension path.
- Return structured row-level errors and warnings.
- Persist only validated enterprise-local data.
- Do not call vendor services during import.

### EB-4: Reporting View Seed and License-Gated SQL Server Path

SQL boundary scripts exist, but the first business reporting view and real-database evidence are still missing.

Requirements:

- Reporting views must live under SQL Server `rpt`.
- `pbi_user` may read `rpt` only and must not read/write `dbo`.
- Business reporting views must depend on the License gate, for example with `WHERE EXISTS (SELECT 1 FROM rpt.v_LicenseGate)`.
- `rpt.v_LicenseGate` is only a gate behavior check view, not the ordinary business report sample view.

### EB-5: Install ID / Device Fingerprint Source

Current HTTP flows accept `expectedInstallId` explicitly for testability.

Requirements:

- Define a production install-id provider before replacing explicit request parameters in protected flows.
- Keep test seams for deterministic License tests.
- Do not derive install ID from vendor data.

## Code Style

Follow the existing RuoYi layering:

```java
@RequiredArgsConstructor
@Service
public class CeExampleServiceImpl implements ICeExampleService {

    private final CeDependencyService dependencyService;

    @Override
    public CeResult validate(CeRequest request) {
        // Service owns business decisions; controller only adapts HTTP.
        return dependencyService.queryCurrent()
            .map(this::toResult)
            .orElseGet(CeResult::deny);
    }
}
```

Conventions:

- Controllers adapt HTTP and permission annotations only.
- Services own business decisions.
- Domain DTOs expose stable frontend-facing fields only.
- Tests must cover positive and negative business branches.

## Testing Strategy

- Unit tests for service decisions.
- Controller tests for HTTP shape, validation, and permission-facing behavior.
- Import validation tests for row/column-specific errors.
- SQL boundary scripts for enterprise/vendor data separation.
- Maven commands must override root `<skipTests>true</skipTests>` with `-DskipTests=false`.

## Boundaries

- Always:
  - Keep enterprise business code inside `ruoyi-modules/carbon-enterprise`.
  - Reuse License state/gate services instead of duplicating verification.
  - Run targeted Maven tests before commit.
  - Return sanitized business errors.
- Ask first:
  - Database schema changes.
  - New dependencies.
  - Changing License payload fields.
  - Changing SQL Server permission model.
- Never:
  - Import vendor backend packages.
  - Store or return vendor private keys or `privateKeyRef`.
  - Let Power BI connect to vendor data for enterprise reports.
  - Trust client-provided derived activity fields.

## Success Criteria

- Protected enterprise operations are denied when License gate is closed.
- `sheet_656` backend validation blocks strong errors and returns weak warnings.
- Import API reports row and column names for validation failures.
- First approved `rpt` business view exists and is gated by License state.
- Targeted tests and boundary checks pass.

## Open Questions

- Which enterprise operations are exempt from License gate for bootstrap or recovery?
- What is the production install-id provider?
- Which raw activity table becomes the first SQL Server `rpt` business reporting view?
