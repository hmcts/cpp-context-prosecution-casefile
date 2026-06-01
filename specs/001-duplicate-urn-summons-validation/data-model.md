# Data Model: Duplicate URN Validation for Summons Application Creation

**Feature**: CIMD-3818 | **Branch**: `CIMD-3818-duplicate-urn-summons-validation`

## Aggregate State (existing, no changes)

### `ProsecutionCaseFile` aggregate

The aggregate is loaded by stream ID = `SystemIdMapperClient.caseId(urn)`. All fields below are already part of the aggregate; this feature adds no new state.

| Field | Type | Set by | Meaning for this feature |
|-------|------|--------|--------------------------|
| `applicationIdToDefendantIdsMap` | `Map<UUID, List<UUID>>` | `DefendantsParkedForSummonsApplicationApproval` mutator | Non-empty → a pending summons application already exists for this URN |
| `prosecutionReceived` | `boolean` | `ProsecutionReceived` / `CcCaseReceived` / etc. mutators | `true` → a fully accepted prosecution already exists |
| `prosecutorCaseReference` | `String` | `ProsecutionReceived` mutator | The URN string; used as the value in the duplicate error |
| `noDefendantsParkedForSummonsApplicationApproval` | (local boolean) | Computed in `receiveCCCase()` as `applicationIdToDefendantIdsMap.isEmpty()` | Guard signal in `receiveCCCase()` |

## State Transitions Relevant to this Feature

```
New aggregate (stream empty)
  │
  ├─ DefendantsParkedForSummonsApplicationApproval applied
  │     applicationIdToDefendantIdsMap = { applicationId → [defendantIds] }
  │     prosecutionReceived = false (unchanged)
  │     ← DUPLICATE GUARD NOW ACTIVE on subsequent submissions to same URN
  │
  └─ ProsecutionReceived / CcCaseReceived applied
        prosecutionReceived = true
        ← DUPLICATE GUARD ALREADY ACTIVE (existing behaviour)
```

## Validation Rule (new behaviour)

### `receiveSjpProsecution()` — updated condition

```
BEFORE:  if (prosecutionReceived)
AFTER:   if (prosecutionReceived || !applicationIdToDefendantIdsMap.isEmpty())
```

### `receiveCCCase()` — updated condition

```
BEFORE:  if (messageFromCppiOrMccOrCivil && prosecutionReceived)
AFTER:   if ((messageFromCppiOrMccOrCivil && prosecutionReceived) || !noDefendantsParkedForSummonsApplicationApproval)
```

Both conditions produce a `DUPLICATED_PROSECUTION` problem with field key `"urn"` and value = the submitted URN, added to `caseProblems`, which causes the aggregate to return a rejection event.

## Entities and Schemas — No Changes

No new JPA entities, no new Liquibase changelogs, no new JSON schemas, no RAML changes. The change is entirely within aggregate validation logic.
