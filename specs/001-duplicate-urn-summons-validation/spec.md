# Feature Specification: Duplicate URN Validation for Summons Application Creation

**Feature Branch**: `CIMD-3818-duplicate-urn-summons-validation`
**Created**: 2026-05-12
**Status**: Draft
**Jira**: CIMD-3818 (Epic: CIMD-3746 Stop duplicate URN)

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Block Duplicate URN on New Summons Application (Priority: P1)

As a CP user with permission to create cases manually, when I enter a Prosecution reference number (URN) that already belongs to an existing record in Common Platform, I want the system to immediately reject my submission with a clear inline error on the 'Prosecutor details' screen, so that I am prevented from creating a duplicate case or application before it is ever stored.

**Why this priority**: This is the core guard that prevents duplicate prosecutions from entering the system. All other stories depend on this working correctly first.

**Independent Test**: Can be fully tested by submitting any Summons creation flow with a URN that already exists in CP (as any record type, any state) and confirming the user stays on the 'Prosecutor details' screen with an inline error.

**Acceptance Scenarios**:

1. **Given** a CP user with manual case creation permission, **And** an existing Summons Application (not yet Approved) exists with URN `5SU300426101`, **When** the user selects any Summons type from the creation menu and submits with URN `5SU300426101`, **Then** the submission is rejected, the user remains on the 'Prosecutor details' screen, and an inline error is displayed against the 'Prosecution reference number' field.

2. **Given** a CP user with manual case creation permission, **And** an existing Approved Summons Application exists with URN `5SU300426102`, **When** the user submits a new Summons application with URN `5SU300426102`, **Then** the submission is rejected with the same inline error.

3. **Given** a CP user with manual case creation permission, **And** an existing record of any type (SJP Case, Crown Court Case, Standalone Application, Magistrates Case) exists with URN `56GD9202526` in any state (Active, Inactive, Closed, Completed), **When** the user submits a new Summons application with URN `56GD9202526`, **Then** the submission is rejected with an inline error against the 'Prosecution reference number' field.

4. **Given** the inline error is displayed, **When** the user corrects the URN to a unique value not present in CP, **Then** the submission succeeds and the Summons application is created with a success message shown.

---

### User Story 2 — Block Duplicate URN Across All New Creation Types (Priority: P2)

As a CP user, when I attempt to create **any** new case or application type (not just Summons) using a URN that is already held by an existing record (including an unapproved Summons), the system must reject the submission with an inline error, so that URN uniqueness is enforced platform-wide.

**Why this priority**: The duplicate URN rule must be symmetric — it protects in both directions. A Charge, Crown Court Case, SJP, Requisition, or Standalone Application must all be blocked from reusing a URN already claimed by a pending Summons application.

**Independent Test**: Can be fully tested by submitting a Charge (or Crown Court Case, SJP, Requisition, Standalone Application) creation flow with a URN that belongs to an existing unapproved or approved Summons, and confirming rejection.

**Acceptance Scenarios**:

1. **Given** an existing unapproved Summons Application with URN `5SU300426103`, **When** a user creates a new Charge case with that URN, **Then** the submission is rejected with an inline error (AC5 — scoped to a sibling story but validated here as a QA matrix row).

2. **Given** an existing SJP record with a URN, **When** a user creates a new Requisition or Crown Court Case with that same URN, **Then** the submission is rejected.

3. **Given** an existing Summons with a URN, **When** a user creates a new Standalone Application with that URN, **Then** the submission is rejected.

---

### User Story 3 — Unique URN Accepted Without Obstruction (Priority: P3)

As a CP user, when I enter a URN that does not exist anywhere in Common Platform, I want the system to accept my submission without any duplicate-URN errors, so that valid new cases and applications are not incorrectly blocked.

**Why this priority**: Essential guard against false positives. The validation must only fire for genuine duplicates.

**Independent Test**: Can be fully tested by submitting a Summons or any creation type with a freshly generated unique URN and confirming successful creation.

**Acceptance Scenarios**:

1. **Given** no existing record in CP has URN `<unique-urn>`, **When** a user selects any Summons type and submits with that URN, **Then** the application is created successfully and a success message is shown.

2. **Given** no existing record in CP has URN `<unique-urn>`, **When** a user creates a new case (any type) with that URN, **Then** the case is created successfully.

---

### Edge Cases

- What happens when an existing record is in INACTIVE or CLOSED state — does the URN remain blocked? Yes — the validation applies to all states (Active, Inactive, Closed, Completed, Approved, Unapproved).
- What happens when the user submits without entering a URN (i.e., selects 'No' for 'Do you have a prosecution reference number?')? Duplicate URN validation is not triggered — out of scope for this feature.
- What happens when the same URN is submitted concurrently by two users? The CPP framework's optimistic locking on the event stream guarantees that the second write attempt raises a version conflict and is retried; on retry the duplicate guard in the aggregate fires and returns the rejection event. No application-level concurrency handling is required.
- What happens for each Summons sub-type (Application/Complaint, Breach offences, Either Way/Serious, MCA Case, Witness Statement)? Duplicate validation applies identically to all sub-types.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST reject any new Summons application creation when the submitted Prosecution reference number (URN) already exists in Common Platform, regardless of the existing record's type or state.
- **FR-002**: The system MUST reject any new case or application creation (SJP, Crown Court Case, Charge, Requisition, Standalone Application) when the submitted URN already belongs to an existing **pending or approved Summons** application. Other duplicate URN conflicts between non-Summons creation types are handled by the existing aggregate mechanism and are unaffected by this change.
- **FR-003**: When a duplicate URN is detected, the user MUST remain on the 'Prosecutor details' screen and see an inline error against the 'Prosecution reference number' field with the message: _"Prosecution reference \<URN\> already exists - enter correct reference"_.
- **FR-004**: The system MUST allow a new creation to proceed when the submitted URN is not held by any existing record in the system.
- **FR-005**: The duplicate URN check MUST cover all Summons sub-types: Application/Complaint, Breach offences, Either Way or Serious offences, MCA Case, and Witness Statement. All five sub-types share the same `receiveCCCase()` code path (distinguished by channel via `nonSpiChannels`), so a single parameterized test covers all sub-types without requiring separate per-sub-type cases.
- **FR-006**: The duplicate URN check MUST be applied to all existing record types as the "existing holder": SJP Case, Crown Court Case, Standalone Application, Magistrates Case, and Summons (approved and unapproved).
- **FR-007**: The duplicate URN check MUST be applied regardless of the existing record's lifecycle state (Active, Inactive, Closed, Completed, Approved, Unapproved).
- **FR-008**: After correcting the URN to a unique value, the user MUST be able to resubmit and complete the creation successfully without additional duplicate errors.
- **FR-009**: The error MUST be displayed inline against the specific 'Prosecution reference number' field, not as a generic page-level error.

### Key Entities

- **Prosecution Reference Number (URN)**: The unique identifier entered by a CP user on the 'Prosecutor details' screen. Must be unique across all case and application types in Common Platform, regardless of status.
- **Summons Application**: A manually created application of type Application/Complaint, Breach offences, Either Way/Serious, MCA Case, or Witness Statement. Subject to duplicate URN validation at point of submission.
- **Existing Record**: Any CP record (SJP Case, Crown Court Case, Standalone Application, Magistrates Case, Summons) that already holds a given URN, in any lifecycle state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of new Summons application submissions with a URN matching any existing CP record are rejected before creation, across all Summons sub-types and all existing record types and states (11 matrix combinations).
- **SC-002**: 100% of new case/application submissions of any creation type (Charge, Crown Court Case, SJP, Requisition, Standalone Application) with a URN matching an existing Summons (approved or unapproved) are rejected.
- **SC-003**: When a duplicate URN is submitted, the user remains on the 'Prosecutor details' screen and sees the inline error within the normal page response time (no additional delay introduced by the validation).
- **SC-004**: 100% of Summons creation attempts with a genuinely unique URN (no existing record) succeed without any false-positive duplicate errors.
- **SC-005**: After correcting a duplicate URN to a unique value, users can complete the creation flow successfully on the next attempt without encountering further duplicate errors.

## Assumptions

- The duplicate URN check is applied at the point of submission (when the user clicks 'Submit'), not on field blur or keystroke.
- The validation covers all CP record types that can hold a URN — no new record types are added in scope of this story.
- AC5 (Charge flow blocking on existing Summons URN) is in scope for QA validation in this ticket but is formally scoped to a sibling story (CIMD-3851 FE / CIMD-3850 BE) for implementation tracking.
- The existing duplicate URN mechanism for the standard case creation process (non-Summons) already functions correctly and is not being modified.
- The back-end validation (domain aggregate layer) is the authoritative source of the duplicate check — the front end relies on the error returned from the back end to display the inline message.
- The 'Prosecution reference number' field is only shown when the user selects 'Yes' for 'Do you have a prosecution reference number?' — validation only fires when this field is present and submitted.
- Both ACTIVE and INACTIVE case states are covered by the existing record check; no distinction is made between them for the purposes of blocking duplicate URNs.
- The exact inline error message text ("Prosecution reference \<URN\> already exists - enter correct reference") is rendered by the UI when it receives the `DUPLICATED_PROSECUTION` problem code; the back-end is only responsible for returning this problem code with `urn` as the field key. The message string itself is not part of the back-end contract.
- The CPP aggregate is loaded from the event stream by stream ID (URN) regardless of the logical lifecycle status of the record (Active, Inactive, Closed, Completed). No additional lifecycle-state filter is applied at validation time, so no special handling for non-active states is required.
- Duplicate URN protection for other creation types acting as the existing holder (SJP Case, Crown Court Case, Standalone Application, Magistrates Case) is provided by the `prosecutionReceived` flag in the existing aggregate guard, which is production-verified and is not modified by this change.
- The placement of the inline error against the specific 'Prosecution reference number' field rather than as a page-level error (FR-009) is a UI/front-end responsibility. The back-end's contract is to return `DUPLICATED_PROSECUTION` in the problems array with the `urn` field key; how that is rendered on-screen is determined by the UI.
