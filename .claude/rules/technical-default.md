# Service Identity

- **Service:** cpp-context-prosecution-casefile
- **Description:** Front of the prosecution pipeline. Ingests prosecution cases (SPI/SJP, Crown Court, group cases, summons applications, online plea, civil fees, CPS-served IDPC material), validates them, persists them as an event-sourced aggregate, and forwards successfully created cases as public events to progression / courts / defence / sjp / resulting.
- **Bounded context:** `prosecutioncasefile` (one of many CPP contexts).
- **Programme:** Crime Common Platform (CPP).
- **Organisation:** HMCTS / Ministry of Justice.

## Technology Stack

| Component         | Value                                                                |
|-------------------|----------------------------------------------------------------------|
| Build tool        | Maven (multi-module reactor; root `pom.xml`)                          |
| Language          | Java 17 (CI demand `centos8-j17`; local `mvn17` alias)                |
| Framework         | CPP `service-parent-pom:17.103.x` (JEE/CDI-style)                     |
| Packaging         | WAR → WildFly via Docker                                              |
| Annotations       | `@ServiceComponent`, `@Handles`, `@ApplicationScoped`                 |
| Persistence       | Liquibase changelogs (event-store, aggregate-snapshot, viewstore, event-buffer) |
| Messaging         | ActiveMQ (Docker for ITs); JMS topics + queues                        |
| Tests             | JUnit + Mockito (unit); framework's IT harness (`runIntegrationTests.sh`) |
| CI                | Azure DevOps Pipelines (`azure-pipelines.yaml`)                       |
| Quality gate      | SonarQube in CI (no local Checkstyle/PMD enforcement)                 |
| Java packaging    | Root namespace `uk.gov.moj.cpp.prosecution.casefile.*`                |

## Constraints

- Maven only — Gradle is forbidden in this codebase
- Java 17 only — do not use `var` outside method-local scope where the type is non-obvious; prefer explicit types in public APIs
- Use the CPP framework's `@ServiceComponent` + `@Handles` for command/event handling — NOT hand-rolled JMS listeners
- Aggregate state mutation must go through `AggregateStateMutator` / `CompositeCaseAggregateStateMutator` (functional-interface dispatcher pattern)
- Event listeners and processors must use converter classes in `converter/` packages — NOT inline mapping in the listener/processor body
- Contracts (RAML, JSON schemas, `subscriptions-descriptor.yaml`, `event-sources.yaml`) update FIRST, Java second (Constitution Principle I)
- Schema additions / removals / renames update both `subscriptions-descriptor.yaml` AND JSON schema in lockstep (Constitution Principle VI)
- Logging via SLF4J only — no `System.out` / `System.err` (Constitution Principle VII)
- Test-Driven Development is mandatory (Constitution Principle VIII)

## Build & Test Commands

```bash
# Full build + unit tests
mvn clean install

# Build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Single module with deps
mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate -am clean install

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName

# Integration tests (requires Dockerised env up; CPP_DOCKER_DIR must be set)
./runIntegrationTests.sh

# Single IT against running env
mvn -pl prosecutioncasefile-integration-test test -Dit.test=ClassNameIT

# Framework JMX commands
./runSystemCommand.sh           # help
./runSystemCommand.sh --list    # list available commands
./runSystemCommand.sh CATCHUP   # run one
```

## Key version pins (`pom.xml`)

- Parent: `uk.gov.moj.cpp.common:service-parent-pom:17.103.x` (currently 17.103.3)
- Cross-context pins to keep aligned: `coredomain`, `progression`, `resulting`, `sjp`, `defence`, `referencedata`, `referencedata.offences`, `material`, `authorisation.service`, `stream-transformation-tool`
- When bumping any of these, also check the matching schema/RAML classifier dep is on the same version
