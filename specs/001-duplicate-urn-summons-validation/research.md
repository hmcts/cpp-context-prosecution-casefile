# Research: Duplicate URN Validation for Summons Application Creation

**Feature**: CIMD-3818 | **Branch**: `CIMD-3818-duplicate-urn-summons-validation`

## Question 1: Is a viewstore query needed to check for duplicate URNs?

**Decision**: No viewstore query is needed.  
**Rationale**: The `ProsecutionCaseFile` aggregate's stream ID is deterministically derived from the URN — `SystemIdMapperClient.caseId(urn)` produces the same UUID for the same URN on every call. When a command arrives for URN `X`, the framework loads the aggregate stream for `caseId(X)`. If any prior event exists in that stream (e.g., `DefendantsParkedForSummonsApplicationApproval`), the aggregate is already hydrated with that state. The duplicate check can read directly from in-memory aggregate fields — no database round-trip is required.  
**Alternatives considered**: Querying the viewstore (`DS.prosecutioncasefile`) by `prosecutorCaseReference` — rejected because it adds a read-side dependency to a command-side operation and is unnecessary given the deterministic stream ID.

---

## Question 2: Which aggregate state field signals an existing pending summons?

**Decision**: `applicationIdToDefendantIdsMap`.  
**Rationale**: This `Map<UUID, List<UUID>>` field is populated in the `DefendantsParkedForSummonsApplicationApproval` state mutator and is non-empty whenever a pending summons application has been recorded in the aggregate stream. The field is already computed into a local boolean `noDefendantsParkedForSummonsApplicationApproval = this.applicationIdToDefendantIdsMap.isEmpty()` at the top of `receiveCCCase()`, making it trivially available for the duplicate guard.  
**Alternatives considered**: `prosecutionReceived` — rejected because this flag is explicitly `false` for pending summons (it is only set when a prosecution is fully accepted, not when defendants are parked for approval).

---

## Question 3: Which code paths require the new guard?

**Decision**: Two methods — `receiveSjpProsecution()` and `receiveCCCase()`.  
**Rationale**:
- `receiveSjpProsecution()` handles SJP prosecutions. It already has a guard `if (this.prosecutionReceived)` that it uses to block duplicate regular prosecutions. Summons applications arriving on the SJP path must also be blocked when the aggregate already holds a parked summons.
- `receiveCCCase()` handles CC/CPPI/MCC prosecutions and is also the entry point for summons applications (routed by `SUMMONS_INITIATION_CODE = "S"`). It already has a guard `if (messageFromCppiOrMccOrCivil && prosecutionReceived)`. Summons must be blocked here too.
- No other intake methods (`receiveProsecution()`, `receiveGroupCases()`, `receivePleadOnline()`, etc.) handle summons applications, so they require no changes.  
**Alternatives considered**: Adding a shared private helper method — rejected because the two call sites are structurally different and a shared helper would require awkward parameter passing for no gain in clarity.

---

## Question 4: What error constant/message is used for duplicate URN rejection?

**Decision**: `ProblemCode.DUPLICATED_PROSECUTION` with field key `"urn"` and value = the URN string.  
**Rationale**: This is the existing constant used by all other duplicate URN checks in the codebase. Reusing it ensures the frontend error-display logic (which keyed on this code to show the inline error) works without any frontend change.  
**Alternatives considered**: A new `ProblemCode` constant — rejected; no new semantics are needed and using a new constant would require frontend changes to handle it.

---

## Question 5: Does this change affect the event listener or event processor layers?

**Decision**: No.  
**Rationale**: The duplicate check fires in the aggregate's validation phase and causes a rejection event (`CcProsecutionRejected` or `SjpProsecutionRejected`) to be emitted rather than an acceptance event. These rejection events already exist, already have JSON schemas, and are already subscribed to (or deliberately not subscribed to) by the listener and processor. No new event type is introduced, so the listener and processor layers are entirely unaffected.

---

## Question 6: Are there concurrent submission risks?

**Decision**: Standard framework conflict resolution applies; no special handling needed.  
**Rationale**: The CPP framework's optimistic concurrency control on the event store prevents two commands from writing to the same aggregate stream simultaneously. The second concurrent summons submission receives an optimistic lock failure, which is retried by the framework. On retry, the aggregate will have been hydrated with the first submission's event, and the duplicate guard will fire correctly. This is documented as an edge case in the spec and is resolved by the existing infrastructure.
