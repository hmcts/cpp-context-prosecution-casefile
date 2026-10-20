# Common errors on the Java 25 upgrade

Search this file for a fragment of the error text. "Step N" refers to the workflow in SKILL.md.

## Maven and compilation

| Error | Cause | Fix |
|---|---|---|
| `'dependencies.dependency.version' for ... is missing` after changing the parent | DeltaSpike, OpenEJB, `javax` or `org.apache.activemq:artemis-*` dependencies the new parent doesn't manage | Steps 1, 2 and 7 |
| `Detected JDK Version: 17... is not in the allowed range [25,)` | Building on JDK 17, locally or on the old pipeline agent | JDK 25; Step 12 |
| `class file has wrong version 69.0, should be 61.0` | Java 25 jar in a Java 17 build | Right JDK for the branch |
| `Unsupported class file major version 69` | Old JaCoCo or ASM-based plugin pinned in the context | Remove the pin (Step 3) |
| `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN` | Lombok < 1.18.40 or MapStruct < 1.6.3 | Remove version pins (Step 3) |
| Many `cannot find symbol: method builder()` | Another compile error stopped Lombok | Fix the first non-Lombok error |
| `NoClassDefFoundError: javax/xml/bind/SchemaOutputResolver` | Generator plugin without JAXB 2.3.2 | Step 2 |
| `Provider org.eclipse.parsson.JsonProviderImpl not found` | No JSON-P provider on a plugin or test classpath | Add parsson |
| `... JsonProviderImpl not a subtype` / `Could not initialize class ...JsonObjects` | `org.glassfish:javax.json` in tests | Replace with parsson |
| `RuntimeDelegate ... not a subtype` | `resteasy-jaxrs` in a pom | Remove it |
| `NoClassDefFoundError: jakarta/xml/bind/JAXBException` in repository tests | Hibernate 6.6 made JAXB optional | Add `jakarta.xml.bind-api` test dependency; don't manage it at 2.3.2 in the root pom |
| `NoClassDefFoundError: org/hibernate/internal/util/collections/ArrayHelper` | `hibernate-core` at `test` scope | Make it `provided` |
| `NoSuchMethodError: org.jboss.jandex.Indexer.indexWithSummary` | Older Jandex first on the classpath | `hibernate-core` `provided` |
| `package org.junit does not exist` | JUnit 4 came in via DeltaSpike | Convert to JUnit 5 |
| `TestEngine junit-jupiter failed to discover tests ... unaligned` | Pinned `junit-platform-launcher` | Remove the pin |
| `ClassNotFoundException:${jvm.options}` | Old surefire argLine | `@{argLine} -Duser.timezone=UTC` |
| Module shows 0% coverage in Sonar | Surefire argLine without `@{argLine}` | Step 8 |
| `package javax.ws.rs.core does not exist` in an unchanged module | Stale generated classes from a Java 17 build | `mvn clean` at the root |
| `[WARNING] Skipping resource ... Failed to find schema with id` | Unresolvable `$ref`; schema skipped | Fix the reference |
| `NoSuchMethodError: io.netty.util.internal.PlatformDependent.newFixedMpscUnpaddedQueue(int)` | `netty-common` pinned | Remove the pin |
| H2 rejects the test JDBC URL (`NON_KEYWORDS=VALUE`, `MVCC`, `MV_STORE`) | `h2.version` 1.4.x pinned, or H2 1.x settings left in the URL | Remove the pin and the 1.x settings |

## Deployment (server.log)

| Error | Cause | Fix |
|---|---|---|
| `WELD-000123 ... ClassNotFoundException: org.apache.deltaspike...` | DeltaSpike `<alternatives>` in `beans.xml` | Step 5 |
| `WELD-001303: No active contexts for scope type jakarta.transaction.TransactionScoped` | `@Inject EntityManager` | `@PersistenceContext(unitName = "...")` |
| `WFLYWELD0037 Can't find a persistence unit named ...` | Wrong unit name | Use the name from `persistence.xml` |
| `WFLYJPA0040` | XML declaration changed along with the schema version | Step 6 |
| `WELD-001408: Unsatisfied dependencies` | Class not in a bean archive, or from a `javax` jar | Step 5; check for `WELD-000119` |
| `WELD-000119: Not generating any bean definitions from ... javax.json.JsonValue ... not found` | A `javax`-built jar in the WAR | Find the jar; if it's another context's, tell the user |
| `WELD-001480 ... not proxyable because it contains a final method` | `final` method on an `@ApplicationScoped` class | Remove `final` |
| `WELD-001471` on `@PostConstruct` | Lifecycle method declares a checked exception | Catch and rethrow unchecked |
| `DefaultEjbProcessApplication ... neither a JakartaServletProcessApplication nor an EJB Session Bean Component` | `javax` Camunda artifacts | special-cases.md, Camunda |
| Extension or start-up hook never runs | `META-INF/services/javax.enterprise.inject.spi.Extension` not renamed | Step 4 |
| `AccessDeniedException: /opt/wildfly...` | Wrong WildFly path | `/opt/jboss/wildfly` |

## Runtime and integration tests

| Symptom | Cause | Fix |
|---|---|---|
| `InvalidEventException: Failed to map event. No event registered for class ...` | Empty `beans.xml` in a domain-event module | Step 5 |
| `LazyInitializationException ... (no session)` | Query view not transactional | deltaspike-to-jpa.md section 7 |
| `StrictJpaComplianceViolation` | Non-standard JPQL | deltaspike-to-jpa.md section 8 |
| 404 or `{}` where 200 was expected, or the reverse | Finder's not-found behaviour changed | deltaspike-to-jpa.md section 4 |
| `NullPointerException: Value in JsonObjects name/value pair cannot be null` | JSON-P rejects `add(key, null)` | Only add non-null values |
| NPE on `.isEmpty()` after `getJsonArray(key)`, command retried repeatedly | `getJsonArray` returns `null` for a missing key | Null check |
| `NoSuchMethodError: ...EnumResolver.constructFor...` or `BufferRecycler.releaseToPool()` | Old Jackson inside WireMock standalone jars | Exclude `wiremock-jre8-standalone` and `wiremock-webhooks-extension` |
| Every query poll empty, then timeout | CXF and RESTEasy both on the test classpath | Remove CXF |
| Thousands of `RESTEASY004687` | JAX-RS clients not closed | try-with-resources or `@PreDestroy` |
| `AMQ219019: Session is closed` in a long test class | Newer Artemis client closes idle connections | `setConnectionTTL(-1)`, `setClientFailureCheckPeriod(-1)`, reuse one connection |
| `Expected: <200> but: was <400>` in a queue-count helper | Artemis 2.54 Jolokia rejects the GET | POST the MBean in a JSON body |
| `404` from `:8161/jolokia/...` | Jolokia moved | `:8161/console/jolokia/` |
| `Destination(s) ... not exist` | Context with no command handler doesn't create its queues | Add them to `broker.xml` in `cpp-developers-docker` |
| Expected `...Z[UTC]`, got `...Z` | Framework uses `ZoneOffset.UTC` | Update expected values |
| Test fails only around midnight | Not running in UTC | `-Duser.timezone=UTC` |
| Every IT fails in set-up with HTTP 406, run takes ~4 minutes | Failsafe under `<build><plugins>` | Move to `<pluginManagement>` |
| `ValidationFailedException: addAfterColumn is not allowed on postgresql` | Liquibase 5 | Don't edit the changeset; tell the user |
| `password authentication failed for user "<context>"` locally | `cpp-developers-docker` on the wrong branch | `java-25` |
| Script exits 0 but no tests ran | WAR didn't deploy (often a `WELD-` error or missing JNDI binding in `cpp-developers-docker`'s `standalone.xml`) | Read `server.log` |
| Mockito runs out of memory reading a mocked `InputStream` | commons-io calls `read(byte[], int, int)` | Stub that too |
| PDF byte-size assertion slightly off | Library and JVM change | Update the expected size |

## Pipeline and deployment

| Symptom | Cause | Fix |
|---|---|---|
| `role "<context>" does not exist` | `ubuntu-j25-postgres` agent | `ubuntu-j25` |
| `context deadline exceeded` after ten minutes | WildFly 26 chart for a WildFly 40 image | `aksDeployBranch: 'wildfly40'`; chart `0.25.5-wildfly` |
| Liquibase job times out with `Strict check failed ... 'liquibase.hub.mode'` | `hub.mode` in `liquibase.properties` | Step 9 |
| Build fails ~14 minutes after Sonar passes, "1 errors / 0 warnings", no test failures | Build agent dropped | Re-run |
| Merge ran the pipeline but no new image | Last commit was the release job's, or Dockerfile failed | Check the registry; re-trigger with a real, harmless change such as a comment in the root `pom.xml` (an empty commit doesn't trigger it) |
