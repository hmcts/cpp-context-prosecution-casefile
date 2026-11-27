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