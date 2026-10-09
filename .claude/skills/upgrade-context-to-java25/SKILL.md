---
name: upgrade-context-to-java25
description: Upgrade a CPP bounded context (cpp-context-*) from Java 17 / WildFly 26 / javax to Java 25 / WildFly 40 / Jakarta EE 11 on the 25.104.x framework stack. Use when asked to migrate, upgrade or port a context to Java 25, 25.104.x, WildFly 40 or Jakarta, or to fix build, deploy or integration-test failures on a context's Java 25 branch.
---

# Upgrade a CPP context to Java 25

This skill takes a `cpp-context-*` repository from the Java 17 line (17.104.x, WildFly 26, `javax.*`, DeltaSpike
Data, Hibernate 5, Liquibase 4) to the Java 25 line (25.104.x, WildFly 40, `jakarta.*`, plain JPA, Hibernate 6.6+,
Liquibase 5). It is the machine-readable version of the Confluence guide
[How to Upgrade a Context to Java 25](https://hmcts.atlassian.net/wiki/spaces/CTP/pages/328434064). If the two
disagree, the Confluence page wins, and tell the user.

## Target versions

| Property | Version |
|---|---|
| `service-parent-pom` (the context's `<parent>`) | **25.104.3** |
| `coredomain.version` | **25.104.3** |
| Context project version, first Java 25 version | `25.104.0-M1-SNAPSHOT` |

These change when framework or platform libraries are re-released. Check the "Target versions" table on the
Confluence page before starting, and use those numbers if they're newer. `service-parent-pom` and
`coredomain.version` must always be equal.

## Ground rules

Follow these throughout. They come from mistakes made on earlier contexts.

- Work on a branch. Don't push, raise a pull request, merge or tag unless the user asks you to.
- Never edit an existing Liquibase changeset, and never add `validCheckSum`. Changed checksums break every deployed
  environment. If data needs fixing, add a new changeset.
- Don't add third-party versions to the context. The 25.104.x BOMs manage everything a context needs. Remove
  existing pins rather than updating them. The one exception is snakeyaml, which stays at 1.33.
- Don't commit SNAPSHOT dependencies, `-DskipTests`, `-Denforcer.skip`, `<skip>true</skip>` on tests (except the MI
  contract-test unpack described below) or `@Disabled`. Never delete a failing test to get green.
- Don't add JUnit 4 as a dependency. Convert JUnit 4 tests to JUnit 5.
- Don't convert native SQL to JPQL, and don't change what a repository returns when nothing is found. Both change
  production behaviour without any test failing.
- Production code must not call `Json.createObjectBuilder()`, `Json.createArrayBuilder()`,
  `Json.createReaderFactory()` or `Json.createBuilderFactory()`. Use the shared factory (`jsonBuilderFactory`).
- Run Maven from the project root and read the full output. Don't filter it through `grep`, `tail` or `head`.
- A zero exit code from `runIntegrationTests.sh` doesn't mean tests ran. Check that the WAR deployed, the
  healthchecks passed, and failsafe reports show tests run with zero failures and zero errors.
- Report results as they are. If something failed or was skipped, say so with the output.
- Don't touch the `*-domain-transformation-anonymise` modules unless they break the build. They are unused.
  If one fails on Java 25, remove the module and its `<module>` line.

## Workflow

Copy this checklist into your notes and tick items off as you go:

```
- [ ] 0. Survey the context and record the Java 17 baseline
- [ ] 1. Project version, parent and core-domain
- [ ] 2. Java EE dependencies to Jakarta
- [ ] 3. Remove third-party version pins; Lombok/MapStruct
- [ ] 4. javax -> jakarta in source and service files
- [ ] 5. beans.xml
- [ ] 6. persistence.xml
- [ ] 7. DeltaSpike repositories (references/deltaspike-to-jpa.md)
- [ ] 8. Tests and test plugins
- [ ] 9. Liquibase properties
- [ ] 10. Full build green
- [ ] 11. Integration tests green
- [ ] 12. azure-pipelines.yaml
- [ ] 13. Dockerfile
- [ ] 14. Pre-PR checks and summary for the user
```

### 0. Survey

Run these from the context root and summarise the results for the user before changing anything:

```bash
git status && git branch --show-current
grep -rl "org.apache.deltaspike" --include='*.java' --include=pom.xml --include=beans.xml . | grep -v /target/ | wc -l
find . -name beans.xml -not -path '*/target/*' | wc -l
find . -name beans.xml -size 0 -not -path '*/target/*'
find . -name persistence.xml -not -path '*/target/*'
find . -path '*/META-INF/services/javax.*' -not -path '*/target/*'
grep -rln "camunda\|activiti\|elasticsearch\|docmosis\|jaxb2-maven\|maven-jaxb2" --include=pom.xml . | grep -v /target/
grep -rn afterColumn --include='*.xml' . | grep -v /target/
```

- If DeltaSpike is used, read [references/deltaspike-to-jpa.md](references/deltaspike-to-jpa.md) before Step 7.
- If Camunda, Activiti, Elasticsearch, Docmosis, JAXB code generation, Azure blob storage or Jakarta Mail appear,
  read [references/special-cases.md](references/special-cases.md).
- Ask the user for the Java 17 baseline (unit and integration test counts on `main`), or offer to run it. Without
  a baseline you can't tell a regression from a test that was already failing.
- The context's Java 25 branch must be `team/25.104.x`. The pipelines only run the integration tests on merge for
  `team/...` branches. Never use `release/25.104.x` for a context; that name is for the framework and platform
  libraries. Do the work on a `dev/` branch and raise the pull request against `team/25.104.x`.
- The build needs JDK 25. Check with `java -version` and `mvn -version`.

### 1. Project version, parent and core-domain

Set the project version first, while the old parent still parses. The 25.104.x parent doesn't manage DeltaSpike,
OpenEJB, `hibernate-entitymanager`, the `javax` APIs or `org.apache.activemq:artemis-*`, so once the parent changes,
poms that declare them without a version fail with `'dependencies.dependency.version' for ... is missing`.

```bash
mvn versions:set -DnewVersion=25.104.0-M1-SNAPSHOT -DprocessAllModules=true -DgenerateBackupPoms=false
```

Then set the `<parent>` version to the target `service-parent-pom` version, and `coredomain.version` to the same.
Leave other contexts' interface versions (`progression.version`, `referencedata.version` and so on) on their released
Java 17 versions. They're RAML and JSON contracts.

After the first release, the release job manages the project version (`25.104.1-M1-SNAPSHOT`, ...). Never change it
by hand after that.

### 2. Java EE dependencies to Jakarta

In every pom:

| Remove | Replace with |
|---|---|
| `javax:javaee-api` | `jakarta.platform:jakarta.jakartaee-api` (`provided`) |
| `javax.json:javax.json-api` | `jakarta.json:jakarta.json-api` |
| `javax.xml.bind:jaxb-api` | `jakarta.xml.bind:jakarta.xml.bind-api` |
| `org.glassfish:javax.json` (test) | `org.eclipse.parsson:parsson` (test) |
| `org.apache.activemq:artemis-jms-client` | `org.apache.artemis:artemis-jms-client` |
| `org.jboss.resteasy:resteasy-jaxrs` | nothing, or `resteasy-client` if a client is needed |
| `org.hibernate:hibernate-core`, `hibernate-entitymanager` | `org.hibernate.orm:hibernate-core` (`provided`) |

Leave versions out; the BOMs supply them. If the context redeclares `rest-client-generator-plugin`,
`messaging-client-generator-plugin` or `messaging-adapter-generator-plugin` with its own `<dependencies>`, change
`javax:javaee-api` there too and remove any old parsson or JAXB workarounds. The parent already adds
`jakarta.xml.bind-api` 2.3.2 and parsson to those plugins.

### 3. Third-party version pins, Lombok and MapStruct

Go through the root pom's `<properties>` and `<dependencyManagement>`, and every module pom, and remove versions
for anything that isn't a `uk.gov.justice` or `uk.gov.moj` artifact. Pins known to break Java 25 builds include
`h2.version` 1.4.x, `resteasy.version` 4.x, JaCoCo below 0.8.14, `netty-common` 4.1.x, `junit-platform-launcher`,
`lombok.version` below 1.18.40, `maven-compiler-plugin` 3.8.x and `wildfly-maven-plugin` 4.x. If removing a pin
breaks something, tell the user it needs adding to a BOM. Don't put it back.

JDK 25 no longer runs annotation processors found on the classpath. If the context uses Lombok, configure the
processor path without a version:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPathsUseDepMgmt>true</annotationProcessorPathsUseDepMgmt>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

With MapStruct, add `mapstruct-processor`, `lombok` and `lombok-mapstruct-binding` as paths, again without versions.

If you see dozens of `cannot find symbol: method builder()` errors, Lombok didn't run because something else failed
to compile. Find the first error that isn't about a Lombok-generated method and fix that.

### 4. javax to jakarta

Run this, then review the diff:

```bash
find . -name '*.java' -not -path '*/target/*' -print0 | xargs -0 perl -pi -e \
  's/\bjavax\.(?=(?:annotation\.(?:PostConstruct|PreDestroy|Resource|Priority)|enterprise|inject|json|persistence|ejb|jms|ws\.rs|validation|interceptor|decorator|servlet|el|websocket|transaction(?!\.xa)|xml\.bind|mail|activation)\b)/jakarta./g'
```

Check that nothing is left. This command should print nothing:

```bash
grep -rn 'javax\.' --include='*.java' . | grep -v /target/ \
  | grep -vE 'javax\.(sql|naming|crypto|net|security|management|xml\.(parsers|transform|datatype|validation|namespace|stream)|transaction\.xa|annotation\.(processing|Nullable|Nonnull))'
```

Verify the replacement actually happened (count changed files with `git diff --stat`). A shell quoting problem once
made it silently change nothing.

Also:

- Rename `META-INF/services/javax.enterprise.inject.spi.Extension` to
  `META-INF/services/jakarta.enterprise.inject.spi.Extension`. If you miss it, the CDI extension silently doesn't run.
- Check `web.xml`, `jboss-*.xml`, `ejb-jar.xml` and Drools `.drl` files for `javax` class names.
- Replace `javax.faces.bean.ApplicationScoped` with `jakarta.enterprise.context.ApplicationScoped`,
  `org.jboss.resteasy.util.HttpResponseCodes.SC_*` with `jakarta.ws.rs.core.Response.Status`,
  `org.drools.core.util.StringUtils` with `org.apache.commons.lang3.StringUtils`, and
  `org.apache.activemq.command.ActiveMQTextMessage` in test helpers with `session.createTextMessage(json)`.

### 5. beans.xml

Every `beans.xml` goes to the CDI 4.0 schema with `bean-discovery-mode="all"`:

```xml
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns="https://jakarta.ee/xml/ns/jakartaee"
       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
       version="4.0" bean-discovery-mode="all">
</beans>
```

- Give every empty `beans.xml` this full content. Under CDI 4 an empty file means "annotated" discovery, and
  unannotated classes (typically domain events) stop being beans. That only shows up in integration tests, as
  `InvalidEventException: Failed to map event. No event registered for class ...`.
- Delete any `<alternatives>` block that names a DeltaSpike class. It builds fine but the WAR won't deploy
  (`WELD-000123`).
- A module that needs CDI but has no `beans.xml` gets one.

### 6. persistence.xml

In the main `persistence.xml` (viewstore-persistence):

- namespace `https://jakarta.ee/xml/ns/persistence`, schema `persistence_3_0.xsd`, `version="3.0"` on the
  `<persistence>` element. Leave the `<?xml version="1.0"?>` declaration alone; changing it breaks the deploy with
  `WFLYJPA0040`.
- Replace numbered dialects such as `PostgreSQL9Dialect` with `org.hibernate.dialect.PostgreSQLDialect`, or remove
  the dialect.
- Keep the persistence unit name and `<jta-data-source>`. Note the unit name: every repository needs it.

### 7. DeltaSpike repositories

Follow [references/deltaspike-to-jpa.md](references/deltaspike-to-jpa.md). The points most often got wrong:

- `@PersistenceContext(unitName = "<unit>") EntityManager entityManager;`, never `@Inject EntityManager`.
- Single-result finders keep throwing `NoResultException` when nothing matches (`getSingleResult()`).
- Query-view services that read lazy collections need `@Transactional`.
- No `final` methods on `@ApplicationScoped` classes.

### 8. Tests and test plugins

- Convert JUnit 4 tests to JUnit 5 (`@Test` from `org.junit.jupiter.api`, `@BeforeEach`, `@ExtendWith(MockitoExtension.class)`,
  `assertThrows`, assertion messages move to the last argument, `junit-jupiter-dataprovider` becomes `@ParameterizedTest`).
- Any surefire `<argLine>` must start with `@{argLine}`, or JaCoCo isn't attached and Sonar sees 0% coverage.
  Remove `${jvm.options}`. Use `<argLine>@{argLine} -Duser.timezone=UTC</argLine>`.
- Failsafe configuration belongs in `<pluginManagement>`, never `<build><plugins>`. Otherwise integration tests run
  during the normal build, before deployment, and all fail with HTTP 406.
- Expected times built with `ZoneId.of("UTC")` change to `ZoneOffset.UTC` (the framework now writes `...Z`, not
  `...Z[UTC]`).
- Mocked `InputStream`s must stub `read(any(), anyInt(), anyInt())` as well as `read()`.
- The event-listener module's unpack of `mireportdata-<context>-event-listener:RELEASE:tests` resolves to a `javax`
  jar. Set `<skip>true</skip>` on that one `maven-dependency-plugin` execution and tell the user.

### 9. Liquibase properties

In every `liquibase.properties` (viewstore, eventstore-setup, viewstore-setup):

- remove `liquibase.hub.mode` (Liquibase 5 rejects it and the Helm pre-install job times out);
- add `liquibase.analytics.enabled: false`.

If `afterColumn` appears in changesets, don't change them. The parent shades a compatibility extension into the
Liquibase jar. If a migration still fails on it, stop and tell the user.

### 10. Build

Run a full build from the root (`mvn clean install`). Fix errors in order, starting with the first one; later errors
are often caused by earlier ones. Search the output for `Skipping resource`: a JSON schema with an unresolvable
`$ref` is skipped with only a warning, and its generated class goes missing. Generated constructors take arguments
in alphabetical order, so a schema that gained a field shifts positional constructor calls; switch those to the
builder, starting from `withValuesFrom(original)` when converting setters.

When stuck on an error, check [references/common-errors.md](references/common-errors.md).

### 11. Integration tests

- `cpp-developers-docker` must be on its `java-25` branch and up to date (Artemis 2.54 since 28 September 2026).
- Run `mvn clean` at the root after switching between Java 17 and Java 25 branches. `runIntegrationTests.sh` only
  cleans the service module.
- Run `./runIntegrationTests.sh`. While it runs, watch `cpp-developers-docker/containers/wildfly/log/server.log`
  for `ERROR`, `WELD-` and `WFLY` failures, and stop to fix a failed deployment rather than waiting for timeouts.
- To redeploy a WAR by hand, `docker cp` it into `containers-cpp-wildfly-1:/tmp/` and run
  `/opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="deploy /tmp/<war> --force"`. WildFly 40 doesn't scan
  `deployments/`.
- Tests that query Artemis through Jolokia need `http://localhost:8161/console/jolokia/` and a POST with the MBean
  in a JSON body. A GET with an escaped backslash in the MBean name returns 400 on Artemis 2.54.
- Compare the counts with the Java 17 baseline. Every difference needs an explanation.

### 12. azure-pipelines.yaml

Three changes: the templates repository `ref: 'wildfly40'`, the pool demand `identifier -equals ubuntu-j25`
(not `ubuntu-j25-postgres`), and `aksDeployBranch: 'wildfly40'` next to `itTestFolder`. Camunda contexts keep
`isCamunda: true`. Don't add `docker_build_image`, `dockerfilePath` or `repository`.

### 13. Dockerfile

Nothing local builds the Dockerfile, so read it:

- no `yum` (the base image is Ubuntu 24.04);
- WildFly lives in `/opt/jboss/wildfly`, not `/opt/wildfly`;
- remove old `ARG framework_version=` and `ARG eventstore_version=` defaults;
- download `file-service-liquibase` with `${fileservice_version}` (add `ARG fileservice_version`), not
  `${framework_libraries_version}`;
- `docker/scripts/liquibase.sh` starts with `#!/bin/bash`;
- Activiti and Camunda Liquibase jar versions: see [references/special-cases.md](references/special-cases.md).

### 14. Before handing back

Run and report:

```bash
grep -rnE 'Json\.create(ObjectBuilder|ArrayBuilder|ReaderFactory|BuilderFactory)\(' --include='*.java' . | grep /src/main/
grep -rn 'SNAPSHOT' --include=pom.xml . | grep -v /target/
grep -rn '<skip>\|skipTests\|enforcer.skip' --include=pom.xml . | grep -v /target/
grep -rn deltaspike . --include='*.java' --include=pom.xml --include='*.xml' | grep -v /target/
```

The first command should print nothing. In the SNAPSHOT output, the only acceptable lines are the context's own
project version (in each module's `<parent>` and the root `<version>`). The only acceptable `<skip>` is the MI
contract-test unpack from Step 8.

Then give the user a short summary: parent and core-domain versions, unit and integration test counts against the
baseline, anything skipped and why, and anything the owning team needs to decide (for example
`hibernate.enable_lazy_load_no_trans`). Suggest they update the context's row on the
[Java 25 Upgrade Status](https://hmcts.atlassian.net/wiki/spaces/PETTA/pages/280299472) page.

If the Java 25 branch later needs `main` merged in, re-run the Step 4 check for new `javax.` references and compare
each pom's dependencies with `origin/main`. Conflict-free merges have dropped dependencies before.

## Examples to compare against

Each of these has a finished `team/25.104.x` branch:

- cpp-context-system-scheduling: the simplest, no JPA.
- cpp-context-notification: a small DeltaSpike conversion.
- cpp-context-users-groups: the most complete, including behaviour-parity work.
- cpp-context-listing: native queries, JSON columns, Hibernate 6 types, JAXB. Has a `Java-25-Upgrade-README.md`.
- cpp-context-businessprocesses: Camunda.
- cpp-context-prosecution-casefile: Activiti.
- cpp-context-applications-courtorders, cpp-context-unifiedsearch-query: Elasticsearch.
