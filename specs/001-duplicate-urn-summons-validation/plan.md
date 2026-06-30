# Implementation Plan: Duplicate URN Validation for Summons Application Creation

**Branch**: `CIMD-3818-duplicate-urn-summons-validation` | **Date**: 2026-05-13 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/001-duplicate-urn-summons-validation/spec.md`

## Summary

The system currently blocks duplicate URN submissions for standard case creation (SJP/CC), but not for summons applications. The fix adds the duplicate URN guard to the two aggregate methods that handle summons intake: `receiveSjpProsecution()` (SJP path) and `receiveCCCase()` (CC/CPPI/MCC path). The implementation leverages an existing aggregate state field (`applicationIdToDefendantIdsMap`), which is already populated whenever a pending summons application exists. No new commands, events, schemas, or database tables are required.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: CPP `service-parent-pom:17.103.x`, CDI (`@ApplicationScoped`, `@Inject`), JUnit 5 + Mockito (unit tests)  
**Storage**: `DS.eventstore` (event store) and `DS.prosecutioncasefile` (viewstore) — no changes to either  
**Testing**: JUnit + Mockito for unit tests; `./runIntegrationTests.sh` for ITs (Dockerised WildFly + Postgres + ActiveMQ)  
**Target Platform**: WildFly/JEE via Docker (WAR deployment)  
**Project Type**: Event-sourced CQRS microservice (CPP bounded context)  
**Performance Goals**: No additional latency — validation uses in-memory aggregate state already loaded by the framework  
**Constraints**: Aggregate-only change; no viewstore query, no new injection, no schema additions  
**Scale/Scope**: Two method-level guard additions in `ProsecutionCaseFile.java`; four new unit test methods

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. RAML / JSON-Schema Contract First | ✅ PASS | No new command, no new event. Existing rejection events (`CcProsecutionRejected`, `SjpProsecutionRejected`) and `ProblemCode.DUPLICATED_PROSECUTION` reused without schema change. |
| II. CQRS Three-Layer Discipline | ✅ PASS | Command side only: aggregate guard logic added. No new event emitted → listener and processor layers unaffected. |
| III. CPP Framework Idioms | ✅ PASS | No hand-rolled JMS / JDBC / ObjectMapper introduced. Existing `@ServiceComponent`, `@Handles`, aggregate `apply()` mechanism preserved. |
| IV. Spec-Driven Build Loop | ✅ PASS | Spec created (`/speckit-specify`), plan in progress (`/speckit-plan`). `code-reviewer`, `qa`, and `spec-validator` agents must all return PASS before merge. |
| V. HMCTS CPP Standards | ✅ PASS | Java 17, Maven multi-module reactor, WildFly WAR packaging, Azure DevOps CI unchanged. |
| VI. Schema-Subscription Symmetry | ✅ PASS | No new event or schema. Existing schemas untouched. |
| VII. No System.out — SLF4J Only | ✅ PASS | No new logging statements of any kind. |
| VIII. Test-Driven Development | ✅ PASS | Four failing tests written first; production code added second. `mvn test` — 505 tests, 0 failures. |

**Gate result**: ALL PASS. No complexity-tracking violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-duplicate-urn-summons-validation/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — architecture investigation
├── data-model.md        # Phase 1 — entity and state analysis
├── quickstart.md        # Phase 1 — how to build/test this change
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
prosecutioncasefile-domain/
└── prosecutioncasefile-domain-aggregate/
    ├── src/main/java/uk/gov/moj/cpp/prosecution/casefile/aggregate/
    │   └── ProsecutionCaseFile.java          ← two guard additions
    └── src/test/java/uk/gov/moj/cps/prosecutioncasefile/
        └── ProsecutionCaseFileTest.java       ← four new test methods
```

**Structure Decision**: Single-module change; only the domain aggregate module is touched. No new modules, no new packages.

## Complexity Tracking

No constitution violations — table omitted per plan template instructions.
