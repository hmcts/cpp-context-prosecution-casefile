package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ResolvedCasesRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final LocalDate RESOLUTION_DATE = LocalDate.of(2024, 1, 15);
    private static final LocalDate OTHER_DATE = LocalDate.of(2024, 2, 15);

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private final List<ResolvedCases> resolvedCasesList = new ArrayList<>();

    private ResolvedCasesRepository resolvedCasesRepository;

    @BeforeEach
    public void createRepositoryAndInsertTestData() {
        resolvedCasesRepository = new ResolvedCasesRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(resolvedCasesRepository);
        resolvedCasesList.clear();

        insertResolvedCases(10, "NW", "Manchester", "Summary");
        insertResolvedCases(20, "NW", "Liverpool", "Crown");
        insertResolvedCases(30, "NE", "Leeds", "Summary");
        insertResolvedCases(40, "NE", "Sheffield", "Crown");
    }

    @AfterEach
    public void removeTestData() {
        for (final ResolvedCases resolvedCases : resolvedCasesList) {
            resolvedCasesRepository.remove(resolvedCases);
        }
    }

    @Test
    void shouldCountAllCasesWithNoFilters() {
        final Long count = resolvedCasesRepository.countOfCasesFixedByDate(RESOLUTION_DATE, empty(), empty(), empty());

        assertThat(count, is(100L));
    }

    @Test
    void shouldCountZeroForDifferentDate() {
        final Long count = resolvedCasesRepository.countOfCasesFixedByDate(OTHER_DATE, empty(), empty(), empty());

        assertThat(count, is(0L));
    }

    @Test
    void shouldCountCasesFilteredByRegion() {
        final Long count = resolvedCasesRepository.countOfCasesFixedByDate(RESOLUTION_DATE, of("NW"), empty(), empty());

        assertThat(count, is(30L));
    }

    @Test
    void shouldCountCasesFilteredByRegionAndCourtLocation() {
        final Long count = resolvedCasesRepository.countOfCasesFixedByDate(RESOLUTION_DATE, of("NW"), of("Manchester"), empty());

        assertThat(count, is(10L));
    }

    @Test
    void shouldCountCasesFilteredByRegionAndCourtLocationAndCaseType() {
        final Long count = resolvedCasesRepository.countOfCasesFixedByDate(RESOLUTION_DATE, of("NE"), of("Leeds"), of("Summary"));

        assertThat(count, is(30L));
    }

    @Test
    void shouldCountCasesFilteredByCaseType() {
        final Long count = resolvedCasesRepository.countOfCasesFixedByDate(RESOLUTION_DATE, empty(), empty(), of("Crown"));

        assertThat(count, is(60L));
    }

    @Test
    void shouldCountCasesFilteredByCourtLocation() {
        final Long count = resolvedCasesRepository.countOfCasesFixedByDate(RESOLUTION_DATE, empty(), of("Sheffield"), empty());

        assertThat(count, is(40L));
    }

    private void insertResolvedCases(final int count, final String region, final String courtLocation, final String caseType) {
        for (int i = 0; i < count; i++) {
            final ResolvedCases resolvedCases = new ResolvedCases();
            resolvedCases.setId(UUID.randomUUID());
            resolvedCases.setCaseId(UUID.randomUUID());
            resolvedCases.setResolutionDate(RESOLUTION_DATE);
            resolvedCases.setRegion(region);
            resolvedCases.setCourtLocation(courtLocation);
            resolvedCases.setCaseType(caseType);
            resolvedCasesRepository.save(resolvedCases);
            resolvedCasesList.add(resolvedCases);
        }
    }
}
