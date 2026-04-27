# Architecture & Domain Rules

## Three Layers (CQRS / Event-Sourced)

```
1. Command side (handler → aggregate → domain event)
       ↓ writes to event store (DS.eventstore)

2. Event listener (projects events → viewstore tables)
       ↓ projects to DS.prosecutioncasefile

3. Event processor (consumes domain events → emits public events)
       ↓ emits to public.event for other contexts
```

Every change touching events MUST be reasoned about across **all three layers**. Breaking one without the others produces silent data drift.

- **Command side** — RAML-declared commands hit `@Handles`-annotated handler classes which load the `ProsecutionCaseFile` aggregate, run validation rules, and apply domain events. State mutation centralised via `AggregateStateMutator` / `CompositeCaseAggregateStateMutator`.
- **Event listener** — projects domain events into the viewstore DB (`DS.prosecutioncasefile`). Lives under `prosecutioncasefile-event/prosecutioncasefile-event-listener`. Heavy use of converters mapping events → JPA entities.
- **Event processor** — consumes domain events and emits **public** events for downstream contexts. Lives under `prosecutioncasefile-event/prosecutioncasefile-event-processor`. Heavy use of converters mapping internal events → downstream public schemas.

## Domain Concepts

| Concept                 | Description                                                                                                                              |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Prosecution Case File   | The aggregate. One per prosecution case. Records intake, validates the case, emits domain events on every state change.                  |
| Domain event            | Internal event written to the event store. Examples: `ProsecutionReceived`, `DefendantChanged`, `CcCaseReceived`, `GroupCasesReceived`.  |
| Public event            | Cross-context event emitted on `public.event`. Consumed by progression / courts / defence / sjp / resulting. Has its own JSON schema.    |
| Command                 | Inbound request via `prosecutioncasefile.handler.command` queue. Declared in RAML, dispatched by `@Handles`.                              |
| Listener                | Read-side projection — `*Listener` class extending the framework's listener base; projects events → viewstore JPA entities via converters. |
| Processor               | Public-event emitter — `*Processor` class extending the framework's processor base; maps domain events → public-event payloads via converters. |
| Validation rule         | Predicate evaluated by the aggregate during command handling; under `prosecutioncasefile-validation-rules`. Failures abort the command.  |
| Viewstore               | Read-model database `DS.prosecutioncasefile`, populated by listeners. Schema managed by `prosecutioncasefile-viewstore-liquibase`.        |
| Event store             | Append-only log `DS.eventstore`. Source of truth for aggregate state. Schema managed by `event-repository-liquibase`.                     |

## Three Subscription Sources

The processor and listener can be triggered by:

1. **Internal event topic** `prosecutioncasefile.event` — replay of this context's own domain events.
2. **Public event bus** `public.event` — events from other contexts (progression, sjp, resulting, material, stagingprosecutors).
3. **Command queue** `prosecutioncasefile.handler.command` — RAML-declared commands.

## Authoritative Routing Files (always re-read before reasoning about a flow)

- `prosecutioncasefile-event-sources/src/yaml/event-sources.yaml` — internal + public topic declarations.
- `prosecutioncasefile-event/prosecutioncasefile-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions.
- `prosecutioncasefile-event/prosecutioncasefile-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `prosecutioncasefile-command/prosecutioncasefile-command-handler/src/raml/prosecutioncasefile-command-handler.messaging.raml` — command → handler mapping.
- Per-command/per-event JSON schemas: `src/raml/json/schema/` and `src/main/resources/json/schema/` under each command/domain-event module.

## Intake Channels

| Channel                    | Handler                                                  | Notes                                                                                          |
|----------------------------|----------------------------------------------------------|------------------------------------------------------------------------------------------------|
| SPI / SJP                  | `SjpProsecutionHandler`                                  | listeners: `ProsecutionReceivedListener`, `SjpProsecutionReceivedListener`                     |
| Crown Court                | `CcProsecutionHandler`                                   | events: `cc-case-received(-with-warnings)`                                                     |
| Group prosecutions         | `GroupProsecutionHandler`                                | events: `group-cases-received`, `group-cases-parked-for-approval`                              |
| Summons applications       | `SummonsApplicationHandler`, `SubmitApplicationHandler`  |                                                                                                |
| Civil                      | (no dedicated handler)                                   | driven by progression public events (`civil-case-exists`); civil-specific rules under `validation/rules/defendant/offence/*ForCivil*` |
| Online plea                | `PleadOnlineHandler`                                     |                                                                                                |
| CPS-served IDPC material   | `CpsServeMaterialHandler`                                |                                                                                                |

## Module Layout

- `prosecutioncasefile-common`, `prosecutioncasefile-datatypes-common` — shared utils + value objects
- `prosecutioncasefile-command/-command-api` — RAML + schemas
- `prosecutioncasefile-command/-command-handler` — `@Handles` handlers
- `prosecutioncasefile-domain/-domain-aggregate` — `ProsecutionCaseFile` aggregate + validation rule providers + `AggregateStateMutator`
- `prosecutioncasefile-domain/-domain-event` — event JSON schemas
- `prosecutioncasefile-domain/-domain-event-processor` — domain-level event processor logic
- `prosecutioncasefile-domain/-domain-value-schema` — shared schema objects
- `prosecutioncasefile-event/-event-listener` — listeners + converters → viewstore
- `prosecutioncasefile-event/-event-processor` — processors + converters → public events
- `prosecutioncasefile-event-sources` — `event-sources.yaml`
- `prosecutioncasefile-query/-query-api` — RAML for query side
- `prosecutioncasefile-query/-query-view` — read services over the viewstore
- `prosecutioncasefile-validation-rules` — cross-cutting validation rule wiring
- `prosecutioncasefile-viewstore` — Liquibase changelogs for `DS.prosecutioncasefile`
- `prosecutioncasefile-service` — packaging WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources / queues / topics
- `prosecutioncasefile-progression-service`, `prosecutioncasefile-defence-service` — outbound REST/messaging clients
- `prosecutioncasefile-healthchecks`, `prosecutioncasefile-refdata` — healthchecks and reference-data client wiring
- `prosecutioncasefile-integration-test` — `*IT.java` orchestrated by `runIntegrationTests.sh`

## Adding a New Command

1. **RAML first.** Add the command operation to `prosecutioncasefile-command-handler.messaging.raml` (or the appropriate query RAML).
2. **JSON schema.** Add the command payload schema under the command-api module's `src/raml/json/schema/`.
3. **Handler.** Add a method with `@Handles("<command-name>")` on a class annotated `@ServiceComponent(COMMAND_HANDLER)`. Method takes `Envelope<CommandPayload>`.
4. **Aggregate.** If the command mutates state, the handler calls into the aggregate's `apply(event)` mechanism via the framework. The aggregate emits a domain event.
5. **Mutator.** Register a mutator function for the new event type in `AggregateStateMutator` / `CompositeCaseAggregateStateMutator`.
6. **Listener.** If the new event is consumed by the listener: subscription entry + JSON schema + listener method + converter.
7. **Processor.** If the new event triggers a public event: subscription entry + JSON schema + processor method + converter + public-event JSON schema.
8. **Tests.** Failing unit tests for handler, aggregate, listener (if touched), processor (if touched), converters (if touched). Then production code. Then IT exercising the end-to-end flow.

## Adding a New Domain Event

Same as "Adding a New Command" steps 5–8, plus:

- Add the event's JSON schema under the domain-event module's `src/main/resources/json/schema/`
- Update both listener AND processor `subscriptions-descriptor.yaml` files with the new event entry (or document explicitly which is unaffected)
- Update `event-sources.yaml` if a new internal topic is introduced

## Adding a Public-Event Subscription (incoming from another context)

1. **Subscription entry.** Add to listener and/or processor `subscriptions-descriptor.yaml` for the relevant context's `public.event` source.
2. **JSON schema.** Add the public-event schema (matches the upstream context's contract version) under the consuming module's `src/main/resources/json/schema/`.
3. **Listener / processor method.** With `@Handles("<public-event-name>")` and `Envelope<PayloadType>`.
4. **Converter.** Map the public-event payload → either a viewstore entity (listener) or a domain command (if it triggers a state change).
5. **Tests.** Unit tests for the listener/processor + converter. IT simulating the public-event arrival.

## Out-of-Scope (do not add)

- Hand-rolled JMS listeners — use the framework's `@Handles`
- Hand-rolled JDBC — use Liquibase changelogs and JPA repositories
- Ad-hoc `ObjectMapper` instances — use the framework's configured mapper
- Manual JSON schema validation — the framework validates incoming envelopes against subscription-declared schemas
- Spring annotations (`@Autowired`, `@Component`, `@Service`, `@RequiredArgsConstructor`) — this service does not use Spring
- Cross-context coupling beyond declared public events — never call another context's REST API for command-side traffic; consume their public events instead

## Common Gotchas

1. **Schema-subscription drift** — adding a `subscriptions-descriptor.yaml` entry without the matching JSON schema produces a runtime 500 on dispatch. Constitution Principle VI makes this a review-blocker.
2. **Three-layer drift** — modifying a domain event without updating listener AND processor is the most common silent-data-drift bug. Constitution Principle II makes this a review-blocker.
3. **Liquibase registration** — adding a changelog file without registering it in the right registry (event-store / aggregate-snapshot / viewstore / event-buffer) means it never applies in CI's IT setup.
4. **Cross-context pin drift** — bumping `coredomain` / `progression` / `resulting` / `sjp` / `defence` / `referencedata` / `material` versions in `pom.xml` requires bumping the matching schema/RAML classifier dep to the same version, otherwise schema validation fails at runtime.
5. **Wrong `@ServiceComponent` value** — `COMMAND_HANDLER` vs `EVENT_LISTENER` vs `EVENT_PROCESSOR` are NOT interchangeable; the framework dispatches based on the value.
