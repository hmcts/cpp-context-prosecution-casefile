# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service is

`prosecutioncasefile` is one of the HMCTS CPP bounded contexts. It sits at the **front of the prosecution pipeline**: it ingests prosecution cases (SPI/SJP, Crown Court, group cases, summons applications, online plea, civil fees, CPS-served IDPC material), validates them, persists them as an event-sourced aggregate, and forwards successfully created cases as public events consumed by progression / courts / defence / sjp / resulting.

Built on the CPP framework (`uk.gov.moj.cpp.common:service-parent-pom`), packaged as WARs, deployed to WildFly. Java 17.

## Build, test, run

```bash
# Full build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Build + unit tests
mvn clean install

# Single module (with deps)
mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate -am clean install

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName
```

### Integration tests

The `prosecutioncasefile-integration-test` module is **not** run by `mvn verify`. It needs WildFly + Postgres + ActiveMQ in Docker first:

```bash
./runIntegrationTests.sh
```

Prerequisites:
- `CPP_DOCKER_DIR` env var pointing at a local checkout of `hmcts/cpp-developers-docker`.
- Docker daemon running and authenticated to the `crmdvrepo01` registry.

The script builds WARs → undeploys old → starts containers → runs Liquibase (event log, aggregate snapshot, viewstore, event buffer, activiti, system, event tracking, file service) → deploys WireMock stubs → deploys WARs → healthchecks → runs ITs.

Once the env is up, run a single IT against it:

```bash
mvn -pl prosecutioncasefile-integration-test test -Dit.test=CaseFileQueryIT
```

### Framework JMX commands

```bash
./runSystemCommand.sh           # help
./runSystemCommand.sh --list    # list available commands (CATCHUP, etc.)
./runSystemCommand.sh CATCHUP   # run one
```

Uses `admin/admin` against local WildFly. Downloads `framework-jmx-command-client` on first use.

### CI

Azure DevOps (`azure-pipelines.yaml`):
- PR builds → `pipelines/context-verify.yaml` (Sonar + unit tests).
- `IndividualCI` on `main` / `team/*` → `pipelines/context-validation.yaml` with `serviceName=prosecutioncasefile` and `itTestFolder=prosecutioncasefile-integration-test`.
- `dev/release-*` branches are excluded.
- Agent pool: `MDV-ADO-AGENT-AKS-01`, demand `centos8-j17`.

## Architecture — the three layers you must reason across

Every change touching events needs to be reasoned about across **three layers**. Breaking one without the others produces silent data drift:

1. **Command side** — RAML-declared commands hit `@Handles`-annotated handler classes which load the `ProsecutionCaseFile` aggregate, run validation rules, and apply domain events. State mutation is centralised via `AggregateStateMutator` / `CompositeCaseAggregateStateMutator` (functional-interface dispatcher, one mutator per event type).
2. **Event listener** — projects domain events into the viewstore DB (`DS.prosecutioncasefile`). Lives under `prosecutioncasefile-event/prosecutioncasefile-event-listener`. Heavy use of converters mapping events → JPA entities.
3. **Event processor** — consumes domain events and emits **public** events for downstream contexts (courts, progression, defence, sjp). Lives under `prosecutioncasefile-event/prosecutioncasefile-event-processor`. Heavy use of converters mapping internal events → downstream public schemas.

### Three subscription sources

The processor and listener can be triggered by:

1. **Internal event topic** `prosecutioncasefile.event` — replay of this context's own domain events.
2. **Public event bus** `public.event` — events from other contexts (progression, sjp, resulting, material, stagingprosecutors).
3. **Command queue** `prosecutioncasefile.handler.command` — RAML-declared commands.

### Authoritative routing files (always re-read before reasoning about a flow)

- `prosecutioncasefile-event-sources/src/yaml/event-sources.yaml` — internal + public topic declarations.
- `prosecutioncasefile-event/prosecutioncasefile-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions.
- `prosecutioncasefile-event/prosecutioncasefile-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `prosecutioncasefile-command/prosecutioncasefile-command-handler/src/raml/prosecutioncasefile-command-handler.messaging.raml` — command → handler mapping.
- Per-command/per-event JSON schemas: `src/raml/json/` and `src/raml/json/schema/` under each command/domain-event module.

### Intake channels

Each channel has its own command/event path:

| Channel | Handler | Notes |
|---|---|---|
| SPI / SJP | `SjpProsecutionHandler` | listeners: `ProsecutionReceivedListener`, `SjpProsecutionReceivedListener` |
| Crown Court | `CcProsecutionHandler` | events: `cc-case-received(-with-warnings)` |
| Group prosecutions | `GroupProsecutionHandler` | events: `group-cases-received`, `group-cases-parked-for-approval` |
| Summons applications | `SummonsApplicationHandler`, `SubmitApplicationHandler` | |
| Civil | (no dedicated handler) | driven by progression public events (`civil-case-exists`); listener converters `CaseDetailsToCivilFees`, `ProsecutionReceivedToCase`; civil-specific validation rules under `validation/rules/defendant/offence/*ForCivil*` |
| Online plea / CPS serve / IDPC material | own handlers and listener chains | |

### Data stores

- `java:/app/prosecutioncasefile-service/DS.eventstore` — event store (event-repository-liquibase + aggregate-snapshot-repository-liquibase).
- `java:/DS.prosecutioncasefile` — viewstore (event-buffer-liquibase + `prosecutioncasefile-viewstore-liquibase`).

## Critical gotcha — when adding/removing an event

**Always update both** the relevant `subscriptions-descriptor.yaml` **and** the JSON schema tree under `*/src/main/resources/json/schema/` (or `*/src/raml/json/schema/`). A subscription without a matching schema produces a runtime 500 on dispatch.

## Module layout (high-level)

- `prosecutioncasefile-common`, `prosecutioncasefile-datatypes-common` — shared utils + value objects.
- `prosecutioncasefile-command` → `-command-api` (RAML + schemas), `-command-handler` (`@Handles` handlers).
- `prosecutioncasefile-domain` → `-domain-aggregate`, `-domain-event` (event JSON schemas), `-domain-event-processor`, `-domain-value-schema`.
- `prosecutioncasefile-event` → `-event-listener`, `-event-processor`.
- `prosecutioncasefile-event-sources` — `event-sources.yaml`.
- `prosecutioncasefile-query` → `-query-api` (RAML), `-query-view` (read services over the viewstore).
- `prosecutioncasefile-validation-rules` — cross-cutting validation rule wiring.
- `prosecutioncasefile-viewstore` — Liquibase changelogs for `DS.prosecutioncasefile`.
- `prosecutioncasefile-service` — packaging WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources/queues/topics, service mapping `/prosecutioncasefile-[^/]+`.
- `prosecutioncasefile-progression-service`, `prosecutioncasefile-defence-service` — outbound REST/messaging clients.
- `prosecutioncasefile-healthchecks`, `prosecutioncasefile-refdata` — healthchecks and reference-data client wiring.
- `prosecutioncasefile-integration-test` — `*IT.java` orchestrated by `runIntegrationTests.sh`.

## Key version pins (in `pom.xml`)

Parent `service-parent-pom:17.103.3`. Current artifact `17.0.89-SNAPSHOT`. Notable upstream pins: `coredomain=17.103.12`, `progression=17.0.246`, `resulting=17.0.37`, `sjp=17.103.166`, `defence=17.0.85`, `referencedata=17.103.131`, `referencedata.offences=17.0.38`, `material=17.0.70`, `authorisation.service=0.1.49`, `stream-transformation-tool=6.4.5`. When bumping any of these, also check the matching schema/RAML classifier dep is on the same version.

## Java style

No wildcard imports. Always use explicit per-class imports.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
