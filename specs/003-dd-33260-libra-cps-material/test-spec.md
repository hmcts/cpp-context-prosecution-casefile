# Test Spec: Record a Migrated Libra Case for CPS Material (DD-33260)

**Plan**: [plan.md](plan.md) · **Shape**: unit tests cover every row below; ITs prove the end-to-end flow (≥1 IT for the new command); write nothing that already exists.

TDD order: write each unit test first and watch it fail on an assertion (not a compile error), then write the production code. Contracts (schemas, RAML) come before the tests that need the generated classes.

## Unit — processor (`ProgressionPublicEventProcessorTest`)

| # | Test | Setup | Expect |
|---|------|-------|--------|
| **P1** | LIBRA case → record command | `migrationSourceSystemName = "LIBRA"`, `caseURN`, 2 defendants | `record-migrated-case` sent with caseId, reference, defendants; **no** `accept-case` |
| ~~P2~~ | other migration source → nothing | already exists: `shouldHandleCaseCreatedPublicEventIfPayloadHasMigrationSystem` (XHIBIT); containment, not rewritten | nothing sent |
| **P3** | reference falls back | LIBRA, `prosecutionAuthorityReference` only (no `caseURN`) | command reference = `prosecutionAuthorityReference` |

Existing non-migrated test(s) for `accept-case` stay as containment.

## Unit — converter

| # | Test | Expect |
|---|------|--------|
| **C1** | defendant with reference **and** ASN | both mapped |
| **C2** | defendant with ASN only | ASN mapped, no reference |
| **C3** | defendant with reference only | reference mapped, no ASN |
| **C4** | organisation defendant | `organisationName` mapped |
| **C5** | `migrationSourceSystem` without `migrationSourceSystemCaseIdentifier` (optional in core-domain) | command built, identifier absent |

## Unit — handler

| # | Test | Expect |
|---|------|--------|
| **H1** | `record-migrated-case` handled | aggregate method called; its events appended to the case stream |

## Unit — aggregate (`ProsecutionCaseFile`)

| # | Test | Setup | Expect | AC |
|---|------|-------|--------|----|
| **A1** | record a fresh Libra case | empty aggregate | `MigratedCaseRecorded` emitted | AC1 |
| **A2** | case-level CPS material after record | A1, then `addCpsMaterial` (case-level doc) | `MaterialAdded`; **no** `MaterialPending`, **no** `DOCUMENT_NOT_MATCHED` review | AC1 |
| **A3** | defendant-level, matched by reference | A1 with reference, `prosecutorDefendantId` = reference | `MaterialAdded` with that defendant's id + name | AC1 |
| **A4** | defendant-level, matched by ASN (Libra defendant without the optional reference) | A1 with ASN only, `prosecutorDefendantId` = ASN | `MaterialAdded` with that defendant's id | AC1 |
| **A5** | defendant-level, no match | `prosecutorDefendantId` matches neither | `MaterialRejected` `DEFENDANT_ID_INVALID` + review (same as any CP case) | — |
| **A6** | second CPS material after record | A1, then two `addCpsMaterial` calls | each emits its own `MaterialAdded`; no `MaterialPending` | AC1 |
| **A7** | idempotent | record twice, or record on a case already received in PCF (`CaseReferredToCourtRecorded` applied) | second call emits nothing | — |
| **A8** | Libra case already received via MCC (spec 002) | `CcCaseReceived` (channel MCC, `migrationSourceSystemName` LIBRA), then record | no events, no exception; still received, not accepted; MCC defendants unchanged (proves the MCC route is unaffected) | — |
| **A9** | CMS IDPC on a recorded Libra case with two defendants of the same first name and surname | A1 with two "Jane Doe" defendants, then `addIdpcCaseMaterial` | `IdpcDefendantMatchPending` (unmatched), no exception — recorded person defendants carry an empty `selfDefinedInformation`, so the date-of-birth tie-break finds no match instead of a NullPointerException | — |

## IT (new class `RecordMigratedCaseIT`)

Scope of this change: Progression's LIBRA `public.progression.prosecution-case-created` → PCF raises `prosecutioncasefile.events.migrated-case-recorded`. IT1 proves exactly that; IT2 then proves CPS material is added for the recorded case, reusing the existing CPS material helper and fixture. `AddMaterialIT` is not touched.

Fixture `stub-data/public.progression.prosecution-case-created-libra.json`: copy of `public.progression.prosecution-case-created.json` plus a `migrationSourceSystem` block (`LIBRA`, legacy case identifier); `caseURN` instead of `prosecutionAuthorityReference`; defendant with ASN and no `prosecutionAuthorityReference`. Placeholders: case id, URN, defendant id, ASN. Synthetic data; no existing fixture edited.

| # | Test | Expect |
|---|------|--------|
| **IT1** | send Libra `prosecution-case-created` | `migrated-case-recorded` with caseId, reference (URN), `LIBRA`, legacy case identifier, defendant id, ASN, first/last name |
| **IT2** | IT1, then the `add-cps-material` command (the command staging-prosecutors sends for CPS material; defendant-level `SJPN`, `prosecutorDefendantId` = ASN), via existing `AddMaterialHelper` | `material-added` for the case with that Libra defendant's id, `CC`, "Defendant level"; material upload called |
| **IT3** | Libra case submitted via MCC (as spec 002's IT), then Progression's Libra `prosecution-case-created`, then `add-cps-material` | `material-pending` for the case — same as before this change (MCC Libra route unaffected) |

## Containment (existing, run GREEN, do not rewrite)

- `ProgressionPublicEventProcessorTest` — existing non-migrated `accept-case` test(s).
- `AddMaterialIT.shouldCreateMaterialPendingEventWhenAddMaterialForCpsCaseCalled`, `.shouldCreateDocumentReviewRequiredWhenAddMaterialForCpsCaseCalled` (unaccepted case still pends).
- `AddMaterialIT.shouldCreateMaterialRejectedEventForCPSForCCDefendantLevelMaterialWithMissingProsecutorDefendantId`, `.shouldCreateCPSMaterialRejectedEventForCCDefendantLevelMaterialWithInvalidProsecutorDefendantId` (matching rules unchanged).
- `CpsServeMaterialIT`, `InitiateCCProsecutionIT`, `SubmitCCApplicationIT` (all send the non-migrated `prosecution-case-created`).

## Commands

```bash
mvn -pl prosecutioncasefile-event/prosecutioncasefile-event-processor test -Dtest='ProgressionPublicEventProcessorTest,ProsecutionCaseToRecordMigratedCaseConverterTest'
mvn -pl prosecutioncasefile-command/prosecutioncasefile-command-handler test -Dtest=RecordMigratedCaseHandlerTest
mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate test -Dtest=ProsecutionCaseFileMigratedCaseTest
mvn clean install
./runIntegrationTests.sh            # cpp-developers-docker on java-17
mvn -pl prosecutioncasefile-integration-test test -Dit.test=RecordMigratedCaseIT
```
