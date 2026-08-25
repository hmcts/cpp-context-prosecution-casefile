package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class ResolvedCasesRepositoryTest {
    private static final List<ResolvedCases> resolvedCasesList = new ArrayList<>();

    private final static long FILTER_RESULTS_FOR_JUST_DATE_COUNT = 10;
    private final static long FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_COUNT = 20;
    private final static long FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_AND_COURT_LOCATION_COUNT = 30;
    private final static long FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_AND_COURT_LOCATION_AND_CASE_TYPE_COUNT = 40;

    private static final String REGION = "region";
    private static final String COURT_LOCATION = "courtLocation";
    private static final String CASE_TYPE = "caseType";

    /**
     * Caller-controlled filter values crafted to break out of a string-concatenated SQL/JPQL
     * predicate. Referenced by the injection-resistance tests below (OWASP A03:2021).
     */
    private static final String TAUTOLOGY_PAYLOAD = "nowhere' OR '1'='1";
    private static final String COMMENT_TRUNCATION_PAYLOAD = REGION + "' --";
    private static final String STACKED_DDL_PAYLOAD = REGION + "'; DROP TABLE resolved_cases; --";


    public static LocalDate fromDate = LocalDate.of(2020, 02, 1);

    @Inject
    private ResolvedCasesRepository resolvedCasesRepository;

    @Before
    public void setUp(){
        for (int i = 0; i < FILTER_RESULTS_FOR_JUST_DATE_COUNT; i++) {
            final UUID caseId = UUID.randomUUID();
            final UUID ID = UUID.randomUUID();
            resolvedCasesRepository.save(createResolvedCases(ID, caseId, fromDate, empty(), empty(), empty()));
        }

        for (int i = 0; i < FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_COUNT; i++) {
            final UUID caseId = UUID.randomUUID();
            final UUID ID = UUID.randomUUID();
            resolvedCasesRepository.save(createResolvedCases(ID, caseId, fromDate.plusDays(1), of(REGION), empty(), empty()));
        }

        for (int i = 0; i < FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_AND_COURT_LOCATION_COUNT; i++) {
            final UUID caseId = UUID.randomUUID();
            UUID ID = UUID.randomUUID();
            resolvedCasesRepository.save(createResolvedCases(ID, caseId, fromDate.plusDays(2), of(REGION), of(COURT_LOCATION), empty()));
        }

        for (int i = 0; i < FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_AND_COURT_LOCATION_AND_CASE_TYPE_COUNT; i++) {
            final UUID caseId = UUID.randomUUID();
            UUID ID = UUID.randomUUID();
            resolvedCasesRepository.save(createResolvedCases(ID, caseId, fromDate.plusDays(3), of(REGION), of(COURT_LOCATION), of(CASE_TYPE)));
        }
    }

    @After
    public void tearDown() {
        for (final ResolvedCases resolvedCases : resolvedCasesList) {
            resolvedCasesRepository.remove(resolvedCases);
        }
    }

    @Test
    public void shouldTestCountOfCasesFixedByDate() {
        long count = resolvedCasesRepository.countOfCasesFixedByDate(fromDate, empty(), empty(), empty());
        assertThat(count, is(FILTER_RESULTS_FOR_JUST_DATE_COUNT));
    }

    @Test
    public void shouldTestCountOfCasesFixedByDateAndRegion() {
        long count = resolvedCasesRepository.countOfCasesFixedByDate(fromDate.plusDays(1), of(REGION), empty(), empty());
        assertThat(count, is(FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_COUNT));
    }

    @Test
    public void shouldTestCountOfCasesFixedByDateAndRegionAndCourtLocation() {
        long count = resolvedCasesRepository.countOfCasesFixedByDate(fromDate.plusDays(2), of(REGION), of(COURT_LOCATION), empty());
        assertThat(count, is(FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_AND_COURT_LOCATION_COUNT));
    }

    @Test
    public void shouldTestCountOfCasesFixedByDateAndRegionAndCourtLocationAndCaseType() {
        long count = resolvedCasesRepository.countOfCasesFixedByDate(fromDate.plusDays(3), of(REGION), of(COURT_LOCATION), of(CASE_TYPE));
        assertThat(count, is(FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_AND_COURT_LOCATION_AND_CASE_TYPE_COUNT));
    }

    /*
     * ------------------------------------------------------------------------------------------
     * Injection resistance for the region filter of prosecutioncasefile.query.counts-cases-errors
     * (OWASP A03:2021). region reaches this repository only; courtLocation / caseType are also
     * covered against business_validation_errors in BusinessValidationErrorRepositoryTest.
     *
     * countOfCasesFixedByDate builds its predicate with the JPA Criteria API
     * (DeltaSpike criteria().eqIgnoreCase(...)), so every filter value is bound as a query
     * parameter and never spliced into query text.
     * ------------------------------------------------------------------------------------------
     */

    @Test
    public void countOfCasesFixedByDate_withTautologyInRegion_should_not_bypass_the_filter() {
        // Sanity check: 20 resolved cases on this date are available to leak if the filter
        // can be bypassed.
        assertThat(resolvedCasesRepository.countOfCasesFixedByDate(fromDate.plusDays(1), empty(), empty(), empty()),
                is(FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_COUNT));

        final long count = resolvedCasesRepository
                .countOfCasesFixedByDate(fromDate.plusDays(1), of(TAUTOLOGY_PAYLOAD), empty(), empty());

        // Bound as data: no stored region equals the literal "nowhere' OR '1'='1".
        // Had it been concatenated, the OR would have matched every row on that date.
        assertThat(count, is(0L));
    }

    @Test
    public void countOfCasesFixedByDate_withCommentTruncationInRegion_should_not_bypass_the_filter() {
        // "region' --" would close the literal and comment out the rest of the predicate if
        // concatenated, matching all 20 rows on that date.
        final long count = resolvedCasesRepository
                .countOfCasesFixedByDate(fromDate.plusDays(1), of(COMMENT_TRUNCATION_PAYLOAD), empty(), empty());

        assertThat(count, is(0L));
    }

    @Test
    public void countOfCasesFixedByDate_withStackedDropTableInRegion_should_not_execute_it() {
        final long count = resolvedCasesRepository
                .countOfCasesFixedByDate(fromDate.plusDays(1), of(STACKED_DDL_PAYLOAD), empty(), empty());

        assertThat(count, is(0L));

        // The stacked DROP TABLE was never executed: the table is still queryable with its rows.
        assertThat(resolvedCasesRepository.countOfCasesFixedByDate(fromDate.plusDays(1), empty(), empty(), empty()),
                is(FILTER_RESULTS_FOR_JUST_DATE_AND_REGION_COUNT));
    }

    @Test
    public void countOfCasesFixedByDate_withInjectionStringStoredAsRegion_should_match_it_as_a_literal_value() {
        resolvedCasesRepository.save(createResolvedCases(UUID.randomUUID(), UUID.randomUUID(),
                fromDate.plusDays(1), of(TAUTOLOGY_PAYLOAD), empty(), empty()));

        final long count = resolvedCasesRepository
                .countOfCasesFixedByDate(fromDate.plusDays(1), of(TAUTOLOGY_PAYLOAD), empty(), empty());

        // Exactly the one matching row — proving the value is compared as data, not parsed as SQL.
        assertThat(count, is(1L));
    }

    public ResolvedCases createResolvedCases(final UUID ID, final UUID caseId,
                                                    final LocalDate localDate,
                                                    final Optional<String> region,
                                                    final Optional<String> courtLocation,
                                                    final Optional<String> caseType) {
        final ResolvedCases lResolvedCases = new ResolvedCases();
        lResolvedCases.setCaseId(caseId);
        lResolvedCases.setId(ID);
        lResolvedCases.setResolutionDate(localDate);
        if (region.isPresent()) {
            lResolvedCases.setRegion(region.get());
        }
        if (courtLocation.isPresent()) {
            lResolvedCases.setCourtLocation(courtLocation.get());
        }
        if (caseType.isPresent()) {
            lResolvedCases.setCaseType(caseType.get());
        }
        resolvedCasesList.add(lResolvedCases);
        return lResolvedCases;
    }
}