# Enterprise License Gate Operation Map

Updated: 2026-06-13

## Scope

This document defines the enterprise-side license gate points inside `enterprise-backend` only.
It covers the current enterprise-owned HTTP surface in:

- `ruoyi-modules/carbon-enterprise`
- `ruoyi-admin` only as an out-of-scope reference boundary for shared login infrastructure

This map is intentionally documentation-only. It does not add code and does not change permissions.

## Gate Rule

When the enterprise license gate returns `DENY`, protected operations must fail by reusing `ICeLicenseGateService` against stored enterprise license state.

Product rule: License authorization limits only vendor data provision and authorized resource downloads. Enterprise-local business data entry, local CRUD, local validation, system management, and login/recovery workflows must continue to work without a valid vendor-provided License.

The implementation for EB-2 must:

- reuse `ICeLicenseGateService`
- avoid `.lic` re-parse or signature verification duplication
- avoid vendor backend packages, data, or cross-repository state
- stay limited to vendor data sync and authorized resource download gates

## EB-2 Install-Id Sourcing Rule

EB-2 must not widen every protected endpoint with a new public `expectedInstallId` request parameter just to call the license gate.

For the thin EB-2 slice:

- gate enforcement should obtain install-id from a server-side seam, provider, or equivalent internal source if one is needed
- the final production install-id provider design may remain deferred to EB-5
- whatever seam is used must still call `ICeLicenseGateService` against stored enterprise state rather than duplicating license verification logic

This document does not require a specific implementation if that provider/seam is not already present.

## Decision Basis

The current controller surface falls into four buckets:

1. Bootstrap and recovery endpoints that are needed to import or inspect license state.
2. Enterprise-local business read/write endpoints and validation/reporting-supporting data, which are not License-gated.
3. Vendor data sync and vendor-provided resource download endpoints, which are License-gated.
4. Shared platform login entry in `ruoyi-admin`, which is documented only as an out-of-scope reference and not as an implementation target.

## Protected Operations

These endpoints are protected because they request vendor-provided data or consume resources that are only available under License authorization.

| Endpoint | Permission | Why protected |
| --- | --- | --- |
| `POST /enterprise/factor-sync/run` | `enterprise:factorSync:run` | Pulls License-scoped factor data from the vendor open API into the enterprise local cache. |
| `POST /enterprise/report-template-sync/run` | `enterprise:reportTemplateSync:run` | Pulls License-scoped vendor report template metadata and files. |
| `GET /enterprise/report-template-file/download/{id}` | `enterprise:reportTemplateFile:download` | Downloads a vendor-provided report template resource that must remain License-controlled. |

## Exempt Enterprise-Local Operations

These endpoints are intentionally not License-gated. They operate on enterprise-local data or support local recovery/administration.

| Endpoint group | Why exempt |
| --- | --- |
| `/enterprise/activity-data/**` | Enterprise-local data entry must not be disabled by vendor License state. |
| `/enterprise/emission-source/**` | Enterprise-local base data and configuration remain local business data. |
| `/enterprise/extension-field/**` and `/enterprise/extension-field-value/**` | Enterprise-local custom field configuration and values remain editable. |
| `/enterprise/factor-cache-version/**` | Cached factor inventory can be inspected locally even when new vendor sync is denied. |
| `/enterprise/green-power-certificate/**` | Enterprise-local green power certificate data remains editable. |
| `/enterprise/factor-confirmation/**` | Enterprise-local factor confirmation workflow remains usable. |
| `/enterprise/intensity-metric/**` | Enterprise-local intensity metric data remains usable. |
| `/enterprise/capture-batch/**`, `/enterprise/capture-row/**`, `/enterprise/capture-cell/**` | Enterprise-local import capture records remain inspectable. |
| `/enterprise/activity-import/**` and `/enterprise/data-validation/**` | Local import and validation must keep returning user-friendly validation results. |
| `/enterprise/report-template-file/list`, `/enterprise/report-template-file/{id}` and local CRUD | Listing local synced/downloaded template metadata is not a vendor resource download. |
| `/enterprise/dimension-record/**` | Vendor-owned dimension data is fetched through vendor open APIs; enterprise-local endpoint remains a local management boundary. |
| `/enterprise/template-version/**`, `/enterprise/template-sheet/**`, `/enterprise/template-field/**` | Local template metadata and validation support data remain local. |

## Exempt Operations

These endpoints remain exempt because they are needed to bootstrap, diagnose, or recover enterprise license state itself.

| Endpoint | Permission | Exemption rationale |
| --- | --- | --- |
| `GET /enterprise/license-state/list` | `enterprise:licenseState:list` | Needed for operator audit/recovery workflows when multiple imported states must be reviewed. |
| `GET /enterprise/license-state/current` | `enterprise:licenseState:query` | Needed so enterprise UI and operators can inspect the stored current license state while investigating a denial. |
| `GET /enterprise/license-state/{id}` | `enterprise:licenseState:query` | Needed for audit/troubleshooting of persisted license-state history during recovery. |
| `POST /enterprise/license-import/import` | `enterprise:licenseImport:import` | Must remain available during `DENY` so operators can import a renewed or repaired license. Gating this endpoint would block the primary recovery path. |
| `GET /enterprise/license-gate/current` | `enterprise:licenseState:query` | This is the read-only diagnostic surface for the gate decision itself and must stay callable so the UI can inspect and explain `DENY` reasons. |

## Shared Login Note

`POST /auth/login` in `ruoyi-admin` is a shared platform entrypoint, not an enterprise business controller.

For documentation purposes:

- it is **not** listed as an enterprise-owned protected endpoint
- it is documented only as an out-of-scope reference
- any future login hook must still consume `ICeLicenseGateService` or stored enterprise state rather than duplicating signature verification

Reason for not classifying it now:

- the current task scope says enterprise backend owns local runtime, validation, and report gates only
- `AuthController` is shared application login infrastructure, not an enterprise business route under `ruoyi-modules/carbon-enterprise`
- EB-2 must not modify `ruoyi-admin` unless a separate task explicitly expands scope
- documenting it as a direct EB-2 gate point would broaden scope beyond the current enterprise module

## Enforcement Order

The implementation order is:

1. Keep all exempt operations open.
2. Gate only the protected vendor data sync and resource download endpoints listed above through one reusable enforcement path.
3. Leave `ruoyi-admin`, including `POST /auth/login`, untouched unless a separate task explicitly expands scope into `ruoyi-admin`.

This keeps License authorization aligned with the business rule: it controls vendor-provided data and downloads, not enterprise-local operations.
