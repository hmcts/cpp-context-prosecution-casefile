# Special cases

Read the section that applies to the context.

## Contents

- Camunda
- Activiti
- Elasticsearch
- Docmosis
- JAXB code generation (Xhibit)
- Azure blob storage in integration tests
- Jakarta Mail

## Camunda

Applies to businessprocesses, boxworkmanagement, work-management-proxy, or any context with `processes.xml`.
Copy businessprocesses' `team/25.104.x` branch. Human-readable version:
https://hmcts.atlassian.net/wiki/spaces/CTP/pages/328728827

Contexts deploy onto one shared engine hosted by WildFly (`<process-engine>default</process-engine>`). Three things
must match: the engine in the WildFly 40 Camunda image (7.24), the schema in the shared `workmanagement` database
(`uk.gov.moj.cpp.camunda:camunda-liquibase` 25.104.x, `25.104.0-M1` or later), and the context's code. A 7.24 engine
on a 7.17 schema deploys fine but timers never fire.

In the context:

- `<cpp.camunda.version>7.24.0</cpp.camunda.version>`, and import `camunda-bom` at that version in the root
  `<dependencyManagement>`.
- `camunda-engine-cdi` becomes `camunda-engine-cdi-jakarta`; `camunda-ejb-client` becomes
  `camunda-ejb-client-jakarta`. `camunda-engine` stays (`provided`). Clean build afterwards. Without this the WAR
  fails with `DefaultEjbProcessApplication ... neither a JakartaServletProcessApplication nor an EJB Session Bean Component`.
- Dockerfile `ARG camunda_liquibase_version=` must be a released 25.104.x version, not `17.21.x`. The shared
  `workmanagement` database-setup image tag is set in `cpp-aks-deploy` (`workmanagement_dbsetup_image_tag`);
  mention it to the user if it looks old.
- Process definitions need a history time-to-live since 7.20 (`ENGINE-12018`). Deployed environments and
  `cpp-developers-docker` set an engine default. For unit tests that start an embedded engine, add
  `<property name="historyTimeToLive" value="P180D"/>` to `camunda.cfg.xml` (and `camunda-cdi.cfg.xml`). Don't add
  TTLs to the BPMN models.
- `taskService.createComment(taskId, ...)` bumps the task revision. Saving a `Task` loaded before the comment fails
  with `OptimisticLockingException` (`ENGINE-03005`). Save first, or reload.
- JavaScript script tasks don't work on JDK 25 (GraalVM `NoSuchMethodError`). None exist today; don't add any.
- Integration-test JMS client: `org.apache.artemis:artemis-jakarta-client`.
- Pipeline keeps `isCamunda: true`. Image tags include `Camunda7.24`.

## Activiti

Applies to prosecution-casefile, sjp, staging-enforcement, staging-prosecutors-spi. Activiti works on Java 25
unchanged; prosecution-casefile passes all its integration tests. Copy its event-processor pom: `activiti-engine`,
`activiti-cdi` and `activiti-embedded-rest`, versioned by the parent's `activiti.library.version`. Don't override
that version.

Dockerfile: `ARG activiti_version=25.104.0` (was `17.21.1`; the changelogs are identical).

`PropertyNotFoundException: Cannot resolve identifier '...'` from `org.activiti.engine.impl.javax.el`, or
`ClassNotFoundException: org.activiti.spring.SpringTransactionContext`, means the event-processor pom differs from
prosecution-casefile's.

## Elasticsearch

Applies to contexts using Elasticsearch (applications-courtorders, unifiedsearch-query and others). The client moved
to `co.elastic.clients:elasticsearch-java` inside `cpp-platform-libraries`, so context code usually doesn't change.

- Set `elasticsearch.embedded.version` to `9.3.3`. The `elasticsearch-maven-plugin` stays on 6.13.
- Remove `elasticsearch-rest-high-level-client`.
- RESTEasy must be the only JAX-RS client on the integration-test classpath. Remove
  `org.apache.tomee:openejb-cxf-rs` or anything else that brings in CXF; otherwise query polls come back empty and
  time out.
- unifiedsearch-query: `pageSize=0` or no page size now returns 10 results, not none.
- Remove the Elasticsearch data volume when switching the local stack between Java 17 and Java 25.

## Docmosis

`/opt/docmosis/cache` and `/opt/docmosis/templates` must exist and be owned by `jboss`. Create them with explicit
paths; `mkdir -p /opt/docmosis/{cache,templates}` under `sh` creates one directory literally named
`{cache,templates}`.

## JAXB code generation (Xhibit)

hearing and listing. See listing's `Java-25-Upgrade-README.md`.

- `org.jvnet.jaxb2.maven2:maven-jaxb2-plugin` becomes `org.jvnet.jaxb:jaxb-maven-plugin` 4.0.8. Drop
  `-Xnamespace-prefix` and its plugin dependency.
- `binding.xjb`: namespace `https://jakarta.ee/xml/ns/jaxb`, `version="3.0"`. The `xjc` namespace stays on
  `java.sun.com`.
- `com.sun.xml.bind:jaxb-impl` / `jaxb-core` become `org.glassfish.jaxb:jaxb-runtime` (else
  `JAXBException: Implementation not found`).
- Namespace prefixes: `org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper` via the property
  `org.glassfish.jaxb.namespacePrefixMapper`.

## Azure blob storage in integration tests

Tests that read back uploaded blobs need Azurite: `buildAndStartContainers "--profile azurite"` in
`runIntegrationTests.sh` (see mi-reportdata).

## Jakarta Mail

`jakarta.mail:jakarta.mail-api` (`provided`) in anything deployed to WildFly; `org.eclipse.angus:angus-mail` in
anything that runs outside it. GreenMail 2.x (see notification-notify).
