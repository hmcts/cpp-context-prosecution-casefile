# Implementation Plan: Migrated Libra Summons Creates the Case Directly (ST-001)

**Branch**: `DD-43418-libra-summons-direct-create` (off `team/libra1`) | **Date**: 2026-09-21 | **Spec**: ST-001 (`cp-meta-arch` KB `changes/2026-09-dlrm-mcc-libra-direct-create-backend/stories/st-001-libra-summons-direct-create.md`)
**Work order**: `wo-cpp-context-prosecution-casefile-03` | **Jira**: DD-43418 (epic DD-39576)
**Baseline (read reference)**: `cpp-context-prosecution-casefile@v17.0.95 (acc03ef458)` — a valid ancestor of the delivery branch; all line numbers re-read in situ at `team/libra1 @ 9fd98b13` (17.0.101-LIBRA1-SNAPSHOT).

## Summary

A Libra summons case that a DLRM operator migrates by hand is one Libra already granted, issued and served — so it must become a live case on submission rather than being parked as a box-work summons application for approval. Today `processWithoutProblems()` parks **every** initiation-code `S` case before any channel/marker test. This change adds one named predicate — `isMigratedLibraProsecution` (channel `MCC` **and** `migrationSourceSystem.migrationSourceSystemName` **exactly** `"LIBRA"`) — and uses it in one place (the park guard) so a migrated Libra summons falls through to the existing `ccCaseReceived` / `ccCaseReceivedWithWarnings` branch; the existing duplicate-URN guard (via `prosecutionReceived`, L551) keeps working unchanged. **No new command, no new event, no new schema, no core-domain change, no `.drl`/`RuleConstants` change, no runtime switch.** The change ships inert until the co-released frontend normalises `'Libra'` → `'LIBRA'`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: CPP `service-parent-pom:17.103.x`, CDI; JUnit 5 + Mockito (unit); framework IT harness (`./runIntegrationTests.sh`)
**Storage**: `DS.eventstore` and `DS.prosecutioncasefile` — no changes to either
**Target Platform**: WildFly/JEE via Docker (WAR)
**Project Type**: Event-sourced CQRS microservice (CPP bounded context)
**Constraints**: Aggregate-only change; single file `ProsecutionCaseFile.java`; no new injection, event, schema or DB change
**Scale/Scope**: One new private predicate + two call-site guards in `ProsecutionCaseFile.java`; 3 new unit tests; 2 new ITs

### Three-layer discipline (Constitution Principle II)

| Layer | Touched? | Reasoning |
|-------|----------|-----------|
| 1. Command side (handler → aggregate → domain event) | **YES** | The only layer changed. The aggregate emits `ccCaseReceived` / `ccCaseReceivedWithWarnings` instead of `defendantsParkedForSummonsApplicationApproval` for the migrated Libra summons population. These are **existing** events already emitted by the CC branch and by `approveCaseDefendants` on SA approval. |
| 2. Event listener (→ viewstore) | **NO** | No new event type. `ccCaseReceived(-WithWarnings)` already have listeners and schemas. |
| 3. Event processor (→ public events) | **NO** | No new event type. `progression.initiate-court-proceedings` already flows from `ccCaseReceived` via the existing converters (AC-FR-007 is an assertion, not a build). |

Because no event type is added, removed or reshaped, `subscriptions-descriptor.yaml` and the JSON schema tree are **unchanged** (Constitution Principle VI holds vacuously). This is called out explicitly per the three-layer rule.

## Constitution Check

*GATE: must pass before implementation; re-check after.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Contract First | ✅ PASS | No new command/event/schema. Existing `CcCaseReceived`, `CcCaseReceivedWithWarnings`, `DUPLICATED_PROSECUTION` reused. |
| II. CQRS Three-Layer Discipline | ✅ PASS | Command side only; see table above. Listener/processor explicitly out of scope with reasoning. |
| III. CPP Framework Idioms | ✅ PASS | No hand-rolled JMS/JDBC/ObjectMapper. Aggregate `apply()` mechanism preserved. |
| IV. Spec-Driven Build Loop | ⏳ IN PROGRESS | Plan (this doc) + test-spec before code; `code-reviewer`, `qa`, `spec-validator` must PASS before ship (gate 6). |
| V. HMCTS CPP Standards | ✅ PASS | Java 17, explicit imports, SLF4J. |
| VI. Schema-Subscription Symmetry | ✅ PASS | No event/schema change. |
| VII. No System.out | ✅ PASS | No new logging. |
| VIII. TDD | ⏳ PLANNED | U1/U2/U3 authored failing first, then production code; ITs after. |

## The three edits (single file: `ProsecutionCaseFile.java`)

> Line numbers are in-situ at `team/libra1 @ 9fd98b13`; re-verify before editing.

### Edit 3 — the predicate (used by Edit 1)

Add one **new, separate** private predicate (no code comment — the user has ruled out comments; the rationale lives here and in the KB story):

```java
private boolean isMigratedLibraProsecution(final Prosecution prosecution) {
    return MCC.equals(prosecution.getChannel())
            && Optional.ofNullable(prosecution.getMigrationSourceSystem())
                    .map(MigrationSourceSystem::getMigrationSourceSystemName)
                    .filter(LIBRA_MIGRATION_SOURCE_SYSTEM::equals)
                    .isPresent();
}
```

- **Exact match** on `"LIBRA"` (`::equals`, not `equalsIgnoreCase`) is deliberate: it is the co-release inertness property (ADR-002). Until the frontend normalises `'Libra'` → `'LIBRA'`, this predicate matches nothing and the direct-create path stays dormant.
- Reuses the existing constant `LIBRA_MIGRATION_SOURCE_SYSTEM` (`"LIBRA"`, L202) — one `"LIBRA"` literal in the file.
- The MCC channel test is defence-in-depth; the LIBRA marker is the deciding condition. Guarding on MCC alone would break the entire ordinary MCC summons journey (same channel + initiation code).
- **Precedent**: `isStandaloneCaseWithoutHearing` (L542) already branches on the same migration block (on `migrationCaseStatus`).
- **Do NOT touch** `isLibraMigratedCase` (L534-537) or `getCaseValidationRules(...)` (L538).
- **Gate-6 checklist item (replaces the omitted reviewer comment):** the reviewer confirms `isMigratedLibraProsecution` (exact-match) is kept **separate** from the `equalsIgnoreCase` `isLibraMigratedCase`, and that the exact-match semantics are intentional. With no in-code comment (user rule), this guard against a future dev unifying the two lives in the KB story + this plan and is an explicit review-checklist item.

### Edit 1 — skip the park (`processWithoutProblems`, park branch at L787)

```java
if (incomingInitiationCode.equals(SUMMONS_INITIATION_CODE) && !isMigratedLibraProsecution(prosecution)) {
    return apply(builder.add(defendantsParkedForSummonsApplicationApproval()...));
}
```

A migrated Libra `S` case now falls through to the existing `ccCaseReceived` / `ccCaseReceivedWithWarnings` branch (L795-808) — the same events `approveCaseDefendants` emits on SA approval, minus `summonsApprovedOutcome`.

### Edit 2 — the duplicate guard (`noErrorsAndNotSeenBefore`, L504-505) — OMITTED (in-situ finding)

The work order specified a third edit to `noErrorsAndNotSeenBefore` on the premise that once the migrated Libra path stops parking, a resubmitted URN would stop being detected unless the code-`S` arm flips. **In-situ analysis and TDD (U3, executed) show this edit is behaviourally inert and is therefore omitted.** Confirmed and agreed by the control plane (`cp-meta-arch-27`, 2026-09-21) and corroborated against `main @ 3566a98`:

- `noErrorsAndNotSeenBefore` has exactly one consumer — `firstMessageFromSpi` (L508), which is `SPI`-gated. `isMigratedLibraProsecution` requires channel `MCC`, so `firstMessageFromSpi` is always `false` for a migrated Libra case; editing this line changes no branch for the target population.
- The duplicate is caught instead at L551: `(messageFromCppiOrMccOrCivil && prosecutionReceived) || !noDefendantsParkedForSummonsApplicationApproval`. Once the first migrated Libra summons direct-creates, `CcCaseReceived`'s mutator (L1266) sets `prosecutionReceived = true`, so the resubmission is rejected `DUPLICATED_PROSECUTION` on `urn` via the first disjunct — with no dependency on the parked-map arm.

**AC-FR-003 is preserved** (proven by U3, which asserts the first submission direct-creates and the resubmission is rejected). This is a mechanism/coverage finding, not a scope cut. The KB story ST-001 (Scope item 2 + U3 rationale) is corrected accordingly.

**Final production diff: Edit 1 (guard) + Edit 3 (predicate) only.**

## Project Structure

### Documentation (this feature)

```text
specs/002-libra-summons-direct-create/
├── plan.md        # This file
└── test-spec.md   # U1/U2/U3 + IT1/IT2 contracts and the 13 containment tests
```

### Source code touched

```text
prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/
├── src/main/java/uk/gov/moj/cpp/prosecution/casefile/aggregate/
│   └── ProsecutionCaseFile.java                              ← predicate + 2 guards
└── src/test/java/uk/gov/moj/cps/prosecutioncasefile/
    └── ProsecutionCaseFileTest.java                          ← U1, U2, U3 (63 → 66 @Test)

prosecutioncasefile-integration-test/
├── src/test/java/uk/gov/moj/cpp/prosecution/casefile/it/
│   └── InitiateCCProsecutionIT.java                          ← IT1, IT2
└── src/test/resources/command-json/
    ├── (new) ...migrated-libra-mcc-summons-prosecution.json            ← derived from initiate-channel-mcc-summons-prosecution.json + migrationSourceSystem/LIBRA
    └── (new) subsequent_...migrated-libra-mcc-summons-prosecution.json ← derived from the subsequent_ pair + migrationSourceSystem/LIBRA
```

**Structure Decision**: single production file changed (aggregate module). New IT fixtures ADD a `migrationSourceSystem` block with `LIBRA`; no existing fixture is modified.

## Test strategy (see `test-spec.md`)

- **3 new unit tests** in `ProsecutionCaseFileTest` (package `uk.gov.moj.cps.prosecutioncasefile`): **U1** predicate (one parameterised test: `MCC`+`LIBRA`→true; `MCC`+`XHIBIT`, `MCC`+no marker, `SPI`+`LIBRA`→false), **U2** migrated Libra `S` → `ccCaseReceived` and **no** park, **U3** same URN resubmitted → `DUPLICATED_PROSECUTION` on `urn`.
- **2 new ITs** extending `InitiateCCProsecutionIT`: **IT1** happy path (`ccCaseReceived` present, parked-approval absent, `progression.initiate-court-proceedings` carries the LIBRA marker + legacy case id); **IT2** same URN resubmitted → `DUPLICATED_PROSECUTION`.
- **13 existing containment tests** (blast-radius proof — must be executed and named GREEN in gate-7 evidence, **not** rewritten):
  - `ProsecutionCaseFileTest.shouldRaiseDefendantsParkedForSummonsApplicationApprovalEventForSubsequentMessages` (park path)
  - `ProsecutionCaseFileTest.shouldRejectNewSjpProsecutionWithDuplicatedProsecutionWhenExistingUnapprovedSummonsAlreadyExistsForSameUrn` (duplicate-via-parked-map)
  - `InitiateCCProsecutionIT.shouldCreateParkedEventInsteadOfDefendantsAddedEventWhenSummonsInitiationCodeIsGiven` and `.shouldCreateDefendantsAddedEventWhenInitiationCodeIsNotSummons` (park-vs-create pair)
  - all **7** `InitiateSummonsProsecutionIT` tests (MCC/CPPI/SPI resubmission after approval/rejection)
  - the SPI/CPPI families already in `ProsecutionCaseFileTest` (FR-009c: guard unreachable from non-migration channels)

## Risks & co-release

- **No runtime switch** (ADR-002). Containment is entirely the 13 existing tests + the 3 new unit tests + 2 ITs, run on the service pipeline.
- **Co-release**: must NOT merge to `main` without the frontend change (enum `'Libra'` → `'LIBRA'`). Until it lands the guard matches nothing and every backend test passes regardless — an expected intermediate state, not a defect.
- **Two independent PRs onto `team/libra1`, one merge to main.** ST-002 (DD-43416) already merged; disjoint files.
- **Reversibility**: revert-and-redeploy only.

## Complexity Tracking

No constitution violations — table omitted.
