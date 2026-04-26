<!--
SYNC IMPACT REPORT
==================
Version change: (uninitialised template) → 1.0.0
Bump rationale: Initial ratification. All principles and sections are new; no
                prior principles to remove or redefine, so MAJOR is the correct
                starting point (1.0.0).

Modified principles: N/A (initial ratification).

Added sections:
  - Core Principles
      I.    RAML / JSON-Schema Contract First
      II.   CQRS Three-Layer Discipline (Command / Listener / Processor)
      III.  CPP Framework Idioms — No Manual Rolling
      IV.   Spec-Driven Build Loop
      V.    HMCTS CPP Standards Compliance
      VI.   Schema-Subscription Symmetry
      VII.  No System.out / System.err — SLF4J Only
      VIII. Test-Driven Development
  - Technology Stack & Deployment
  - Development Workflow & Quality Gates
  - Governance

Removed sections: None.

Templates requiring updates:
  - .specify/templates/plan-template.md       ✅ compatible — the "Constitution
      Check" block is filled per-feature by `/speckit-plan`. Plan authors MUST
      gate on Principles I–VIII.
  - .specify/templates/spec-template.md       ✅ compatible.
  - .specify/templates/tasks-template.md      ✅ compatible — task ordering
      already encodes "tests before implementation", aligning with VIII.
  - .specify/templates/checklist-template.md  ✅ compatible.
  - README.md / CLAUDE.md / docs/*            ✅ aligned — `.claude/rules/*.md`
      encodes these principles informally; this constitution is now the
      authoritative source.

Follow-up TODOs: None. All placeholders resolved.
-->

# cpp-context-prosecution-casefile Constitution

## Core Principles

### I. RAML / JSON-Schema Contract First (NON-NEGOTIABLE)

The contracts of this service — commands it accepts, queries it answers,
domain events it emits, public events it consumes — are defined in
**RAML files and JSON schemas under `*/src/raml/...` and
`*/src/main/resources/json/schema/...` directories**. Those artefacts are
the source of truth. Java handler signatures, listener mappings, and
processor mappings MUST follow the contracts; the contracts MUST NOT be
inferred from the Java code.

For every command/event change you MUST update:

1. The RAML messaging file (`*-handler.messaging.raml` for commands,
   `subscriptions-descriptor.yaml` for listener / processor subscriptions).
2. The matching JSON schema under `src/raml/json/schema/` (or
   `src/main/resources/json/schema/`).
3. The `event-sources.yaml` if a new internal/public topic is involved.
4. Then — and only then — the Java handler / listener / processor.

**Rationale**: the CPP framework dispatches commands and events by
matching the RAML contract against handler annotations. A drift between
the RAML/schema and the Java code produces a runtime 500 (no matching
schema) or, worse, silent message-loss with no logging. The contract
files are also consumed by upstream/downstream contexts; treating them
as documentation rather than source-of-truth produces cross-context
incidents.

### II. CQRS Three-Layer Discipline (NON-NEGOTIABLE)

Every change touching events MUST be reasoned about across **all three
layers**:

```
Command side (handler → aggregate → domain event)
    ↓ writes events to event store
Event listener (projects events → viewstore tables)
    ↓ projects to DS.prosecutioncasefile
Event processor (consumes domain events → emits public events)
    ↓ emits to public.event for other contexts
```

Adding or modifying a domain event WITHOUT updating both the listener and
the processor is a Principle II violation. Plan authors MUST list which
of the three layers a change touches and confirm the other two are
either unaffected (with reasoning) or carry a paired change in the same
PR.

**Rationale**: the read-model in `DS.prosecutioncasefile` and downstream
contexts (progression, courts, defence, sjp, resulting) depend on the
listener and processor staying in lockstep with the command side.
Breaking one without the others produces silent data drift — the
aggregate is correct, the read-model lies, and downstream contexts see
nothing.

### III. CPP Framework Idioms — No Manual Rolling (NON-NEGOTIABLE)

This service is built on `uk.gov.moj.cpp.common:service-parent-pom`. Use
the framework's idioms rather than rolling your own:

- Command handlers: `@ServiceComponent(COMMAND_HANDLER)` + `@Handles(...)`
  on a method taking `Envelope<CommandPayload>`.
- Aggregate state mutation: route through `AggregateStateMutator` /
  `CompositeCaseAggregateStateMutator` (functional-interface dispatcher,
  one mutator per event type composed into a single dispatcher).
- Event listeners: extend the framework's listener bases; map events →
  JPA entities via dedicated converter classes under `converter/`.
- Event processors: extend the framework's processor bases; map domain
  events → public-event schemas via dedicated converter classes.
- Persistence: Liquibase changelogs only — never manual DDL.
- Outbound REST: use the framework's REST client wiring (see
  `prosecutioncasefile-progression-service`, `*-defence-service`).

**Forbidden**: hand-rolled JMS listeners, hand-rolled JDBC, ad-hoc
ObjectMapper instances, manual schema validation. The framework already
solves these and rolling your own diverges from the rest of the CPP
estate.

**Rationale**: every CPP service follows these idioms, so cross-service
maintenance and operability depend on consistency. A bespoke pattern in
one service makes the next maintainer reach for the wrong mental model.

### IV. Spec-Driven Build Loop (NON-NEGOTIABLE)

Every non-trivial change MUST flow through the cycle:

```
Spec → Write → Code Review → QA → Spec-Validate → Fix → Ship
```

The reviewer agents (`code-reviewer`, `qa`, `spec-validator`) report
findings only; they MUST NOT modify code. The primary agent or a human
applies fixes, then re-runs the loop until all three return PASS /
COMPLIANT. The `spec-validator` here checks that RAML and JSON-schema
files are consistent with `subscriptions-descriptor.yaml`,
`event-sources.yaml`, and the Java handler / listener / processor
mappings. Changes exempt from the loop: markdown-only edits, whitespace
or import-only edits, `.claude/rules/*` and `CLAUDE.md` rule updates.

**Rationale**: keeps a human (or primary agent) as the decision point;
prevents conflicting auto-fixes; preserves auditable, reproducible
review output.

### V. HMCTS CPP Standards Compliance (NON-NEGOTIABLE)

- **Build tool**: Maven. Gradle is forbidden in this codebase.
- **Java**: 17. CI pool demands `centos8-j17`. Local builds use the
  CPP-standard `mvn17` alias when system default differs.
- **Parent**: `uk.gov.moj.cpp.common:service-parent-pom:17.103.x` —
  pin updates require a coordinated cross-context check (`coredomain`,
  `progression`, `resulting`, `sjp`, `defence`, `referencedata`,
  `material`, `authorisation.service`, `stream-transformation-tool`).
- **Packaging**: WAR deployed to WildFly via Docker. The `*-service`
  module is the packaging WAR; `src/main/descriptors/resource-descriptor.yml`
  wires datasources / queues / topics / service mapping.
- **Tests**: JUnit + Mockito for unit tests; integration tests in
  `prosecutioncasefile-integration-test` orchestrated by
  `runIntegrationTests.sh` (Docker-based WildFly + Postgres + ActiveMQ +
  WireMock). ITs require `CPP_DOCKER_DIR` pointing at a local checkout
  of `hmcts/cpp-developers-docker`.
- **CI/CD**: Azure DevOps `azure-pipelines.yaml`. PR builds run
  `pipelines/context-verify.yaml` (Sonar + unit tests). `IndividualCI`
  on `main` / `team/*` runs `pipelines/context-validation.yaml`.
- **Quality gate**: SonarQube — coverage, duplication, smells. No local
  Checkstyle / PMD enforcement at build time.

**Rationale**: aligns this service with the rest of the CPP estate
(naming, build, deploy, test, observability conventions) so cross-team
maintenance, on-call rotation, and platform upgrades work uniformly.

### VI. Schema-Subscription Symmetry (NON-NEGOTIABLE)

When you add, remove, or rename a domain or public event you MUST update
**both**:

- The relevant `subscriptions-descriptor.yaml`
  (`prosecutioncasefile-event/prosecutioncasefile-event-listener/...`,
  `prosecutioncasefile-event/prosecutioncasefile-event-processor/...`).
- The matching JSON schema under
  `*/src/main/resources/json/schema/` (or `*/src/raml/json/schema/`).

A subscription without a matching schema produces a runtime 500 on
dispatch. A schema without a subscription is dead code that drifts
silently as the event evolves.

**Rationale**: this is the most common source of incidents on this
service. Encoding it as a NON-NEGOTIABLE principle (rather than a
"common gotcha" in CLAUDE.md) makes it a review-blocker.

### VII. No `System.out` / `System.err` — SLF4J Only (NON-NEGOTIABLE)

Code MUST NOT use `System.out.println`, `System.err.println`, or
`Throwable#printStackTrace()`. All diagnostic output goes through SLF4J
(`org.slf4j.Logger` via `LoggerFactory.getLogger(...)`). This applies
to production code AND tests.

**Rationale**: container logs are aggregated and structured; stdout
prints bypass the framework's MDC (correlation id propagation through
`Envelope`'s metadata) and the platform log shipping. They vanish from
operations and surface as noise in CI.

### VIII. Test-Driven Development (NON-NEGOTIABLE)

Red → Green → Refactor for every behaviour change.

1. Write the failing test first. It MUST run and fail for the *correct*
   reason — the assertion, not a missing class or compilation error.
2. Write the minimum production code to make it pass.
3. Refactor with the test still green.

PRs MUST show that the test was authored at or before the production
code (commit history or paired-commit are both acceptable). The `qa`
reviewer agent gates on this — production code without an accompanying
failing-then-passing test is FAIL.

Exempt: pure mechanical refactors (rename, move, extract with no
behaviour change), formatting, comment-only edits.

**Rationale**: the regression surface of this service is wide
(seven intake channels, three reasoning layers, dozens of converter
classes, cross-context public events). Only fail-first tests catch the
class of bug where a converter silently drops a field or a listener
projects an event into the wrong column.

## Technology Stack & Deployment

- **Java**: 17 (CI demand `centos8-j17`; local `mvn17` alias).
- **Build**: Maven. Multi-module reactor; modules listed in root
  `pom.xml`.
- **Framework**: CPP `service-parent-pom:17.103.x`. JEE-style with
  `@ServiceComponent` / `@Handles` annotations.
- **Packaging**: WAR → WildFly (Docker).
- **Persistence**: Liquibase changelogs (event store, aggregate
  snapshot, viewstore, event buffer).
- **Messaging**: ActiveMQ (Docker for ITs); JMS topics + queues
  declared in `event-sources.yaml` and `resource-descriptor.yml`.
- **Data stores**:
  - `java:/app/prosecutioncasefile-service/DS.eventstore` — event
    store (event-repository-liquibase + aggregate-snapshot-repository-liquibase).
  - `java:/DS.prosecutioncasefile` — viewstore (event-buffer-liquibase
    + `prosecutioncasefile-viewstore-liquibase`).
- **Inbound JMS**:
  - `prosecutioncasefile.handler.command` — command queue.
  - `prosecutioncasefile.event` — internal event topic (replay).
  - `public.event` — shared platform topic (cross-context traffic).
- **Outbound**: `prosecutioncasefile.event` (own domain events);
  `public.event` (public events for downstream contexts).
- **Tests**:
  - Unit: JUnit + Mockito (`mvn test`).
  - Integration: `runIntegrationTests.sh` orchestrates Docker WildFly +
    Postgres + ActiveMQ + WireMock; runs Liquibase, deploys WARs,
    executes `prosecutioncasefile-integration-test/*IT.java`.
- **Logging**: SLF4J + the framework's logger configuration; MDC keys
  carried through `Envelope` metadata.
- **CI/CD**: Azure DevOps via `azure-pipelines.yaml`. PR =
  `pipelines/context-verify.yaml`. Main branch =
  `pipelines/context-validation.yaml` with
  `serviceName=prosecutioncasefile`,
  `itTestFolder=prosecutioncasefile-integration-test`. `dev/release-*`
  branches excluded.
- **Quality gate**: SonarQube — coverage thresholds, duplication, smells
  enforced in CI; no local equivalent at build time.

## Development Workflow & Quality Gates

- **Contract files** (RAML, JSON schemas, `subscriptions-descriptor.yaml`,
  `event-sources.yaml`) MUST be updated **before** the matching Java
  change (Principle I + VI).
- The build loop (Principle IV) repeats until `code-reviewer`, `qa`, and
  `spec-validator` each return PASS / COMPLIANT.
- TDD (Principle VIII) MUST be visible in commit history — the failing
  test commit precedes (or is paired with) the production code that
  satisfies it.
- Every feature built via spec-kit lives under `specs/NNN-slug/` (or
  `specs/<JIRA-ID>-slug/` if Jira-tracked) containing at least
  `spec.md`, `plan.md`, and `tasks.md`. Flow:
  `/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement
  → /speckit-analyze`.
- Required commands run cleanly before merge:
  - `mvn clean install` — full build + unit tests, green.
  - `./runIntegrationTests.sh` — Dockerised IT run, green (when changes
    touch handlers / listeners / processors / converters / schemas).
  - SonarQube quality gate in CI — passing.
- Commit style: Conventional Commits (`feat:`, `fix:`, `chore:`,
  `docs:`, `refactor:`).
- Pull requests: the description MUST state which principle(s) the
  change touches. Any deviation from a principle requires explicit
  written justification in the PR description and MUST be flagged in
  the plan's "Complexity Tracking" section.
- Branch naming: Jira-prefixed (`DD-XXXXX-feature-slug`) — the speckit
  `before_specify` hook auto-creates these via `/speckit-git-feature`.

## Governance

This constitution supersedes the informal conventions in `.claude/rules/`
copied from the HMCTS overlay template. Where this document and those
files disagree, this document wins; the rule files are retained as
quick-reference material and MUST be kept in sync.

**Amendment procedure**:

1. Propose the change in a feature spec under `specs/`.
2. Bump `Version` per semantic versioning:
   - **MAJOR** — a breaking principle change, removal, or redefinition
     that invalidates existing practice.
   - **MINOR** — a new principle, new section, or materially expanded
     guidance.
   - **PATCH** — clarifications, wording, typo fixes, or non-semantic
     refinements.
3. Re-run `/speckit-analyze` on every in-flight feature spec to verify
   it still aligns with the amended principles; update or waive as
   required.

**Compliance expectations**:

- All PRs MUST honour these principles.
- Deviations MUST be explicitly justified in the PR description and,
  where relevant, in the plan's "Complexity Tracking" table.
- Reviewers MUST block merges that silently violate a NON-NEGOTIABLE
  principle without a written waiver.

**Version**: 1.0.0 | **Ratified**: 2026-04-26 | **Last Amended**: 2026-04-26
