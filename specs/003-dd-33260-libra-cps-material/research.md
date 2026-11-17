# Research: CPS Material for a Migrated Libra Case (DD-33260)

**Branch**: `DD-33260-libra-cps-material` (off `team/libra4 @ a0327d45`, 17.0.107-LIBRA4-SNAPSHOT) | **Date**: 2026-11-17 | **Jira**: DD-33260 (parent DD-33029)
**Line numbers**: at this branch's head unless marked "(base `a0327d45`)".

## Requirement (Jira AC1 — happy path)

Quoted from DD-33260:

> GIVEN a case material is received (from CPS) via CP retrieve channel, AND the material is received by CP and is matched with a case URN for a migrated case AND this migrated case was not ejected from CP previously, WHEN the material matches with this migrated case URN, THEN CP will process it further and store the material with the case AND the authorised user is able to view the material from Case Material (CDES) UI on CP.

Scope: **happy path only**. Ejected cases are not a happy path and are out of scope.

## How CPS material reaches PCF

1. **staging-prosecutors** receives CP20 XML (`POST /v1/prosecutions/cps-xml/materials`) and converts it (`SubmitCpsMaterialConverter`).
2. **staging-prosecutors** `MaterialSubmittedProcessor` resolves URN → case id (`OU_URN` → `CASE_FILE_ID` system-id mapping) and sends `prosecutioncasefile.add-cps-material` — once for case-level documents, otherwise once per defendant with the ASN as `prosecutorDefendantId`. stagingdlrm writes the same mapping for a Libra case, so the lookup works for Libra.
3. **PCF** `AddMaterialApi` (`AddMaterialApi.java:106`, `prosecutioncasefile.add-cps-material`) → `prosecutioncasefile.command.handler.add-cps-material` → `AddMaterialHandler.addCpsMaterial` (`AddMaterialHandler.java:178`) → `ProsecutionCaseFile.addCpsMaterial` (`ProsecutionCaseFile.java:1030`):
   - `isProsecutionAccepted()` → validate → `MaterialAdded` (or `MaterialRejected` + review).
   - otherwise → `MaterialPending` + `CaseDocumentReviewRequired` with `PROBLEM_CODE_DOCUMENT_NOT_MATCHED` (L1042-1044). **This is what happens to every migrated Libra case before this change.**

## Root cause

Before this change, `ProgressionPublicEventProcessor.handleProsecutionCaseCreated` (base `a0327d45` L80-102) handled `public.progression.prosecution-case-created` and sent `prosecutioncasefile.command.accept-case` **only when `migrationSourceSystem` is absent** (base L88-100). Every migrated case was skipped.

Even if it were not skipped, `acceptCase` (`ProsecutionCaseFile.java:921`) only acts `if (isProsecutionReceived())` (L924). `prosecutionReceived` is set only by PCF's own intake events (`CcCaseReceived`, `SjpProsecutionReceived`, …) and `CaseReferredToCourtRecorded`. So `prosecutionAccepted` never becomes true for a migrated case, and CPS material stays pending.

A Libra case reaches progression by one of two routes:
- **Not through PCF's intake** (migrated by PCF DLRM): PCF's stream for the case is empty. This change records it (`MigratedCaseRecorded`).
- **Through PCF's MCC intake** (spec 002, `isMigratedLibraProsecution`, `ProsecutionCaseFile.java:842-847`): PCF already holds `CcCaseReceived`, so the case is received but not accepted. This change leaves it as it is today (test A8, IT3).

## The incoming event: `public.progression.prosecution-case-created`

- **Producer**: progression `ProgressionEventProcessor.publishProsecutionCaseCreatedEvent` (`team/libra4`, L59-73) republishes `progression.event.prosecution-case-created` unchanged, **only when `groupId` is null**.
- **PCF subscription**: `prosecutioncasefile-event-processor/src/yaml/subscriptions-descriptor.yaml:174`; PCF schema refs progression's, which is `{ prosecutionCase: core/courts/prosecutionCase.json }`.
- **Shape** (core-domain 17.104.6, PCF's pin):
  - `prosecutionCase` required: `id`, `prosecutionCaseIdentifier`, `initiationCode`, `defendants`.
  - `prosecutionCaseIdentifier` has a `oneOf`: `caseURN` **or** `prosecutionAuthorityReference`.
  - `migrationSourceSystem` (optional, all fields optional): `migrationSourceSystemName` (free string), `migrationSourceSystemCaseIdentifier`, `migrationCaseStatus`, `defendantFineAccountNumbers[]`.
  - `defendants[]`: `id`, optional `prosecutionAuthorityReference`, optional `personDefendant.arrestSummonsNumber` (ASN), `personDefendant.personDetails` / `legalEntityDefendant.organisation` (names).
- **Libra marker value**: `"LIBRA"` — PCF DLRM constant (PCF DLRM repo, `team/libra4`: `ProsecutionCaseFileHelper.java:80`, `MigratedCaseFileAggregate.java:115`); PCF already has `LIBRA_MIGRATION_SOURCE_SYSTEM = "LIBRA"` (`ProsecutionCaseFile.java:209`).

## Defendant-level matching (existing rule, unchanged)

CC rejection rules (`CCMaterialsValidationRuleProvider`): `CCDocumentTypeValidationRule`, `CCDocumentDefendantLevelValidationRule`, `CaseEjectValidationRule`.

`CCDocumentDefendantLevelValidationRule` (L48-62), for a defendant-level document type:
1. no `prosecutorDefendantId` → `DEFENDANT_ID_REQUIRED`;
2. match on defendant `prosecutorDefendantReference` (case-insensitive); if none,
3. match on defendant `asn` (case-insensitive); if none → `DEFENDANT_ID_INVALID`.

Case-level documents skip this. So the aggregate needs, per defendant, `id`, `prosecutorDefendantReference` **if present** and `asn` **if present**; matching itself is not changed.

## Why existing events/commands cannot be reused

| Candidate | Why not |
|---|---|
| `accept-case` | Carries only `caseId` + `defendantIds`; `acceptCase` is a no-op without `prosecutionReceived`. |
| `CaseCreatedSuccessfully` | `CaseCreatedEventProcessor` turns it into `public.prosecutioncasefile.prosecution-submission-succeeded`, which staging-prosecutors consumes to mark a submission succeeded — Libra has no submission. No defendants. |
| `CcCaseReceived` | Means "a prosecutor submitted to PCF" — untrue for Libra; needs the full PCF submission payload; has listeners/processors. |
| `CaseReferredToCourtRecorded` | Closest precedent (private, no subscriptions, sets `caseType = CC` and `prosecutionReceived`, L1316-1321) but it does **not** set accepted, case reference or defendants, and means a referral. Used as the pattern, not reused. |

PCF's case state is event-sourced, so the only way for PCF to "know" a Libra case is a new event on that case's stream.

## Downstream after `MaterialAdded` (unchanged, already works)

`MaterialAdded` → `MaterialEventProcessor.handleMaterialAdded` (`MaterialEventProcessor.java:110`; CC branch → `material.command.upload-file`) → `material.material-added` (L459) → `progression.add-court-document` → progression already has the case → visible in CDES. `material-added` has no listener (processor only).

## Open questions (do not block the happy path)

1. ~~Does a Libra case carry `caseURN` or `prosecutionAuthorityReference`, and is it the same URN stagingdlrm maps / CPS sends?~~ **Resolved (user, 2026-11-17):** yes, a Libra case carries the URN, and it is the same URN that stagingdlrm maps and CPS sends. The command takes whichever of `caseURN` / `prosecutionAuthorityReference` is present.
2. ~~Do Libra defendants carry `prosecutionAuthorityReference` and/or ASN?~~ **Resolved (user's dev04 sample + PCF DLRM `team/libra4`, 2026-11-17):** a Libra defendant arrives in PCF DLRM with `prosecutorDefendantId` (the Libra legacy id, required) and `asn`. `prosecutorDefendantReference` is **optional**: it may or may not be present (it is absent in the dev04 sample).
   - PCF DLRM maps `prosecutorDefendantReference` → court `defendant.prosecutionAuthorityReference` (`ProsecutionCaseFileMigratedDefendantToCCDefendantConverter.java:77`), so it reaches `prosecution-case-created` **only when the Libra defendant has one**.
   - It maps `asn` → `personDefendant.arrestSummonsNumber` (`ProsecutionMigrationCaseToCCPersonDefendantConverter.java:43`).
   - The Libra `prosecutorDefendantId` is not carried to progression.
   - Both are recorded when present. The existing rule matches CPS's `prosecutorDefendantId` (CPS sends the ASN) on the reference first, then on the ASN. No match gives the standard `DEFENDANT_ID_INVALID` review.
3. Should the PET/BCM part of `AcceptCaseHandler` (`CpsServeMaterialAggregate.acceptCasePet` / `acceptCaseBcm`) also run for Libra? Proposed: out of scope for AC1.
4. APIM ejected/filtered filters (Jira comment) — outside these repos; dependency, not built here.
