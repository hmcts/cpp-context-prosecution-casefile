# Tasks: Duplicate URN Validation for Summons Application Creation

**Input**: Design documents from `specs/001-duplicate-urn-summons-validation/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

**Status key**: `[x]` = complete, `[ ]` = pending

**Organization**: Tasks grouped by user story to enable independent implementation and validation.

---

## Phase 1: Setup

**Purpose**: Feature branch, spec, and plan artefacts

- [x] T001 Create feature branch `CIMD-3818-duplicate-urn-summons-validation` from main
- [x] T002 Write feature specification in `specs/001-duplicate-urn-summons-validation/spec.md`
- [x] T003 [P] Write implementation plan in `specs/001-duplicate-urn-summons-validation/plan.md`
- [x] T004 [P] Write research findings in `specs/001-duplicate-urn-summons-validation/research.md`
- [x] T005 [P] Write data model in `specs/001-duplicate-urn-summons-validation/data-model.md`
- [x] T006 [P] Write quickstart guide in `specs/001-duplicate-urn-summons-validation/quickstart.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Confirm no contract artefacts, schema, or RAML changes are required before coding  
**⚠️ CRITICAL**: Must complete before any user story work

- [x] T007 Verify `ProblemCode.DUPLICATED_PROSECUTION` exists and is reusable without schema change in `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/main/java/uk/gov/moj/cpp/prosecution/casefile/aggregate/ProsecutionCaseFile.java`
- [x] T008 Confirm `applicationIdToDefendantIdsMap` field is populated by `DefendantsParkedForSummonsApplicationApproval` mutator and available in-memory at validation time
- [x] T009 Confirm no new RAML, JSON schema, `subscriptions-descriptor.yaml`, or `event-sources.yaml` changes are needed (Constitution Principles I + VI)

**Checkpoint**: Foundation confirmed — implementation can begin. ✅

---

## Phase 3: User Story 1 — Block Duplicate URN on New Summons Application (P1) 🎯 MVP

**Goal**: Any new summons application submission is rejected with `DUPLICATED_PROSECUTION` when any existing record (any type, any state) already holds the same URN.

**Independent Test**: Submit a summons application with a URN that already belongs to an existing pending summons → expect `CcProsecutionRejected` event containing a `DUPLICATED_PROSECUTION` problem with `urn` field key. Submit a summons with a URN already held by a fully received prosecution → existing guard fires correctly.

### Tests for User Story 1 (TDD — written before production code)

- [x] T010 [US1] Write failing test `shouldRejectNewSummonsApplicationWithDuplicatedProsecutionWhenExistingUnapprovedSummonsAlreadyExistsForSameUrn` (parameterized over `nonSpiChannels`) in `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/test/java/uk/gov/moj/cps/prosecutioncasefile/ProsecutionCaseFileTest.java` — verify test FAILS before production code change
  > **FR-005 note**: parameterizing over `nonSpiChannels` covers all five Summons sub-types (Application/Complaint, Breach offences, Either Way/Serious, MCA Case, Witness Statement) — all share the same `receiveCCCase()` code path; no per-sub-type test is needed.

### Implementation for User Story 1

- [x] T011 [US1] Extend duplicate guard in `receiveCCCase()` to also fire when `!noDefendantsParkedForSummonsApplicationApproval` in `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/main/java/uk/gov/moj/cpp/prosecution/casefile/aggregate/ProsecutionCaseFile.java`
- [x] T012 [US1] Run `mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate -am test` and confirm T010 now passes

**Checkpoint**: User Story 1 (CC/CPPI/MCC/summons path) is fully functional. ✅

---

## Phase 4: User Story 2 — Block Duplicate URN Across All New Creation Types (P2)

**Goal**: Any new case or application creation (Charge, Crown Court Case, SJP, Requisition, Standalone) is rejected when the submitted URN belongs to an existing pending or approved Summons.

**Independent Test**: Apply `DefendantsParkedForSummonsApplicationApproval` to a fresh aggregate, then call `receiveSjpProsecution()` with the same URN → expect `SjpProsecutionRejected` event with `DUPLICATED_PROSECUTION`. Also call `receiveCCCase()` (non-summons channel) with the same URN → expect `CcProsecutionRejected` with `DUPLICATED_PROSECUTION`.

### Tests for User Story 2 (TDD — written before production code)

- [x] T013 [US2] Write failing test `shouldRejectNewCcCaseWithDuplicatedProsecutionWhenExistingUnapprovedSummonsAlreadyExistsForSameUrn` (parameterized over `nonSpiChannels`) in `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/test/java/uk/gov/moj/cps/prosecutioncasefile/ProsecutionCaseFileTest.java`
- [x] T014 [US2] Write failing test `shouldRejectNewSjpProsecutionWithDuplicatedProsecutionWhenExistingUnapprovedSummonsAlreadyExistsForSameUrn` in `ProsecutionCaseFileTest.java`

### Implementation for User Story 2

- [x] T015 [US2] Extend duplicate guard in `receiveSjpProsecution()` to also fire when `!applicationIdToDefendantIdsMap.isEmpty()` in `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/main/java/uk/gov/moj/cpp/prosecution/casefile/aggregate/ProsecutionCaseFile.java`
- [x] T016 [US2] Run `mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate -am test` and confirm T013, T014 now pass

**Checkpoint**: User Story 2 (SJP path + all CC-path types) fully functional. ✅

---

## Phase 5: User Story 3 — Unique URN Accepted Without Obstruction (P3)

**Goal**: A new summons or case creation with a URN not held by any existing record succeeds without duplicate errors.

**Independent Test**: On a fresh (empty) aggregate, call `receiveCCCase()` with `SUMMONS_INITIATION_CODE` and any URN → expect `DefendantsParkedForSummonsApplicationApproval` event (no rejection). No guard fires when `applicationIdToDefendantIdsMap.isEmpty()`.

### Tests for User Story 3 (TDD — written before production code)

- [x] T017 [US3] Write failing test `shouldSuccessfullyParkDefendantsWhenNoExistingRecordExistsForSummonsUrn` (parameterized over `nonSpiChannels`) in `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/test/java/uk/gov/moj/cps/prosecutioncasefile/ProsecutionCaseFileTest.java`

### Implementation for User Story 3

- [x] T018 [US3] Confirm guard conditions are correctly gated: when `applicationIdToDefendantIdsMap.isEmpty()` and `!prosecutionReceived`, no `DUPLICATED_PROSECUTION` problem is added in either `receiveSjpProsecution()` or `receiveCCCase()`
- [x] T019 [US3] Run `mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate -am test` and confirm T017 passes and no regressions

  > **FR-008 / SC-005 note**: the sequential correction flow (reject → correct URN → succeed) is inherently supported and requires no additional test. A rejected submission writes no event to the stream, so the aggregate for the original URN remains empty. Correcting to a unique URN targets a separate, independent aggregate stream. T017–T019 confirm unique-URN acceptance; no sticky error state exists.

**Checkpoint**: All three user stories functional. ✅ Total test count: 505, 0 failures.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Build loop agents, final validation, PR

- [x] T020 Run `code-reviewer` agent against `ProsecutionCaseFile.java` changes — must return PASS
- [x] T021 [P] Run `qa` agent to verify TDD discipline (failing tests authored before production code) and test coverage — must return PASS
- [x] T022 [P] Run `spec-validator` agent to confirm no RAML/schema/subscription drift — must return COMPLIANT
- [x] T023 Run full build `mvn clean install` from repository root — must be green
- [x] T024 Create pull request from `CIMD-3818-duplicate-urn-summons-validation` into `main` with description stating Principles II, VIII touched and III, I, VI confirmed unaffected

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — complete ✅
- **Foundational (Phase 2)**: No dependencies — complete ✅
- **User Stories (Phase 3–5)**: Depend on Foundational phase — all complete ✅
- **Polish (Phase 6)**: Depends on Phases 3–5 — pending

### User Story Dependencies

- **US1 (P1)**: Independent — changes `receiveCCCase()` only
- **US2 (P2)**: Independent — changes `receiveSjpProsecution()` only (CC path guard covers both US1 and US2 for the CC path; T015 adds the SJP path guard for US2)
- **US3 (P3)**: Independent — no code change; verified by guard being correctly conditional

### Within Each User Story

- Tests written and confirmed failing before production code ✅ (Constitution Principle VIII)
- Guard extension is minimal (single boolean condition added to existing `if` statement)

### Parallel Opportunities

- T003–T006 (spec artefacts): all parallel ✅
- T020–T022 (build loop agents): T021 and T022 can run in parallel; T020 must complete before T023

---

## Parallel Example: Phase 6 (Polish)

```bash
# T021 and T022 can run concurrently:
Agent(qa agent)        → verifies TDD + test results
Agent(spec-validator)  → verifies contract/subscription symmetry

# T020 runs first, then T023:
Agent(code-reviewer)   → code quality check
mvn clean install      → full build confirmation
```

---

## Implementation Strategy

### MVP (User Story 1 Only)

All three user stories are implemented together in a single two-line change; splitting is not meaningful here. The MVP is the complete change.

### Incremental Delivery

The change is atomic — both guards must be present for the feature to be correct. The natural delivery unit is a single PR containing both production changes and all four tests.

---

## Notes

- `[P]` tasks = can run in parallel (different files, no dependencies)
- `[US1/2/3]` label maps task to spec user story for traceability
- All production code changes are in one file: `ProsecutionCaseFile.java`
- All test changes are in one file: `ProsecutionCaseFileTest.java`
- No schema, RAML, Liquibase, listener, or processor changes required
- Phases 3–5 are complete; only Phase 6 (Polish) remains
