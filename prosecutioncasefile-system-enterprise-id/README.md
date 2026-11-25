# prosecutioncasefile-system-enterprise-id

## What This Module Does

This module provides Enterprise ID generation and registration for SJP (Single Justice Procedure) prosecution cases.

An **Enterprise ID** is a 12-character human-readable identifier composed of consonants and digits only (no vowels). It is used by downstream HMCTS enterprise systems to reference a CPP case. The ID is generated during the SJP prosecution intake flow, triggered whenever a `prosecutioncasefile.events.sjp-prosecution-received` event is received.

## Class Summary

| Class | Type | Package | Responsibility |
|---|---|---|---|
| `EnterpriseIdGenerator` | Interface | `uk.gov.moj.cpp.enterpriseid.generator` | Declares `String enterpriseId()` |
| `EnterpriseIdGeneratorImpl` | Class | `uk.gov.moj.cpp.enterpriseid.generator` | Generates an 11-character random string of consonants and digits, then appends a 12th checksum character. The checksum is the sum of character positions mod 11. |
| `EnterpriseIdService` | Interface | `uk.gov.moj.cpp.enterpriseid.mapper` | Declares `String enterpriseIdForCase(UUID caseId)` |
| `EnterpriseIdServiceImpl` | Class | `uk.gov.moj.cpp.enterpriseid.mapper` | Generates the ID, registers it with `SystemIdMapperClient` authenticated as the service system user. Retries on `CONFLICT` (ID already used for a different case). |

## Why This Code Lives Here

### Original Library

This code was originally published as a standalone library, `system-enterprise-id`, hosted on an internal Gerrit server:

```
[internal Gerrit server]/cpp.platform.library.system.enterprise-id
```

The library was never migrated to the `hmcts` GitHub organisation.

### Incompatibility With Jakarta EE 10 / CDI 4.0

During the Java 21 / WildFly 32 / Jakarta EE 10 upgrade (17.104.x release line), the released version `0.0.3` was found to be incompatible with CDI 4.0. The library uses `javax.inject.Inject` (Jakarta EE 8 annotations). WildFly 32 with CDI 4.0 does not process `javax.inject.*` annotations, so `EnterpriseIdServiceImpl.systemUserProvider` is never injected — the field remains `null` and a `NullPointerException` is thrown on every SJP prosecution event.

A corrected `0.0.4-SNAPSHOT` was prepared locally on the Gerrit `dev/java-21-upgrade-spike` branch (replacing `javax.inject` with `jakarta.inject`), but it cannot be pushed to Gerrit from outside the network and `0.0.4` has not been released to Artifactory. The external dependency therefore blocks the upgrade.

### Why Inlining Is Appropriate

- `prosecution-casefile` is the **only service that actually uses this library**. `cpp-context-prosecution-documentqueue` has a stale pom entry but contains zero Java references to the library.
- The library is small: 4 source files, approximately 200 lines of code.
- All transitive dependencies of the library (`id-mapper-client`, `system-users-library`, `framework-api-core`) are already declared in `prosecution-casefile` for other purposes — inlining introduces no new dependencies.
- Inlining eliminates the orphaned Gerrit dependency, unblocks the Java 21 upgrade, and simplifies the dependency graph.

## Follow-On Actions

- The Gerrit repository `cpp.platform.library.system.enterprise-id` can be **archived** once this module is verified working in the upgraded service.
- The stale `system-enterprise-id` dependency in `cpp-context-prosecution-documentqueue` should be **removed** from its pom.xml.
- The `system.enterprise-id.version` property and the `system-enterprise-id` managed dependency entry should be **removed from `cpp-platform-maven-common-bom`** if not already done.

## Reversibility

If a future decision is taken to migrate `system-enterprise-id` to the `hmcts` GitHub organisation as a proper shared library, this module can be deleted and the external dependency reinstated. The package names (`uk.gov.moj.cpp.enterpriseid.*`) are unchanged from the original library, so `SjpProsecutionProcessor` and any other callers would require no code changes.
