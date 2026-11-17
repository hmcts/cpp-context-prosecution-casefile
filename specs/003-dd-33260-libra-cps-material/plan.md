# Implementation Plan: Record a Migrated Libra Case so CPS Material is Accepted (DD-33260)

**Branch**: `DD-33260-libra-cps-material` (off `team/libra4 @ a0327d45`) | **Date**: 2026-11-17 | **Jira**: DD-33260 (parent DD-33029), AC1
**Research**: [research.md](research.md) | **Tests**: [test-spec.md](test-spec.md)
**Line numbers**: at this branch's head unless marked "(base `a0327d45`)".

## Summary

CPS material for a migrated Libra case is parked as `MaterialPending` with a `DOCUMENT_NOT_MATCHED` review, because PCF never accepts the case: `handleProsecutionCaseCreated` skips every migrated case, and a Libra case that did not come through PCF's intake has no state in PCF. This change makes PCF record such a **Libra** case when progression announces it:

`public.progression.prosecution-case-created` (LIBRA) → **new command** `prosecutioncasefile.command.record-migrated-case` → new aggregate method → **new private event** `prosecutioncasefile.events.migrated-case-recorded` (sets received + accepted, `CC`, case reference, defendants). From then on, every CPS material for the case is validated and added instead of parked as pending.

After that, `add-cps-material` takes the existing accepted path (`MaterialAdded` → material → progression → CDES). **No public event, no listener, no viewstore change, no matching-rule change.** Non-Libra cases are unchanged.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: CPP `service-parent-pom:17.104.0`, CDI; JUnit 5 + Mockito; framework IT harness
**Storage**: `DS.eventstore` (new event on the case stream); `DS.prosecutioncasefile` unchanged
**Constraints**: PCF only — no change to progression, staging-prosecutors, stagingdlrm or PCF DLRM
**Scope**: happy path (AC1). Ejected cases out of scope.

### Three-layer discipline (Constitution Principle II)

| Layer | Touched? | Reasoning |
|-------|----------|-----------|
| 1. Command side | **YES** | New command + RAML + schema, new `@Handles` handler, new aggregate method, new domain event + schema, new mutator entry. |
| 2. Event listener (→ viewstore) | **NO** | `migrated-case-recorded` is private; like `case-referred-to-court-recorded` it has no listener subscription. `material-added` has no listener either. |
| 3. Event processor | **YES (inbound only)** | `ProgressionPublicEventProcessor.handleProsecutionCaseCreated` sends the new command for LIBRA. No processor subscribes to the new event; no new public event. Existing `material-added` processing unchanged. |

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Contract First | ✅ DONE | Command schema + example + RAML, event schema and value schema written before the Java. |
| II. Three-Layer | ✅ PASS | See table. |
| III. Framework idioms | ✅ PASS | `@Handles`, `Sender`, aggregate `apply()`, mutator dispatcher. |
| IV. Build loop | ✅ DONE | `code-reviewer`, `qa` and `spec-validator` run; findings within this change fixed (see test-spec A8, A9, C5). |
| V. Standards | ✅ PASS | Java 17, explicit imports, SLF4J. |
| VI. Schema-subscription symmetry | ✅ PASS | New event has a schema and no subscription (private, same as `case-referred-to-court-recorded`). New command: RAML entry + schema. Inbound subscription already exists (L174). |
| VII. No System.out | ✅ PASS | |
| VIII. TDD | ✅ DONE | Each test seen failing before its production code (P1, P3, C1–C4, H1, A1, A9); A2–A8 and C5 mutation-checked against the code they guard. |

## Design

### 1. Processor — `ProgressionPublicEventProcessor.handleProsecutionCaseCreated` (L85-116; base `a0327d45` L79-102)

| Case | Today | After |
|---|---|---|
| no `migrationSourceSystem` | `accept-case` | unchanged |
| `migrationSourceSystemName` = `LIBRA` | nothing | `record-migrated-case` |
| any other migration source (e.g. XHIBIT) | nothing | unchanged (nothing) |

Command built from `prosecutionCase` (mapping in a converter class per technical rules, not inline):

| Command field | Source | Notes |
|---|---|---|
| `caseId` | `id` | required |
| `prosecutionCaseReference` | `prosecutionCaseIdentifier.caseURN`, else `.prosecutionAuthorityReference` | schema `oneOf` guarantees one |
| `migrationSourceSystemName`, `migrationSourceSystemCaseIdentifier` | `migrationSourceSystem` | audit |
| `defendants[].id` | `defendants[].id` | required |
| `defendants[].prosecutorDefendantReference` | `defendants[].prosecutionAuthorityReference` | **only if present** |
| `defendants[].asn` | `defendants[].personDefendant.arrestSummonsNumber` | **only if present** |
| `defendants[]` name / `organisationName` | `personDefendant.personDetails` / `legalEntityDefendant.organisation.name` | for `MaterialAdded.defendantName` |

**Decision:** Libra match is exact `"LIBRA"`, as in `ProsecutionCaseFile.isMigratedLibraProsecution` (L842-847). The aggregate's `LIBRA_MIGRATION_SOURCE_SYSTEM` is private to the domain module, so the processor declares its own `LIBRA_MIGRATION_SOURCE_SYSTEM` (`ProgressionPublicEventProcessor.java:69`).

### 2. Contracts (before Java)

- `prosecutioncasefile-command-handler/src/raml/prosecutioncasefile-command-handler.messaging.raml` — new `record-migrated-case` entry (next to `accept-case`, L32).
- `.../src/raml/json/schema/prosecutioncasefile.command.record-migrated-case.json` + example `.../src/raml/json/prosecutioncasefile.command.record-migrated-case.json`.
- `prosecutioncasefile-domain-event/src/main/resources/json/schema/prosecutioncasefile.events.migrated-case-recorded.json`.
- `prosecutioncasefile-domain-value-schema/src/main/resources/json/schema/migrated-case-defendant.json`: the defendant shape, shared by the command and the event.
- `prosecutioncasefile-domain-event-processor/pom.xml`: add `prosecutioncasefile.command.record-migrated-case.json` to the `pojo-generation-plugin` includes, next to `accept-case`. The processor's command classes are generated from this list.
- **No** `subscriptions-descriptor.yaml` change; **no** `event-sources.yaml` change.

### 3. Handler

New class `RecordMigratedCaseHandler` (`@ServiceComponent(COMMAND_HANDLER)`, extends `BaseProsecutionCaseFileHandler`, `@Handles("prosecutioncasefile.command.record-migrated-case")`), in the style of `RecordReferProsecutionCasesToCourtAcceptedHandler`. Loads the case aggregate, calls `recordMigratedCase`, appends the events. Does **not** run the PET/BCM part of `AcceptCaseHandler` (research open question 3).

### 4. Aggregate — new method on `ProsecutionCaseFile`

`recordMigratedCase` (L1554):
- **Guard**: if the case is already received or accepted in PCF → no events. This makes it idempotent on redelivery and leaves a case PCF already holds unchanged — including a Libra case submitted via MCC (spec 002), which is already received through `CcCaseReceived` (test A8, IT3).
- Otherwise: apply `MigratedCaseRecorded`.
- No pending-material release. Each later `add-cps-material` goes through the existing accepted branch of `addCpsMaterial` (L1030).

### 5. Mutator — `when(MigratedCaseRecorded.class)` (L1322), next to `CaseReferredToCourtRecorded` (L1316)

Sets `caseId`, `caseType = CC`, `prosecutionReceived = true`, `prosecutionAccepted = true`, `prosecutorCaseReference`, `defendants`. Defendants are built by `toDefendant` (L1570) with the PCF `Defendant` builder: id, reference/ASN if present, name or organisation name. Person defendants also get an empty `selfDefinedInformation`, so CMS IDPC matching's date-of-birth tie-break finds no match instead of failing (test A9). Pending lists are not touched.

`serialVersionUID` is bumped, so existing `ProsecutionCaseFile` snapshots are discarded and rebuilt from the event stream.

### 6. Unchanged (existing behaviour relied on)

`AddMaterialHandler.addCpsMaterial`, `CCDocumentDefendantLevelValidationRule` (reference → ASN → `DEFENDANT_ID_INVALID`), `CaseEjectValidationRule`, `MaterialEventProcessor`, ejection (`case-or-application-ejected`) and defendant-change subscriptions.

## Source touched

```text
prosecutioncasefile-command/prosecutioncasefile-command-handler/
├── src/raml/prosecutioncasefile-command-handler.messaging.raml               ← record-migrated-case entry
├── src/raml/json/schema/prosecutioncasefile.command.record-migrated-case.json (new)
├── src/raml/json/prosecutioncasefile.command.record-migrated-case.json        (new, example)
├── src/main/java/.../command/handler/RecordMigratedCaseHandler.java           (new)
├── src/test/java/.../command/handler/RecordMigratedCaseHandlerTest.java       (new) ← H1
└── src/test/resources/json/{recordMigratedCase,migratedCaseRecorded}.json     (new)
prosecutioncasefile-domain/prosecutioncasefile-domain-event/
└── src/main/resources/json/schema/prosecutioncasefile.events.migrated-case-recorded.json (new)
prosecutioncasefile-domain/prosecutioncasefile-domain-value-schema/
└── src/main/resources/json/schema/migrated-case-defendant.json                (new)
prosecutioncasefile-domain/prosecutioncasefile-domain-event-processor/
└── pom.xml                                                                    ← POJO include for the command
prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/
├── src/main/java/.../aggregate/ProsecutionCaseFile.java                       ← method, mutator, toDefendant, serialVersionUID
└── src/test/java/uk/gov/moj/cpp/prosecution/casefile/aggregate/ProsecutionCaseFileMigratedCaseTest.java (new) ← A1–A9
prosecutioncasefile-event/prosecutioncasefile-event-processor/
├── src/main/java/.../event/processor/ProgressionPublicEventProcessor.java      ← LIBRA branch
├── src/main/java/.../event/processor/converter/ProsecutionCaseToRecordMigratedCaseConverter.java (new)
├── src/test/java/.../event/processor/ProgressionPublicEventProcessorTest.java  ← P1, P3
└── src/test/java/.../event/processor/converter/ProsecutionCaseToRecordMigratedCaseConverterTest.java (new) ← C1–C5
prosecutioncasefile-integration-test/
├── src/test/java/.../helper/EventSelector.java                                ← EVENT_SELECTOR_MIGRATED_CASE_RECORDED
├── src/test/java/.../it/RecordMigratedCaseIT.java                             (new) ← IT1–IT3
└── src/test/resources/stub-data/public.progression.prosecution-case-created-libra.json (new)
```

## Risks

- **Event is permanent**: a new event type on the case stream cannot be removed later — name and fields must be agreed with `@hmcts/cpp-development` before merge.
- **Non-owner repo**: PCF is owned by `@hmcts/cpp-development`; design review before coding.

## Out of scope

Ejected cases; APIM filters; PET/BCM for Libra; other migration sources (XHIBIT); accepting Libra cases submitted via MCC (they keep today's behaviour, proven by A8 and IT3); any change outside PCF.

## Complexity Tracking

No constitution violations — table omitted.
