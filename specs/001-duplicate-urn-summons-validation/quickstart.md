# Quickstart: Duplicate URN Validation for Summons Application Creation

**Feature**: CIMD-3818 | **Branch**: `CIMD-3818-duplicate-urn-summons-validation`

## What changed

Two guard conditions in `ProsecutionCaseFile.java` (domain aggregate) were extended to also block duplicate submissions when a pending summons application already exists for the same URN — identified by the non-empty `applicationIdToDefendantIdsMap` aggregate state field.

## Files changed

| File | Change |
|------|--------|
| `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/main/java/uk/gov/moj/cpp/prosecution/casefile/aggregate/ProsecutionCaseFile.java` | Two guard conditions updated (SJP path and CC path) |
| `prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate/src/test/java/uk/gov/moj/cps/prosecutioncasefile/ProsecutionCaseFileTest.java` | Four new unit tests added |

## Build and test

```bash
# Unit tests for the changed module (fast — ~30s)
mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate -am test

# Full build + all unit tests
mvn clean install

# Run only the new tests
mvn -pl prosecutioncasefile-domain/prosecutioncasefile-domain-aggregate \
    test -Dtest="ProsecutionCaseFileTest#shouldRejectNewSummonsApplicationWith*+shouldRejectNewCcCase*+shouldRejectNewSjp*+shouldSuccessfully*"
```

Expected: **505 tests, 0 failures, 0 errors**.

## Integration test (when Docker env is available)

```bash
# Full IT run (requires CPP_DOCKER_DIR and Docker)
./runIntegrationTests.sh
```

No new IT class is required for this change — the guard is exercised entirely via unit tests. The existing IT suite covers the end-to-end summons flow; the duplicate guard manifests as a rejection response which the existing IT framework handles.

## Verifying the fix manually (against a running Common Platform env)

1. Create a summons application with any unique URN → expect success.
2. Attempt to create a second summons application with the same URN → expect rejection with inline error "Prosecution reference \<URN\> already exists - enter correct reference" on the 'Prosecutor details' screen.
3. Attempt to create a CC/SJP case with the URN from step 1 → expect the same rejection.
4. Correct the URN to a fresh unique value → expect success.
