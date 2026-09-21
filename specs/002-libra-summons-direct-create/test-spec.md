# Test Spec: Migrated Libra Summons Direct-Create (ST-001 / DD-43418)

**Plan**: [plan.md](plan.md) · **Shape (user, 2026-09-16):** unit tests cover every new code path; ITs prove only the overall flow; write NOTHING that already exists. **3 new unit tests + 2 new ITs.** 13 existing tests are named as containment, not rewritten.

TDD order: author U1/U2/U3 **first**, watch them fail for the right reason (assertion failure, not compilation error), then write the two guards + predicate, then the 2 ITs.

## New unit tests — `ProsecutionCaseFileTest` (`prosecutioncasefile-domain-aggregate`, package `uk.gov.moj.cps.prosecutioncasefile`)

| # | Test | Setup | Expect | AC |
|---|------|-------|--------|----|
| **U1** | `isMigratedLibraProsecution` — **one parameterised test** | rows: (`MCC`,`LIBRA`), (`MCC`,`XHIBIT`), (`MCC`,no marker), (`SPI`,`LIBRA`) | `true`, `false`, `false`, `false` — whole predicate in one test | FR-008a, FR-009a, FR-009c |
| **U2** | migrated Libra `S` through `processWithoutProblems` | fresh aggregate; MCC, initiation code `S`, `migrationSourceSystem.migrationSourceSystemName = "LIBRA"` | `CcCaseReceived` (or `...WithWarnings`) emitted **and NO** `DefendantsParkedForSummonsApplicationApproval` | FR-001, FR-002 |
| **U3** | migrated Libra `S`, then same URN resubmitted | submit once (assert it direct-creates, not parks), then submit the same URN again | first submission emits `CcCaseReceived` and does **not** park; second submission rejected with `DUPLICATED_PROSECUTION` on field `urn` — caught by the `(MCC && prosecutionReceived)` arm at L551 once the case direct-creates (**not** via `noErrorsAndNotSeenBefore`). Executed proof that omitting Edit 2 is safe. | FR-003 |

Notes:
- **U1** must be a single parameterised test (`@ParameterizedTest`), not four methods. The `SPI`+`LIBRA` → false row is what makes AC-FR-009c an *unreachability* assertion, not merely "predicate is false".
- **U2's absence assertion covers AC-FR-002 by construction** — no "Summons To Court" document, no "New summons application - Process" activity and no prosecutor email are all downstream of the park event; asserting them directly would reach into other services.
- `LIBRA` and `migrationSourceSystem` appear in **no** existing test source or fixture — these are genuinely new paths.
- Exact-match semantics: a `MCC`+`Libra` (mixed-case) row would be `false` today; the predicate is case-sensitive by design (ADR-002). Optional extra row, not required by the brief.

## New ITs — extend `InitiateCCProsecutionIT` (`prosecutioncasefile-integration-test`)

Fixtures derive from `command-json/prosecutioncasefile.command.initiate-channel-mcc-summons-prosecution.json` and its `subsequent_` pair by **ADDING** a `migrationSourceSystem` block (`migrationSourceSystemName: "LIBRA"` + a legacy `migrationSourceSystemCaseIdentifier`). Neither base fixture carries one today. No existing fixture is edited.

| # | Test | Expect | AC |
|---|------|--------|----|
| **IT1** | migrated Libra `S`, happy path | `ccCaseReceived` present; `defendants-parked-for-summons-application-approval` **absent**; `progression.initiate-court-proceedings` sent carrying `migrationSourceSystem.migrationSourceSystemName = "LIBRA"` and the legacy case identifier | FR-001, FR-007 |
| **IT2** | migrated Libra `S`, then same URN resubmitted | second submission rejected `DUPLICATED_PROSECUTION` — duplicate guard still works once nothing parks (two submissions ⇒ genuinely integration) | FR-003 |

Do **NOT** add negative-population ITs — migrated XHIBIT `S` and non-migrated `S` still parking are covered by the existing tests below.

## Containment — 13 existing tests (execute + name GREEN in gate-7; do NOT rewrite)

| AC | Test(s) |
|---|---|
| FR-009a (non-migrated `S` still parks) | `ProsecutionCaseFileTest.shouldRaiseDefendantsParkedForSummonsApplicationApprovalEventForSubsequentMessages`; `InitiateCCProsecutionIT.shouldCreateParkedEventInsteadOfDefendantsAddedEventWhenSummonsInitiationCodeIsGiven`; `InitiateCCProsecutionIT.shouldCreateDefendantsAddedEventWhenInitiationCodeIsNotSummons`; the **7** `InitiateSummonsProsecutionIT` tests |
| FR-003 (duplicate detection, non-migrated) | `ProsecutionCaseFileTest.shouldRejectNewSjpProsecutionWithDuplicatedProsecutionWhenExistingUnapprovedSummonsAlreadyExistsForSameUrn`; `InitiateSummonsProsecutionIT.shouldRejectCaseWhenSameDefendantIsReceivedAndSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaMCCChannel` |
| FR-009c (SPI/CPPI/CIVIL unaffected) | the SPI and CPPI families already in `ProsecutionCaseFileTest` (+ U1's `SPI`+`LIBRA` row) |

The 7 `InitiateSummonsProsecutionIT` methods:
`shouldRaiseNewSummonsApplicationForSameDefendantWhenEarlierApplicationWasRejectedForSameCaseReceivedViaCPPIChannel`,
`...ViaMCCChannel`, `...ViaSPIChannel`,
`shouldRejectCaseWhenSameDefendantIsReceivedAndSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaCPPIChannel`,
`...ViaMCCChannel`,
`shouldIgnoreAsDuplicateDefendantWhenSameDefendantIsReceivedAndSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaSPIChannel`,
`shouldInitiateCaseWhenSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaMCCChannel`.

## Commands

```bash
# unit (single new test, fast loop)
mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate test -Dtest=ProsecutionCaseFileTest

# full unit build
mvn clean install

# ITs (Dockerised env up; CPP_DOCKER_DIR set)
./runIntegrationTests.sh
mvn -pl prosecutioncasefile-integration-test test -Dit.test=InitiateCCProsecutionIT
```
