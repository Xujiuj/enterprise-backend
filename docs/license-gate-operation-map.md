# Enterprise License Gate Operation Map

Updated: 2026-06-05

## Scope

This document defines the gate points for EB-1 inside `enterprise-backend` only.
It covers the current enterprise-owned HTTP surface in:

- `ruoyi-modules/carbon-enterprise`
- `ruoyi-admin` only as an out-of-scope reference boundary for shared login infrastructure

This map is intentionally documentation-only. It does not add code and does not change permissions.

## Gate Rule

When the enterprise license gate returns `DENY`, protected operations must fail by reusing `ICeLicenseGateService` against stored enterprise license state.

The implementation for EB-2 must:

- reuse `ICeLicenseGateService`
- avoid `.lic` re-parse or signature verification duplication
- avoid vendor backend packages, data, or cross-repository state
- stay limited to enterprise runtime, validation, and report-facing gates

## EB-2 Install-Id Sourcing Rule

EB-2 must not widen every protected endpoint with a new public `expectedInstallId` request parameter just to call the license gate.

For the thin EB-2 slice:

- gate enforcement should obtain install-id from a server-side seam, provider, or equivalent internal source if one is needed
- the final production install-id provider design may remain deferred to EB-5
- whatever seam is used must still call `ICeLicenseGateService` against stored enterprise state rather than duplicating license verification logic

This document does not require a specific implementation if that provider/seam is not already present.

## Decision Basis

The current controller surface falls into three buckets:

1. Bootstrap and recovery endpoints that are needed to import or inspect license state.
2. Enterprise business read/write endpoints that expose local runtime, validation, or reporting-supporting data.
3. Shared platform login entry in `ruoyi-admin`, which is documented only as an out-of-scope reference and not as an EB-2 implementation target.

## Protected Operations

These endpoints should be treated as protected for EB-2 because they expose enterprise-local runtime data, business validation inventory, or mutable enterprise configuration that should not be usable when license state is closed.

| Endpoint | Permission | Why protected |
| --- | --- | --- |
| `GET /enterprise/extension-field/list` | `enterprise:extensionField:list` | Reads enterprise-managed extension-field configuration used by local validation/runtime behavior. |
| `GET /enterprise/extension-field/{id}` | `enterprise:extensionField:query` | Reads enterprise-managed extension-field details. |
| `POST /enterprise/extension-field` | `enterprise:extensionField:add` | Mutates enterprise-local configuration and must not remain writable during `DENY`. |
| `PUT /enterprise/extension-field` | `enterprise:extensionField:edit` | Mutates enterprise-local configuration and must not remain writable during `DENY`. |
| `DELETE /enterprise/extension-field/{ids}` | `enterprise:extensionField:remove` | Deletes enterprise-local configuration and must not remain writable during `DENY`. |
| `GET /enterprise/factor-cache-version/list` | `enterprise:factorCacheVersion:list` | Exposes enterprise factor-cache runtime state that supports downstream validation/report behavior. |
| `GET /enterprise/factor-cache-version/{id}` | `enterprise:factorCacheVersion:query` | Exposes enterprise factor-cache runtime details. |
| `GET /enterprise/capture-batch/list` | `enterprise:captureBatch:list` | Exposes enterprise-local activity capture/runtime data and should close with the license gate. |
| `GET /enterprise/capture-batch/{id}` | `enterprise:captureBatch:query` | Exposes enterprise-local activity capture/runtime details. |
| `GET /enterprise/capture-row/list` | `enterprise:captureRow:list` | Exposes enterprise-local activity capture/runtime data and should close with the license gate. |
| `GET /enterprise/capture-row/{id}` | `enterprise:captureRow:query` | Exposes enterprise-local activity capture/runtime details. |
| `GET /enterprise/capture-cell/list` | `enterprise:captureCell:list` | Exposes enterprise-local activity capture/runtime data and should close with the license gate. |
| `GET /enterprise/capture-cell/{id}` | `enterprise:captureCell:query` | Exposes enterprise-local activity capture/runtime details. |
| `GET /enterprise/template-version/list` | `enterprise:templateVersion:list` | Template version inventory drives enterprise-side validation/import behavior and should not remain operational after `DENY`. |
| `GET /enterprise/template-version/{id}` | `enterprise:templateVersion:query` | Template version details support enterprise validation/import behavior. |
| `GET /enterprise/template-sheet/list` | `enterprise:templateSheet:list` | Template sheet inventory supports enterprise validation/import behavior. |
| `GET /enterprise/template-sheet/{id}` | `enterprise:templateSheet:query` | Template sheet details support enterprise validation/import behavior. |
| `GET /enterprise/template-field/list` | `enterprise:templateField:list` | Template field inventory supports enterprise validation/import behavior. |
| `GET /enterprise/template-field/{id}` | `enterprise:templateField:query` | Template field details support enterprise validation/import behavior. |
| `POST /enterprise/activity-import/sheet-656/validate` | `enterprise:activityImportValidation:validate` | Validate-only import returns enterprise-local row/column validation results and should close with the license gate. |

## Exempt Operations

These endpoints should remain exempt because they are needed to bootstrap, diagnose, or recover enterprise license state itself.

| Endpoint | Permission | Exemption rationale |
| --- | --- | --- |
| `GET /enterprise/license-state/list` | `enterprise:licenseState:list` | Needed for operator audit/recovery workflows when multiple imported states must be reviewed. |
| `GET /enterprise/license-state/current` | `enterprise:licenseState:query` | Needed so enterprise UI and operators can inspect the stored current license state while investigating a denial. |
| `GET /enterprise/license-state/{id}` | `enterprise:licenseState:query` | Needed for audit/troubleshooting of persisted license-state history during recovery. |
| `POST /enterprise/license-import/import` | `enterprise:licenseImport:import` | Must remain available during `DENY` so operators can import a renewed or repaired license. Gating this endpoint would block the primary recovery path. |
| `GET /enterprise/license-gate/current` | `enterprise:licenseState:query` | This is the read-only diagnostic surface for the gate decision itself and must stay callable so the UI can inspect and explain `DENY` reasons. |

## Shared Login Note

`POST /auth/login` in `ruoyi-admin` is a shared platform entrypoint, not an enterprise business controller.

For EB-1 documentation purposes:

- it is **not** listed as an enterprise-owned protected endpoint
- it is documented only as an out-of-scope reference if product policy later requires login-time enterprise gate enforcement
- any future login hook must still consume `ICeLicenseGateService` or stored enterprise state rather than duplicating signature verification

Reason for not classifying it now:

- the current task scope says enterprise backend owns local runtime, validation, and report gates only
- `AuthController` is shared application login infrastructure, not an enterprise business route under `ruoyi-modules/carbon-enterprise`
- EB-2 must not modify `ruoyi-admin` unless a separate task explicitly expands scope
- documenting it as a direct EB-2 gate point would broaden scope beyond the current enterprise module

## EB-2 Enforcement Order

For the next implementation task, the smallest safe rollout is:

1. Keep all exempt operations open.
2. Gate all protected `enterprise/**` endpoints listed above through one reusable enforcement path.
3. Leave `ruoyi-admin`, including `POST /auth/login`, untouched unless a separate task explicitly expands scope into `ruoyi-admin`.

This keeps the change thin, local to enterprise-owned business operations, and rollback-friendly.
