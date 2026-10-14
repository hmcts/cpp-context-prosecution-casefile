# DeltaSpike Data to plain JPA

DeltaSpike Data has no Jakarta release, and its `@Query` refers to `javax.persistence`, so no module containing a
DeltaSpike repository compiles on 25.104.x. Every repository becomes an ordinary `@ApplicationScoped` class using
an `EntityManager`. Worked examples: cpp-context-notification (small), cpp-context-users-groups (native queries,
parity fixes), cpp-context-listing (optional filters, JSON columns, tests against PostgreSQL). All on `team/25.104.x`.

Human-readable version: https://hmcts.atlassian.net/wiki/spaces/CTP/pages/328794415

## Contents

1. viewstore-persistence pom
2. beans.xml
3. Converting repositories
4. "Not found" behaviour (read this before changing any finder)
5. save versus merge, deleteById
6. final methods
7. Lazy loading in query views
8. Hibernate 6 changes
9. Repository tests
10. Checklist

## 1. viewstore-persistence pom

Remove: `uk.gov.justice.services:persistence-deltaspike`, all `org.apache.deltaspike.*` artifacts (data module,
core, test-control, cdictrl-openejb), `org.apache.tomee:openejb-core` and `openejb-server`,
`org.apache.activemq:activemq-ra`, `org.hibernate:hibernate-entitymanager`, `org.hibernate:hibernate-jpamodelgen`,
`uk.gov.justice.event-store:test-utils-persistence`. Delete
`src/test/resources/META-INF/apache-deltaspike_test-container.properties`.

Add, without versions:

```xml
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>uk.gov.justice.utils</groupId>
    <artifactId>test-utils-hibernate</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
<!-- Hibernate 6.6+ declares jakarta.xml.bind-api as optional; needed in tests (no container) -->
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <scope>test</scope>
</dependency>
```

- `hibernate-core` must be `provided`. With `test` scope the metamodel processor fails with
  `NoClassDefFoundError: org/hibernate/internal/util/collections/ArrayHelper`, and an older Jandex can win
  (`NoSuchMethodError: org.jboss.jandex.Indexer.indexWithSummary`).
- Never manage `jakarta.xml.bind-api` at `${jakarta.xml.bind-api.raml.version}` (2.3.2) in the root
  `<dependencyManagement>`. It overrides 4.x everywhere and causes `NoClassDefFoundError: jakarta/xml/bind/JAXBException`.
- Remove any `h2.version` pin.
- DeltaSpike's test module brought JUnit 4 in transitively. Convert affected tests to JUnit 5; don't add JUnit 4.

## 2. beans.xml

Delete any `<alternatives>` block naming `org.apache.deltaspike...BeanManagedUserTransactionStrategy`. It builds and
unit tests pass, but deployment fails with `WELD-000123`.

## 3. Converting repositories

```java
@ApplicationScoped
public class EventCacheRepository {

    @PersistenceContext(unitName = "notification")
    EntityManager entityManager;

    public List<EventCache> findByUserIdOrderByCreatedDesc(final UUID userId) {
        return entityManager.createQuery(
                        "SELECT e FROM EventCache e WHERE e.userId = :userId ORDER BY e.created DESC",
                        EventCache.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public EventCache save(final EventCache eventCache) {
        return entityManager.merge(eventCache);
    }
}
```

- Always `@PersistenceContext(unitName = "<unit from main persistence.xml>")`. `@Inject EntityManager` deploys, then
  fails outside a transaction with `WELD-001303: No active contexts for scope type jakarta.transaction.TransactionScoped`.
  A wrong unit name fails deployment with `WFLYWELD0037`. The unit name isn't always the context name
  (subscriptions uses `subscription-persistence-unit`).
- Keep the class name and package so callers' `@Inject` still works.
- Only implement methods that are called. For an empty `interface XRepository extends EntityRepository<X, UUID> {}`,
  search main code for `xRepository\.` to see which built-in methods are used.
- `@Query` strings move unchanged into `createQuery` / `createNativeQuery`, with `setParameter` for each `@QueryParam`.
  Never rewrite native SQL as JPQL.
- DeltaSpike `criteria()` becomes the standard `CriteriaBuilder` (see notification's `SubscriptionRepository`).
- If several repositories need the same built-ins, put them in an abstract base class in the context.

Built-in method mapping:

| DeltaSpike | JPA |
|---|---|
| `save(e)` | `entityManager.merge(e)` (see section 5) |
| `saveAndFlush(e)` | `merge`, then `flush()` |
| `saveAndFlushAndRefresh(e)` | `merge`, `flush`, `refresh(merged)` |
| `findBy(id)` | `entityManager.find(E.class, id)` (returns `null`, as before) |
| `findOptionalBy(id)` | `Optional.ofNullable(entityManager.find(E.class, id))` |
| `findAll()` | `createQuery("SELECT e FROM E e", E.class).getResultList()` |
| `findAll(start, max)` | the same with `setFirstResult(start).setMaxResults(max)` |
| `remove(e)` | `entityManager.remove(entityManager.contains(e) ? e : entityManager.merge(e))` |
| `removeAndFlush(e)` | `remove`, then `flush` |
| `count()` | `createQuery("SELECT COUNT(e) FROM E e", Long.class).getSingleResult()` |
| `QueryResult<E>` | `List<E>`, paging with `setFirstResult` / `setMaxResults` |
| `@Modifying @Query` | `executeUpdate()` |

## 4. "Not found" behaviour

DeltaSpike single-result finders throw `NoResultException` when nothing matches: every method-name finder without
`@Query` that returns one entity, and every `@Query` returning one entity unless `singleResult = OPTIONAL`. Callers
rely on this (they catch it, and tests use `thenThrow(new NoResultException())`). Preserve it:

| DeltaSpike | JPA | Nothing matches |
|---|---|---|
| `findByX(...)` returning one entity, no `@Query` | `getSingleResult()` | throws `NoResultException` |
| `@Query` returning one entity | `getSingleResult()` | throws `NoResultException` |
| `@Query(singleResult = SingleResultType.OPTIONAL)` | `getResultStream().findFirst().orElse(null)` | `null` |
| `findOptionalByX(...)` | `getResultStream().findFirst().orElse(null)` | `null` |
| `findAnyByX(...)` | `getResultStream().findFirst().orElse(null)` | `null` |
| collection return | `getResultList()` | empty list |
| `findBy(id)` | `entityManager.find(...)` | `null` |

Before changing any finder, run `grep -rn "NoResultException" --include='*.java' . | grep -v /target/` and check the
callers. Returning `null` where DeltaSpike threw turned a 404 into a 200 in users-groups.

## 5. save versus merge, deleteById

DeltaSpike `save` persisted new entities, so the caller's object became managed. `merge` returns a managed copy.
Code that keeps changing the original, compares identity, or passes a `getReference(...)` placeholder can break.
Where that matters, keep persist semantics:

```java
public Case save(final Case caseEntity) {
    if (caseEntity.getId() != null && entityManager.find(Case.class, caseEntity.getId()) != null) {
        return entityManager.merge(caseEntity);
    }
    entityManager.persist(caseEntity);
    return caseEntity;
}
```

A DeltaSpike `deleteById` without `@Query` loaded and removed the entity, which cascades to children. Don't turn it
into a JPQL `DELETE` (that leaves orphans). Use `find` then `remove`. Method-name bulk deletes with no cascade can
become `executeUpdate()`.

## 6. final methods

Remove `final` from non-private methods of `@ApplicationScoped` classes, or deployment fails with `WELD-001480 ...
not proxyable because it contains a final method`.

## 7. Lazy loading in query views

The container-managed `EntityManager` closes after each call and query views don't run in a transaction, so
touching a lazy collection while building a response fails with `LazyInitializationException ... (no session)`.
Only integration tests show this.

Fix: annotate the query-view service method (the outermost method that both loads the entity and builds the
response) with `jakarta.transaction.Transactional`. Not the repository; the transaction would end too early.

Alternatives: `JOIN FETCH` in the query; `FetchType.EAGER` on one association; or
`hibernate.enable_lazy_load_no_trans=true` in `persistence.xml`, which changes behaviour context-wide and is the
owning team's decision, so ask the user. Never make a unidirectional `@OneToMany @JoinColumn` eager: Hibernate then
nulls the foreign key on every parent save (data loss seen in defence).

## 8. Hibernate 6 changes

Strict JPQL (`StrictJpaComplianceViolation`):

| Before | After |
|---|---|
| `from Widget w where ...` | `select w from Widget w where ...` |
| `join fetch a.b as c ... order by c.f` | `join fetch a.b ... order by a.b.f` |
| `REGEXP_REPLACE(...)`, `REPLACE(...)` | `FUNCTION('REGEXP_REPLACE', ...)`, `FUNCTION('REPLACE', ...)` |
| `IN :list` | `IN (:list)` |
| `count(id) as count` | `count(id)` |
| column name in JPQL | entity field name |

Only if there's no standard way to write a query (for example a row-value `where (a, b) in (select ...)`), set
`hibernate.jpa.compliance.query=false` in the main `persistence.xml`.

Nulls and types:

- `x != null` never matches; use `x IS NOT NULL`.
- `NULL` into a primitive field throws `PropertyAccessException`. Make the field a wrapper (`Long`) and keep a
  primitive getter that defaults. Exception: `@Version` fields stay primitive; add a new changeset setting `NULL`
  rows to 0 instead.
- Native results: `DATE` is `LocalDate`, `TIMESTAMP` is `LocalDateTime`; `COUNT` may not be `BigInteger`, use
  `((Number) result).longValue()`; a column selected twice throws `NonUniqueDiscoveredSqlAliasException`; a `null`
  bind in an optional filter needs `cast(? as varchar)`; strings bound to UUID columns need `cast(... as uuid)`;
  aliases mapped to entity fields should be lower-case.
- `TEXT` column mapped to a `UUID` field: add `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- JSON columns: remove `com.vladmihalcea:hibernate-types`, replace `@TypeDef`/`@Type(type = "jsonb-node")` with
  `@JdbcTypeCode(SqlTypes.JSON)`. Remove `pg-uuid` type definitions.
- Entity collections must be mutable (`new ArrayList<>(x)`), or `merge` throws `UnsupportedOperationException`.
- `ConstraintViolationException` is no longer wrapped in `PersistenceException`; merging an existing primary key
  updates instead of throwing `EntityExistsException`. Adjust tests that assert either.

## 9. Repository tests

JUnit 5 with `HibernateTestEntityManagerProvider` and in-memory H2. Test `persistence.xml` in
`src/test/resources/META-INF/`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
             version="3.0">
    <persistence-unit name="notification-test-persistence-unit" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <jar-file>../classes</jar-file>
        <exclude-unlisted-classes>false</exclude-unlisted-classes>
        <properties>
            <property name="jakarta.persistence.jdbc.driver" value="org.h2.Driver"/>
            <property name="jakarta.persistence.jdbc.url"
                      value="jdbc:h2:mem:notificationtest;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE"/>
            <property name="jakarta.persistence.jdbc.user" value="sa"/>
            <property name="jakarta.persistence.jdbc.password" value=""/>
            <property name="hibernate.archive.autodetection" value="class"/>
            <property name="hibernate.hbm2ddl.auto" value="create"/>
            <property name="hibernate.show_sql" value="false"/>
        </properties>
    </persistence-unit>
</persistence>
```

```java
class SubscriptionRepositoryTest {

    private static final String PERSISTENCE_UNIT = "notification-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        subscriptionRepository = new SubscriptionRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(subscriptionRepository);
    }

    @Test
    void shouldFindSubscriptionsModifiedBeforeTheGivenTime() {
        // arrange with the repository, flush, clear, then query and assert field by field
    }
}
```

- The `@RegisterExtension` field must be `static`. Each test is rolled back afterwards.
- Share the provider's `EntityManager` between repositories used in one test.
- Add H2 reserved words that clash with column names to `NON_KEYWORDS` (seen: `VALUE`, `KEY`, `HOUR`, `MINUTE`).
  Remove `MVCC=FALSE` and `MV_STORE=FALSE` from JDBC URLs.
- Call `flush()` before `clear()`. Fix test data that violates not-null foreign keys rather than relaxing
  constraints. Build `@MapsId` children from a parent loaded through the repository.
- Test fakes that implemented a repository interface must now extend the class.
- PostgreSQL-only SQL (`to_tsquery`, `json_array_elements`) can't run on H2. Either mock the `EntityManager` and
  verify the SQL and parameters (users-groups, applications-courtorders), or test against the integration-test
  PostgreSQL with `hbm2ddl.auto=none`, in the integration-test module behind the integration-test profile (listing).
- The provider forces `hibernate.jpa.compliance.query=true`. A query that needs compliance off in production needs
  its own test class with its own `EntityManagerFactory` (results' `InformantRegisterRepositoryRowValueQueryTest`).
- Sonar treats rewritten repositories as new code and wants about 80% unit-test coverage. Give every repository
  method a test.

## 10. Checklist

- [ ] `grep -rn deltaspike . | grep -v /target/` finds nothing
- [ ] `grep -rn -B1 "EntityManager entityManager" --include='*.java' . | grep "@Inject"` finds nothing
- [ ] every `@PersistenceContext` uses the main `persistence.xml` unit name
- [ ] no non-private `final` methods on `@ApplicationScoped` repositories
- [ ] single-result finders behave as before when nothing matches
- [ ] no cascading `deleteById` became a bulk `DELETE`
- [ ] query views reading lazy collections run in a transaction
- [ ] every repository method has a unit test
