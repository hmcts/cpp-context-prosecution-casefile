# Coding Conventions — MOJ / CPP Standard (this service)

## Dependency Injection / Component Wiring

- Use the CPP framework's component model — `@ApplicationScoped` for framework-managed singletons, `@Inject` (CDI) for collaborator injection where the framework expects it
- For command handlers: `@ServiceComponent(COMMAND_HANDLER)` on the class + `@Handles("<command-name>")` on the method
- For event listeners: framework listener base + `@Handles("<event-name>")` on listener methods, `@ServiceComponent(EVENT_LISTENER)` on the class
- For event processors: framework processor base + `@ServiceComponent(EVENT_PROCESSOR)` on the class
- Do NOT use Spring annotations (`@Autowired`, `@Component`, `@Service`, `@RequiredArgsConstructor`) — this service does not use Spring
- Do NOT roll your own JMS listener / JDBC connection / ObjectMapper

## Envelope / Payload Handling

- Handler/listener method signatures take `Envelope<PayloadType>`, never the raw payload type
- Read the payload via `envelope.payload()`; metadata via `envelope.metadata()`
- Correlation context (correlation id, user id, etc.) lives in `envelope.metadata()` and should be propagated into MDC for SLF4J
- Treat the payload as immutable — do not mutate fields after reading

## Aggregate State Mutation

- All aggregate state mutation goes through `AggregateStateMutator` / `CompositeCaseAggregateStateMutator`
- One mutator function per event type, composed into the central dispatcher
- Aggregates emit domain events via the framework's `apply(event)` mechanism — do not write events directly to the event store

## Converters (Listener and Processor)

- Listener converters live under `prosecutioncasefile-event-listener/.../converter/` and map domain events → JPA viewstore entities
- Processor converters live under `prosecutioncasefile-event-processor/.../converter/` and map domain events → public-event payloads
- Each converter is a single-purpose class (one event → one target shape); composition happens at the listener/processor level

## Error Handling

- Custom exceptions extend `RuntimeException` (or framework-specific bases like `EventStreamException`)
- NEVER swallow exceptions silently — always log or rethrow
- Listener / processor methods can let framework exceptions propagate; the framework handles redelivery and dead-letter routing
- Invalid envelope payloads should fail loudly with a meaningful message — the framework re-delivers, so a silent skip leaks broken state

## Logging

- SLF4J with the framework's logger configuration
- Use `private static final Logger LOGGER = LoggerFactory.getLogger(...)` (or framework-specific equivalent if the codebase has one)
- MDC keys: include correlation id and other relevant fields from `envelope.metadata()`
- NEVER use `System.out.println`, `System.err.println`, or `Throwable#printStackTrace()` (Constitution Principle VII)
- NEVER log sensitive data (defendant identifiers in plain text without masking, tokens, passwords, PII)

## Imports

- NEVER use wildcard imports (`import java.util.*`) — always use explicit imports for each class

## Naming Conventions

| Component        | Pattern                  | Example                                  |
|------------------|--------------------------|------------------------------------------|
| Command handler  | `*Handler`               | `SjpProsecutionHandler`                  |
| Event listener   | `*Listener`              | `ProsecutionReceivedListener`            |
| Event processor  | `*Processor`             | `ProsecutionCreatedProcessor`            |
| Converter        | `*Converter` or `*To*Converter` | `ProsecutionCaseFileOffenceToCourtsOffenceConverter` |
| Aggregate        | (singular noun)          | `ProsecutionCaseFile`                    |
| Domain event     | (past tense)             | `ProsecutionReceived`, `DefendantChanged` |
| Public event     | (past tense)             | `cc-case-received`, `group-cases-received` |
| Service / view   | `*Service`               | `CaseDetailsService`                     |
| Test             | `*Test` / `*IT`          | `SjpProsecutionHandlerTest`, `CaseFileQueryIT` |
| Validation rule  | `*Rule`                  | (under `validation/rules/...`)            |

## Testing Conventions

- JUnit + Mockito for unit tests
- Use `@ExtendWith(MockitoExtension.class)` (or the codebase's preferred Mockito wiring — check existing tests)
- Use `@Nested` classes with `@DisplayName` for grouped scenarios
- Method naming: `{action}_{scenario}_should_{expectation}`
- Use AssertJ (`assertThat(...)`) where the codebase already does; otherwise plain JUnit assertions are fine
- Integration tests live in `prosecutioncasefile-integration-test` and run via `./runIntegrationTests.sh`
- IT class name suffix: `*IT`
- Test commands:
  - `mvn test` — unit tests only
  - `./runIntegrationTests.sh` — full Dockerised IT run
  - `mvn -pl prosecutioncasefile-integration-test test -Dit.test=ClassNameIT` — single IT against running env
- TDD: write the failing test first, see it fail for the right reason, then implement (Constitution Principle VIII)
- Logging in tests: SLF4J only (Constitution Principle VII)

## RAML / JSON Schema

- RAML files: `src/raml/...` per module that owns commands or queries
- JSON schemas: `src/main/resources/json/schema/` (or `src/raml/json/schema/` for legacy paths)
- Every command in RAML has a matching `@Handles` method; every event in `subscriptions-descriptor.yaml` has a matching listener / processor method
- Every event has a JSON schema; every JSON schema is referenced from at least one contract artefact
- When adding a new event:
  1. JSON schema first
  2. `subscriptions-descriptor.yaml` entry
  3. `event-sources.yaml` if a new topic is involved
  4. Java listener / processor method last
