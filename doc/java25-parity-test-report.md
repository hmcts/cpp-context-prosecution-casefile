# Java 25 parity test report — prosecution-casefile

**Branch:** `dev/25.104-cimd-java25` (`fb827b06`, PR #172).
**Baseline:** Java 17 `main` (`a63a4c5e`).
**Date:** 2026-10-13.

## What this is

These are characterisation ("golden master") tests. The same scenario code runs against the Java 17 `main` branch and against this Java 25 branch:
- On **Java 17 every test passes**. That records how the service behaved before the upgrade.
- On **Java 25 a failing parity test is a behaviour change introduced by the upgrade.**

Only the test harness and the `javax`/`jakarta` imports differ between the two copies. No production code was changed.

## Results

| Run | Tests | Failed | Notes |
|---|---|---|---|
| Java 17 `main`, full unit suite (JDK 17) | 1425 | **0** | baseline |
| Java 25 this branch, full unit suite (JDK 25, Hibernate 6.6.1 from the BOM) | 1461 | **25** | all 25 are parity tests; every other test passes |
| Java 25 persistence parity tests on Hibernate **7.3.2** (the version WildFly 40 ships) | 17 | 1 | the same PCF-02 failure |

## Behaviour changes found (tests fail on Java 25)

| ID | Severity | What changed | Test (failing on J25) | Suggested fix |
|---|---|---|---|---|
| PCF-02 | **Major** | `persistence.xml` sets `hibernate.event.merge.entity_copy_observer=allow`. If two defendants carry the same offence id, Java 17 failed loudly. Java 25 saves silently, and the offence ends up under one defendant only. **Seen live:** the CC duplicate-defendants IT case leaves 2 of its 3 defendants with no offence in the viewstore, and no error is logged. | `viewstore-persistence` `CaseGraphPersistenceParityTest#pcf02_shouldFailLoudlyWhenTwoDefendantsCarryTheSameOffenceId` (1) | Remove the `allow` property. The full IT suite still passes (224/0/3) without it. |
| PCF-01 | Minor | `PleadOnlineValidator:71`: a GUILTY online plea with a whitespace-only `employmentStatus` is now accepted. Java 17 rejected it, because Drools `StringUtils.isEmpty` treated blank as empty. | `command-api` `PleadOnlineValidatorTest#shouldReturnErrorWhenGuiltyPleaHasBlankEmploymentStatus` (5) | Use `StringUtils.isBlank` (commons-lang3). |
| PCF-06 | Minor | `RefDataHelper` courtroom mapping was rewritten from Jackson to hand-written JSON-P. It drops refdata fields outside the schema, no longer converts types (`"905"` → 905, `6` → `"6"`), and throws `ClassCastException` where Java 17 returned empty. Private events and the submit-application command lose fields; public events are not affected. | `refdata` `RefDataHelperCourtroomParityTest` (12) | Revert to the framework `ObjectMapper`; the test shows it still matches Java 17. |
| PCF-08 | Minor | The vendored `prosecutioncasefile-system-enterprise-id` module differs from `system-enterprise-id:0.0.3`: <ul><li>it stops after 10 id conflicts and throws `EnterpriseIdAllocationException`, where 0.0.3 kept retrying;</li><li>it throws `IllegalStateException` instead of `NoSuchElementException` when there is no system user;</li><li>it looks up the system user once, not once per attempt;</li><li>it uses `SecureRandom` and `@ApplicationScoped`.</li></ul> | `event-processor` `EnterpriseIdParityTest` (7) | Accept it explicitly: owner sign-off and a note in the PR. The 10-attempt cap is arguably safer, because 0.0.3 ended in `StackOverflowError`. |

## Checked and unchanged (tests pass on both)

| ID | Area | Test |
|---|---|---|
| PCF-05 | The MI contract test is skipped on J25, but no event schema drifted: the 56 schemas the 8 MI events use are identical. | `event-listener` `MiSchemaContractParityTest` |
| PCF-10 | `LocalDate.now()` gives the same dates on both. Java 17 forced UTC as a side effect of the DeltaSpike `EntityManager` producer; Java 25 depends on the image running in UTC, which current images do. Pin `LocalDate.now(ZoneOffset.UTC)` to be safe. | `event-listener` `ResolvedCaseDateZoneParityTest`, `query-view` `ResolvedCaseCountDateZoneParityTest` |
| PCF-17 / PCF-18 | The `@MapsId` removal, `IN :p` without brackets, and the primitive `orderIndex` on a nullable column behave the same, on Hibernate 6.6.1 and 7.3.2. | `viewstore-persistence` `ViewstoreMappingParityTest` |
| PCF-03 | The `individualAliases` change from LAZY to EAGER causes no duplicate aliases. | `viewstore-persistence` `CaseGraphPersistenceParityTest` (alias scenarios) |

## How to run

```bash
# Java 25 (this branch): expect exactly the 25 parity failures above
sdk use java 25.0.3-oracle
mvn -B clean package -Dmaven.test.failure.ignore=true -Djacoco.skip=true

# One area only
mvn -pl prosecutioncasefile-viewstore/prosecutioncasefile-viewstore-persistence test -Dtest=CaseGraphPersistenceParityTest

# PCF-02 with the fix applied (expect 0 failures)
mvn -pl prosecutioncasefile-viewstore/prosecutioncasefile-viewstore-persistence test -Dtest=CaseGraphPersistenceParityTest -Dpcf.entityCopyObserver=disallow
```

Run the same test classes on a Java 17 `main` checkout (`sdk use java 17.0.12-oracle`) to see the baseline pass. `EnterpriseIdParityTest` does not compile in the Java 17 event-processor module, because that module has no id-mapper-client on its classpath. Its Java 17 baseline (14/14) was run against the `system-enterprise-id:0.0.3` jar in a standalone Maven project.

## Notes for maintainers

- When a fix lands, the matching parity test should start passing. Keep the tests as regression guards.
- Unit tests run Hibernate 6.6.1 from the BOM, but WildFly 40 runs Hibernate 7.3.2. Persistence findings were checked on both.
- Out of scope here (not unit-testable): the platform Activiti no-op transaction gap (PCF-04), Liquibase/image config (PCF-09), and PR hygiene items.
